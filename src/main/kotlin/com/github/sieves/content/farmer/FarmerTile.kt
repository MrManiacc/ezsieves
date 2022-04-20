package com.github.sieves.content.farmer

import com.github.sieves.Sieves
import com.github.sieves.content.api.ApiTile
import com.github.sieves.content.api.caps.TrackedEnergy
import com.github.sieves.content.api.caps.TrackedInventory
import com.github.sieves.content.link.Links
import com.github.sieves.content.tile.internal.Configuration
import com.github.sieves.registry.Registry
import com.github.sieves.util.getInflatedAAABB
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Nameable
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.network.NetworkHooks
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class FarmerTile(pos: BlockPos, state: BlockState) :
    ApiTile<FarmerTile>(Registry.Tiles.Farmer, pos, state, "tile.sieves.farmer"), Nameable {
    private val energyHandler = LazyOptional.of { energy }
    private val itemHandler = LazyOptional.of { items }
    private var tick = 0
    private val removals = ArrayList<BlockPos>()
    val energy = TrackedEnergy(250_000, ::update)
    val items = TrackedInventory(21, ::update)
    val links = Links()
    val powerCost: Int get() = ((links.getLinks().size * 600) / configuration.efficiencyModifier).roundToInt()
    val sleepTime: Int get() = ((20 * 60) / configuration.speedModifier).roundToInt()
    val ioPower: Int get() = ((links.getLinks().size * 600) * configuration.efficiencyModifier).roundToInt()
    val ioRate: Int get() = min(64, (abs(1 - configuration.efficiencyModifier.roundToInt()) * 16) + 1)

    /**
     * Called on the server when ticking happens
     */
    override fun onServerTick() {
        if (getConfig().autoExport) autoExport()
        if (getConfig().autoImport) autoImport()
        if (extractPower()) {
            if (tick >= 20) {
                linkCrops()
                purgeLinks()
                harvestCrops()
            }
            if (tick >= sleepTime) {
                growCrops()
                tick = 0
            }
        }
        tick++
    }


    private fun extractPower(): Boolean {
        val extracted = energy.extractEnergy(powerCost, true)
        if (extracted != powerCost) return false
        energy.extractEnergy(powerCost, false)
        return true
    }

    /**
     * This will check the area surrounding a 3x3x3 for crops and link to them
     */
    private fun linkCrops() {
        for (x in -5 until 5) {
            for (y in -5 until 5) {
                for (z in -5 until 5) {
                    val pos = BlockPos(blockPos.x + x, blockPos.y + y, blockPos.z + z)
                    val state = level?.getBlockState(pos) ?: continue
                    val block = state.block
                    if (block is CropBlock) links.addLink(pos, Direction.UP)
                }
            }
        }
    }


    /**
     * Removes links to invalid blocks
     */
    private fun purgeLinks() {
        removals.clear()
        for (link in links.getLinks()) {
            val state = level?.getBlockState(link.key) ?: continue
            val block = state.block
            if (block !is CropBlock) removals.add(link.key)
        }
        removals.forEach(links::removeLink)
    }

    /**
     * Grows one of the linked crops (1 per second)
     */
    private fun growCrops() {
        for (link in links.getLinks()) {
            val state = level?.getBlockState(link.key) ?: continue
            val block = state.block
            if (block !is CropBlock) continue
            block.performBonemeal(level as ServerLevel, level!!.random, link.key, state)
            if (!block.isMaxAge(state)) {
                Registry.Net.sendToClientsWithTileLoaded(Registry.Net.GrowBlock {
                    ownerPos = this@FarmerTile.blockPos
                    blockPos = link.key
                }, this)

            }
        }
    }

    /**
     * Harvests our crops
     */
    private fun harvestCrops() {
        var hasEmpty = false
        for (slot in 0 until items.slots) {
            val stack = items.getStackInSlot(slot)
            if (stack.isEmpty || stack.count < stack.item.getItemStackLimit(stack)) {
                hasEmpty = true
                break
            }
        }
        if (hasEmpty) for (link in links.getLinks()) {
            val state = level?.getBlockState(link.key) ?: continue
            val block = state.block
            if (block !is CropBlock) continue
            if (block.isMaxAge(state)) {
                val seed = block.getCloneItemStack(level!!, blockPos, state)
                val drops = Block.getDrops(state, level as ServerLevel, blockPos, null).filter { !it.sameItem(seed) }

                var missed = false
                drops.forEach {
                    val leftOver = ItemHandlerHelper.insertItem(items, it, false)
                    if (!leftOver.isEmpty) {
                        missed = true
                    }
                }
                if (!missed)
                    level?.setBlockAndUpdate(link.key, block.defaultBlockState())
                else break
                Registry.Net.sendToClientsWithTileLoaded(Registry.Net.HarvestBlock {
                    this.harvested.addAll(drops)
                    this.ownerPos = this@FarmerTile.blockPos
                    this.blockPos = link.key
                }, this)
            }
        }
    }

    /**
     * Called when saving nbt data
     */
    override fun onSave(tag: CompoundTag) {
        tag.put("energy", energy.serializeNBT())
        tag.put("links", links.serializeNBT())
        tag.put("items", items.serializeNBT())
        tag.put("config", getConfig().serializeNBT())
    }

    /**
     * Called when loading the nbt data
     */
    override fun onLoad(tag: CompoundTag) {
        energy.deserializeNBT(tag.get("energy"))
        links.deserializeNBT(tag.getCompound("links"))
        items.deserializeNBT(tag.getCompound("items"))
        getConfig().deserializeNBT(tag.getCompound("config"))
    }

    /**
     * This gets the direction based upon the relative side
     */
    override fun getRelative(side: Configuration.Side): Direction = when (side) {
        Configuration.Side.Top -> Direction.UP
        Configuration.Side.Bottom -> Direction.DOWN
        Configuration.Side.Front -> Direction.NORTH
        Configuration.Side.Back -> Direction.SOUTH
        Configuration.Side.Right -> Direction.EAST
        Configuration.Side.Left -> Direction.WEST
    }


    private fun autoExport() {
        for (key in Direction.values()) {
            val value = getConfig()[key]
            val tile = level?.getBlockEntity(blockPos.offset(key.normal)) ?: continue
            if (value == Configuration.SideConfig.OutputPower) {
                val cap = tile.getCapability(CapabilityEnergy.ENERGY, key.opposite)
                if (cap.isPresent) {
                    val other = cap.resolve().get()
                    val extracted = energy.extractEnergy(ioPower, true)
                    val leftOver = other.receiveEnergy(extracted, false)
                    energy.extractEnergy(leftOver, false)
                }
            } else if (value == Configuration.SideConfig.OutputItem) {
                val cap = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, key.opposite)
                if (cap.isPresent) {
                    val other = cap.resolve().get()
                    for (slot in 0 until this.items.slots) {
                        if (items.getStackInSlot(slot).isEmpty) continue
                        val extracted = items.extractItem(slot, ioRate, false)
                        val leftOver = ItemHandlerHelper.insertItem(other, extracted, false)
                        if (leftOver.isEmpty && !extracted.isEmpty) break
                        items.insertItem(slot, leftOver, false)
                    }
                }
            }
        }
    }

    private fun autoImport() {
        for (key in Direction.values()) {
            val value = getConfig()[key]
            val tile = level?.getBlockEntity(blockPos.offset(key.normal)) ?: continue
            if (value == Configuration.SideConfig.InputPower) {
                val cap = tile.getCapability(CapabilityEnergy.ENERGY, key.opposite)
                if (cap.isPresent) {
                    val other = cap.resolve().get()
                    val extracted = other.extractEnergy(ioPower, true)
                    val leftOver = energy.receiveEnergy(extracted, false)
                    other.extractEnergy(leftOver, false)
                }
            } else if (value == Configuration.SideConfig.InputItem) {
                val cap = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, key.opposite)
                if (cap.isPresent) {
                    val other = cap.resolve().get()
                    for (slot in 0 until other.slots) {
                        if (other.getStackInSlot(slot).isEmpty) continue
                        val extracted = other.extractItem(slot, ioRate, false)
                        val leftOver = ItemHandlerHelper.insertItem(items, extracted, false)
                        if (leftOver.isEmpty && !extracted.isEmpty) break
                        other.insertItem(slot, leftOver, false)
                    }
                }
            }
        }
    }


    /**
     * This is used to open up the container menu
     */
    override fun onMenu(player: ServerPlayer) {
        val menu = SimpleMenuProvider({ id, inv, _ -> FarmerContainer(id, inv, blockPos, this) }, name)
        NetworkHooks.openGui(player, menu, blockPos)
    }

    /**
     * Called on capability invalidation
     */
    override fun onInvalidate() {
        energyHandler.invalidate()
        itemHandler.invalidate()
    }

    override fun getName(): Component {
        return TranslatableComponent("container.${Sieves.ModId}.farmer")
    }

    override fun getRenderBoundingBox(): AABB {
        return INFINITE_EXTENT_AABB
    }


    override fun <T : Any?> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (side == null && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return itemHandler.cast()
        if (cap == CapabilityEnergy.ENERGY && side == null) return energyHandler.cast()
        if (side == null) return super.getCapability(cap, null)
        return when (getConfig()[side]) {
            Configuration.SideConfig.InputItem -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) return itemHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.InputPower -> if (CapabilityEnergy.ENERGY == cap) return energyHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.OutputItem -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) itemHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.OutputPower -> if (CapabilityEnergy.ENERGY == cap) energyHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.None -> LazyOptional.empty()
            Configuration.SideConfig.InputOutputItems -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) return itemHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.InputOutputPower -> if (CapabilityEnergy.ENERGY == cap) return energyHandler.cast() else LazyOptional.empty()
            Configuration.SideConfig.InputOutputAll -> if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == cap) return itemHandler.cast() else if (CapabilityEnergy.ENERGY == cap) energyHandler.cast() else LazyOptional.empty()
        }

    }
}