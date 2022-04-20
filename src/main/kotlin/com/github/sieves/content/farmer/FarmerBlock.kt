package com.github.sieves.content.farmer

import com.github.sieves.content.api.ApiBlock
import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.Net
import com.github.sieves.registry.internal.net.ConfigurePacket
import com.github.sieves.registry.internal.net.TakeUpgradePacket
import com.github.sieves.util.Log
import com.github.sieves.util.getLevel
import com.github.sieves.util.join
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.items.wrapper.InvWrapper
import net.minecraftforge.network.NetworkEvent


class FarmerBlock(properties: Properties) :
    ApiBlock<FarmerTile>(properties, { Registry.Tiles.Farmer }) {
    init {
        Net.Configure.serverListener(::onConfiguration)
        Net.TakeUpgrade.serverListener(::onTakeUpgrade)
    }

    private val shape = Shapes.empty()
        .join(Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0625, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.0625, 0.0625, 0.0625, 0.9375, 0.625, 0.9375), BooleanOp.OR)
        .join(Shapes.box(0.15625, 0.8125, 0.65625, 0.34375, 0.859375, 0.84375), BooleanOp.OR)
        .join(Shapes.box(0.125, 0.625, 0.625, 0.375, 0.8125, 0.875), BooleanOp.OR)
        .join(Shapes.box(0.65625, 0.8125, 0.15625, 0.84375, 0.859375, 0.34375), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.625, 0.125, 0.875, 0.8125, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.65625, 0.8125, 0.15625, 0.84375, 0.859375, 0.34375), BooleanOp.OR)


    private fun onConfiguration(configurePacket: ConfigurePacket, context: NetworkEvent.Context): Boolean {
        val level = configurePacket.getLevel(configurePacket.world)
        val blockEntity = level.getBlockEntity(configurePacket.blockPos)
        if (blockEntity !is FarmerTile) return false
        blockEntity.getConfig().deserializeNBT(configurePacket.config.serializeNBT())
        blockEntity.update()
        Log.info { "Updated configuration: $configurePacket" }
        return true
    }

    private fun onTakeUpgrade(configurePacket: TakeUpgradePacket, context: NetworkEvent.Context): Boolean {
        val inv = context.sender?.inventory ?: return false
        val level = context.sender!!.level
        val blockEntity = level.getBlockEntity(configurePacket.blockPos)
        if (blockEntity !is FarmerTile) return false
        val extracted = blockEntity.getConfig().upgrades.extractItem(configurePacket.slot, configurePacket.count, false)
        ItemHandlerHelper.insertItem(InvWrapper(inv), extracted, false)
        blockEntity.update()
        Log.info { "Updated configuration: $configurePacket" }
        return true
    }

    override fun getShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape = shape

    override fun getShadeBrightness(pState: BlockState, pLevel: BlockGetter, pPos: BlockPos): Float {
        return 0.6f
    }

}