package com.github.sieves.content.battery

import com.github.sieves.content.api.ApiBlock
import com.github.sieves.registry.Registry
import com.github.sieves.registry.internal.net.ConfigurePacket
import com.github.sieves.util.Log
import com.github.sieves.util.getLevel
import com.github.sieves.util.join
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraftforge.network.NetworkEvent


class BatteryBlock(properties: Properties) : ApiBlock<BatteryTile>(properties, { Registry.Tiles.Battery }) {

    init {
        Registry.Net.Configure.serverListener(::onConfiguration)
    }

    private fun onConfiguration(configurePacket: ConfigurePacket, context: NetworkEvent.Context): Boolean {
        val level = configurePacket.getLevel(configurePacket.world)
        val blockEntity = level.getBlockEntity(configurePacket.blockPos)
        if (blockEntity !is BatteryTile) return false
        blockEntity.getConfig().deserializeNBT(configurePacket.config.serializeNBT())
        blockEntity.update()
        Log.info { "Updated configuration: $configurePacket" }
        return true
    }

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(DirectionalBlock.FACING, pContext.clickedFace.opposite)
    }

    override fun rotate(pState: BlockState, pRotation: Rotation): BlockState {
        return pState.setValue(DirectionalBlock.FACING, pRotation.rotate(pState.getValue(DirectionalBlock.FACING)))
    }

    override fun mirror(pState: BlockState, pMirror: Mirror): BlockState {
        return pState.rotate(pMirror.getRotation(pState.getValue(DirectionalBlock.FACING)))
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(DirectionalBlock.FACING)
    }

    override fun getShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape {
        return when (pState.getValue(DirectionalBlock.FACING)) {
            Direction.NORTH -> north
            Direction.EAST -> east
            Direction.WEST -> west
            Direction.SOUTH -> south
            Direction.UP -> up
            Direction.DOWN -> down
            else -> down
        }
    }

    override fun getShadeBrightness(pState: BlockState, pLevel: BlockGetter, pPos: BlockPos): Float {
        return 0.6f
    }


    private val down = Shapes.empty().join(Shapes.box(0.34375, 0.0, 0.3125, 0.65625, 0.015625, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.328125, 0.328125, 0.640625, 0.34375, 0.609375), BooleanOp.OR)
        .join(Shapes.box(0.40625, 0.34375, 0.375, 0.59375, 0.390625, 0.5625), BooleanOp.OR)
        .join(Shapes.box(0.34375, 0.015625, 0.328125, 0.359375, 0.328125, 0.421875), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.015625, 0.421875, 0.375, 0.328125, 0.515625), BooleanOp.OR)
        .join(Shapes.box(0.34375, 0.015625, 0.515625, 0.359375, 0.328125, 0.609375), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.015625, 0.515625, 0.65625, 0.328125, 0.609375), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.015625, 0.421875, 0.640625, 0.328125, 0.515625), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.015625, 0.328125, 0.65625, 0.328125, 0.421875), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.015625, 0.609375, 0.453125, 0.328125, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.453125, 0.015625, 0.59375, 0.546875, 0.328125, 0.609375), BooleanOp.OR)
        .join(Shapes.box(0.546875, 0.015625, 0.609375, 0.640625, 0.328125, 0.625), BooleanOp.OR)
        .join(Shapes.box(0.546875, 0.015625, 0.3125, 0.640625, 0.328125, 0.328125), BooleanOp.OR)
        .join(Shapes.box(0.453125, 0.015625, 0.328125, 0.546875, 0.328125, 0.34375), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.015625, 0.3125, 0.453125, 0.328125, 0.328125), BooleanOp.OR)

    val east = Shapes.empty().join(Shapes.box(0.984375, 0.34375, 0.34375, 1.0, 0.65625, 0.65625), BooleanOp.OR)
        .join(Shapes.box(0.65625, 0.359375, 0.359375, 0.671875, 0.640625, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.609375, 0.40625, 0.40625, 0.65625, 0.59375, 0.59375), BooleanOp.OR)
        .join(Shapes.box(0.671875, 0.34375, 0.359375, 0.984375, 0.359375, 0.453125), BooleanOp.OR)
        .join(Shapes.box(0.671875, 0.359375, 0.453125, 0.984375, 0.375, 0.546875), BooleanOp.OR)
        .join(Shapes.box(0.671875, 0.34375, 0.546875, 0.984375, 0.359375, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.671875, 0.640625, 0.546875, 0.984375, 0.65625, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.671875, 0.625, 0.453125, 0.984375, 0.640625, 0.546875), BooleanOp.OR)
        .join(Shapes.box(0.671875, 0.640625, 0.359375, 0.984375, 0.65625, 0.453125), BooleanOp.OR)
        .join(Shapes.box(0.671875, 0.359375, 0.640625, 0.984375, 0.453125, 0.65625), BooleanOp.OR)
        .join(Shapes.box(0.671875, 0.453125, 0.625, 0.984375, 0.546875, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.671875, 0.546875, 0.640625, 0.984375, 0.640625, 0.65625), BooleanOp.OR)
        .join(Shapes.box(0.671875, 0.546875, 0.34375, 0.984375, 0.640625, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.671875, 0.453125, 0.359375, 0.984375, 0.546875, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.671875, 0.359375, 0.34375, 0.984375, 0.453125, 0.359375), BooleanOp.OR)

    val north = Shapes.empty().join(Shapes.box(0.34375, 0.34375, 0.0, 0.65625, 0.65625, 0.015625), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.359375, 0.328125, 0.640625, 0.640625, 0.34375), BooleanOp.OR)
        .join(Shapes.box(0.40625, 0.40625, 0.34375, 0.59375, 0.59375, 0.390625), BooleanOp.OR)
        .join(Shapes.box(0.546875, 0.34375, 0.015625, 0.640625, 0.359375, 0.328125), BooleanOp.OR)
        .join(Shapes.box(0.453125, 0.359375, 0.015625, 0.546875, 0.375, 0.328125), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.34375, 0.015625, 0.453125, 0.359375, 0.328125), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.640625, 0.015625, 0.453125, 0.65625, 0.328125), BooleanOp.OR)
        .join(Shapes.box(0.453125, 0.625, 0.015625, 0.546875, 0.640625, 0.328125), BooleanOp.OR)
        .join(Shapes.box(0.546875, 0.640625, 0.015625, 0.640625, 0.65625, 0.328125), BooleanOp.OR)
        .join(Shapes.box(0.34375, 0.359375, 0.015625, 0.359375, 0.453125, 0.328125), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.453125, 0.015625, 0.375, 0.546875, 0.328125), BooleanOp.OR)
        .join(Shapes.box(0.34375, 0.546875, 0.015625, 0.359375, 0.640625, 0.328125), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.546875, 0.015625, 0.65625, 0.640625, 0.328125), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.453125, 0.015625, 0.640625, 0.546875, 0.328125), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.359375, 0.015625, 0.65625, 0.453125, 0.328125), BooleanOp.OR)

    val south = Shapes.empty().join(Shapes.box(0.34375, 0.34375, 0.984375, 0.65625, 0.65625, 1.0), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.359375, 0.65625, 0.640625, 0.640625, 0.671875), BooleanOp.OR)
        .join(Shapes.box(0.40625, 0.40625, 0.609375, 0.59375, 0.59375, 0.65625), BooleanOp.OR)
        .join(Shapes.box(0.546875, 0.34375, 0.671875, 0.640625, 0.359375, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.453125, 0.359375, 0.671875, 0.546875, 0.375, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.34375, 0.671875, 0.453125, 0.359375, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.640625, 0.671875, 0.453125, 0.65625, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.453125, 0.625, 0.671875, 0.546875, 0.640625, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.546875, 0.640625, 0.671875, 0.640625, 0.65625, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.34375, 0.359375, 0.671875, 0.359375, 0.453125, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.453125, 0.671875, 0.375, 0.546875, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.34375, 0.546875, 0.671875, 0.359375, 0.640625, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.546875, 0.671875, 0.65625, 0.640625, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.453125, 0.671875, 0.640625, 0.546875, 0.984375), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.359375, 0.671875, 0.65625, 0.453125, 0.984375), BooleanOp.OR)

    val up = Shapes.empty().join(Shapes.box(0.34375, 0.984375, 0.34375, 0.65625, 1.0, 0.65625), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.65625, 0.359375, 0.640625, 0.671875, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.40625, 0.609375, 0.40625, 0.59375, 0.65625, 0.59375), BooleanOp.OR)
        .join(Shapes.box(0.34375, 0.671875, 0.359375, 0.359375, 0.984375, 0.453125), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.671875, 0.453125, 0.375, 0.984375, 0.546875), BooleanOp.OR)
        .join(Shapes.box(0.34375, 0.671875, 0.546875, 0.359375, 0.984375, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.671875, 0.546875, 0.65625, 0.984375, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.625, 0.671875, 0.453125, 0.640625, 0.984375, 0.546875), BooleanOp.OR)
        .join(Shapes.box(0.640625, 0.671875, 0.359375, 0.65625, 0.984375, 0.453125), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.671875, 0.640625, 0.453125, 0.984375, 0.65625), BooleanOp.OR)
        .join(Shapes.box(0.453125, 0.671875, 0.625, 0.546875, 0.984375, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.546875, 0.671875, 0.640625, 0.640625, 0.984375, 0.65625), BooleanOp.OR)
        .join(Shapes.box(0.546875, 0.671875, 0.34375, 0.640625, 0.984375, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.453125, 0.671875, 0.359375, 0.546875, 0.984375, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.359375, 0.671875, 0.34375, 0.453125, 0.984375, 0.359375), BooleanOp.OR)

    val west = Shapes.empty().join(Shapes.box(0.0, 0.34375, 0.34375, 0.015625, 0.65625, 0.65625), BooleanOp.OR)
        .join(Shapes.box(0.328125, 0.359375, 0.359375, 0.34375, 0.640625, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.34375, 0.40625, 0.40625, 0.390625, 0.59375, 0.59375), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.34375, 0.359375, 0.328125, 0.359375, 0.453125), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.359375, 0.453125, 0.328125, 0.375, 0.546875), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.34375, 0.546875, 0.328125, 0.359375, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.640625, 0.546875, 0.328125, 0.65625, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.625, 0.453125, 0.328125, 0.640625, 0.546875), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.640625, 0.359375, 0.328125, 0.65625, 0.453125), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.359375, 0.640625, 0.328125, 0.453125, 0.65625), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.453125, 0.625, 0.328125, 0.546875, 0.640625), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.546875, 0.640625, 0.328125, 0.640625, 0.65625), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.546875, 0.34375, 0.328125, 0.640625, 0.359375), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.453125, 0.359375, 0.328125, 0.546875, 0.375), BooleanOp.OR)
        .join(Shapes.box(0.015625, 0.359375, 0.34375, 0.328125, 0.453125, 0.359375), BooleanOp.OR)

}