package com.github.sieves.registry.internal.net

import com.github.sieves.content.tile.internal.Configuration
import com.github.sieves.util.resLoc
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import java.util.UUID

class ConfigurePacket() : Packet() {
    var config = Configuration {}
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

class TabUpdatePacket() : Packet() {
    var key: ResourceLocation = "missing".resLoc
    var tab = CompoundTag()

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeResourceLocation(key)
        buffer.writeNbt(tab)
    }

    override fun read(buffer: FriendlyByteBuf) {
        key = buffer.readResourceLocation()
        tab = buffer.readNbt()!!
    }
}

class TabClickedPacket() : Packet() {
    var uuid = UUID.randomUUID() //lol
    var key: ResourceLocation = "missing".resLoc


    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeUUID(uuid)
        buffer.writeResourceLocation(key)
    }

    override fun read(buffer: FriendlyByteBuf) {
        uuid = buffer.readUUID()
        key = buffer.readResourceLocation()
    }

}

class TabBindPacket() : Packet() {
    var uuid = UUID.randomUUID() //lol
    var key: ResourceLocation = "missing".resLoc

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeUUID(uuid)
        buffer.writeResourceLocation(key)
    }

    override fun read(buffer: FriendlyByteBuf) {
        uuid = buffer.readUUID()
        key = buffer.readResourceLocation()
    }

    override fun toString(): String {
        return "TabInstantiatePacket(uuid=$uuid, key=$key)"
    }

}


class GrowBlockPacket() : Packet() {
    var blockPos = BlockPos.ZERO
    var ownerPos = BlockPos.ZERO

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeBlockPos(ownerPos)
    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
        ownerPos = buffer.readBlockPos()
    }
}

class TakeUpgradePacket : Packet() {
    var blockPos = BlockPos.ZERO
    var slot: Int = 0
    var count: Int = 0

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeInt(slot)
        buffer.writeInt(count)

    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
        slot = buffer.readInt()
        count = buffer.readInt()
    }
}


class HarvestBlockPacket() : Packet() {
    var blockPos = BlockPos.ZERO
    var ownerPos = BlockPos.ZERO
    val harvested = ArrayList<ItemStack>()

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeBlockPos(ownerPos)
        buffer.writeInt(harvested.size)
        harvested.forEach {
            buffer.writeItem(it)
        }
    }

    override fun read(buffer: FriendlyByteBuf) {
        blockPos = buffer.readBlockPos()
        ownerPos = buffer.readBlockPos()
        val count = buffer.readInt()
        harvested.clear()
        for (i in 0 until count) {
            harvested.add(buffer.readItem())
        }
    }
}


