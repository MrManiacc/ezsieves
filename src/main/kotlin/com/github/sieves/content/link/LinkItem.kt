package com.github.sieves.content.link

import com.github.sieves.content.api.tab.TabRegistry
import com.github.sieves.content.battery.BatteryTile
import com.github.sieves.registry.Registry
import com.github.sieves.util.getBlockPos
import com.github.sieves.util.putBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraftforge.energy.CapabilityEnergy
import java.util.*

class LinkItem : Item(Properties().tab(Registry.Items.CreativeTab).stacksTo(1)) {
    /**
     * Called when this item is used when targetting a Block
     */
    override fun useOn(pContext: UseOnContext): InteractionResult {

        if (pContext.level.isClientSide) return InteractionResult.SUCCESS
        val item = pContext.itemInHand
        val tag = item.orCreateTag
        val level = pContext.level
        val tile = level.getBlockEntity(pContext.clickedPos)
        if (tile == null && pContext.player!!.isShiftKeyDown) {
            if (pContext.player!!.isShiftKeyDown) {
                val uuid = pContext.player!!.uuid
                TabRegistry.bindTab(uuid, Registry.Tabs.PlayerPower.key)
                pContext.player?.displayClientMessage(
                    TextComponent("Added power link to self"),
                    false
                )
                return InteractionResult.SUCCESS
            }
        }
        if (pContext.player?.isShiftKeyDown == true && tile is BatteryTile) {
            tile.unlink()
            pContext.player?.displayClientMessage(
                TextComponent("Removed links from ${tile.blockPos.toShortString()}"), false
            )
            tag.remove("hasFrom")
            tag.remove("from")
        } else
            if (tag.getBoolean("hasFrom")) {
                val from = tag.getBlockPos("from")
                val fromTile = level.getBlockEntity(from)
                if (fromTile !is BatteryTile) return InteractionResult.FAIL
                if (from == tile?.blockPos) return InteractionResult.FAIL

                val to = level.getBlockEntity(pContext.clickedPos) ?: return InteractionResult.FAIL
                if (!to.getCapability(CapabilityEnergy.ENERGY, pContext.clickedFace).isPresent) {
                    if (pContext.player!!.isShiftKeyDown) {
                        val uuid = pContext.player!!.uuid
                        TabRegistry.bindTab(uuid, Registry.Tabs.PlayerPower.key)
                        return InteractionResult.SUCCESS
                    }
                    return InteractionResult.FAIL
                }
                fromTile.linkTo(pContext.clickedPos, pContext.clickedFace)
                tag.remove("hasFrom")
                tag.remove("from")
                pContext.player?.displayClientMessage(
                    TextComponent("Finished link from ${from.toShortString()}, to ${tile?.blockPos?.toShortString()}"),
                    false
                )
            } else {
                if (tile !is BatteryTile) return InteractionResult.PASS
                tag.putBoolean("hasFrom", true)
                tag.putBlockPos("from", tile.blockPos)
                pContext.player?.displayClientMessage(
                    TextComponent("Started link at ${tile?.blockPos.toShortString()}"),
                    false
                )
            }
        item.tag = tag
        return InteractionResult.SUCCESS
    }

    override fun isEnchantable(pStack: ItemStack): Boolean {
        return true
    }

    /**
     * Return the enchantability factor of the item, most of the time is based on material.
     */
    override fun getEnchantmentValue(): Int {
        return 1
    }
}