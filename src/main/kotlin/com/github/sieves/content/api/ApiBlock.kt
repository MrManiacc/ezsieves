package com.github.sieves.content.api

import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.Items
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

abstract class ApiBlock<R : ApiTile<R>>(
    properties: Properties,
    private val type: () -> BlockEntityType<R>
) :
    Block(properties), EntityBlock {
    private val ticker: BlockEntityTicker<R> = ApiTile.Ticker()

    /**
     * Delegates our menu opening to our tile entity
     */
    override fun use(
        pState: BlockState, pLevel: Level, pPos: BlockPos, pPlayer: Player, pHand: InteractionHand, pHit: BlockHitResult
    ): InteractionResult {
        val hand = pPlayer.getItemInHand(InteractionHand.MAIN_HAND).item
        if (hand == Items.Linker || hand == Items.SpeedUpgrade || hand == Items.EfficiencyUpgrade) return InteractionResult.PASS
        if (!pLevel.isClientSide) {
            val tile = pLevel.getBlockEntity(pPos)
            if (tile is ApiTile<*>) tile.onMenu(pPlayer as ServerPlayer)
        }
        return InteractionResult.SUCCESS
    }

    /**
     * This down casts our ticker to the correct type
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity?> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T> {
        return ticker as BlockEntityTicker<T>
    }

    /**
     * Automatically creates our block entity for us
     */
    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity? = type().create(pPos, pState)
}