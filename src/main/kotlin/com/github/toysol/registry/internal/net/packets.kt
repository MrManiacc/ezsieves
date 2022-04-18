package com.github.sieves.registry.internal.net

import com.github.sieves.content.tile.internal.Configuration
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.dimension.DimensionType

class ConfigurePacket() : Packet() {
    var config = Configuration()
    var blockPos = BlockPos.ZERO
    var world: ResourceKey<Level> = Level.OVERWORLD

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeResourceLocation(world.location())
        buffer.writeNbt(config.serializeNBT())
    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
        world = ResourceKey.create(Registry.DIMENSION_REGISTRY, buffer.readResourceLocation())
        buffer.readNbt()?.let { config.deserializeNBT(it) }
    }

    override fun toString(): String {
        return "ConfigurePacket(config=$config, blockPos=$blockPos, world=$world)"
    }

}

class ContainerPacket() : Packet() {
    var id = 0
    var blockPos = BlockPos.ZERO

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeInt(id)

    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
        id = buffer.readInt()

    }


}