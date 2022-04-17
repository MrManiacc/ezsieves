package com.github.toysol.content.container

import com.github.toysol.content.tile.SieveTile
import com.github.toysol.registry.Registry
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler
import net.minecraftforge.items.SlotItemHandler


class SieveContainer(
    id: Int,
    private val inventory: Inventory,
    private val slotsIn: IItemHandler = ItemStackHandler(2),
    private val slotsOut: IItemHandler = ItemStackHandler(1),
    private val pos: BlockPos = BlockPos.ZERO,
    val data: ContainerData = SimpleContainerData(2),
    val player: Player = inventory.player
) : AbstractContainerMenu(Registry.Containers.Sieve, id) {
    private val containerAccess = ContainerLevelAccess.create(inventory.player.level, pos)

    init {
        val size = 18
        val startX = 8
        val startY = 84
        val hotbarY = 142
        var i = 0
        for (column in 0 until 9) {
            for (row in 0 until 3) {
                addSlot(Slot(inventory, 9 + row * 9 + column, startX + column * size, startY + row * size))
                i++
            }
            addSlot(Slot(inventory, column, startX + column * size, hotbarY))
            i++
        }
        addSlot(SlotItemHandler(slotsIn, 0, 35, 33))
        addSlot(SlotItemHandler(slotsIn, 1, 66, 33))
        addSlot(SlotItemHandler(slotsOut, 0, 124, 35))
        addDataSlots(data)
    }


    override fun stillValid(pPlayer: Player): Boolean {
        return stillValid(containerAccess, pPlayer, Registry.Blocks.Sieve)
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var retStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasItem()) {
            val stack = slot.item
            retStack = stack.copy()
            val size = slots.size - player.inventory.containerSize
            if (index < size) {
                if (!moveItemStackTo(stack, 0, slots.size, true)) return ItemStack.EMPTY
            } else if (!moveItemStackTo(stack, 0, size, false)) return ItemStack.EMPTY
            if (stack.isEmpty || stack.count == 0) {
                slot.set(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
            if (stack.count == retStack.count) return ItemStack.EMPTY
            slot.onTake(player, stack)
        }
        return retStack
    }

    companion object {
        fun getServerContainer(sieve: SieveTile, pos: BlockPos): MenuConstructor {
            return MenuConstructor { id, inv, _ ->
                SieveContainer(id, inv, sieve.inputInv, sieve.outputInv, pos, player = inv.player, data = SieveContainerData(2, sieve))
            }
        }

    }
}