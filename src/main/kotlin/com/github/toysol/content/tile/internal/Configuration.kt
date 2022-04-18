package com.github.sieves.content.tile.internal

import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.common.util.INBTSerializable
import java.util.*

class Configuration : INBTSerializable<CompoundTag> {
    private val sides: MutableMap<Direction, SideConfig> = EnumMap(Direction::class.java)
    var autoExport = false
    var autoImport = false

    operator fun set(direction: Direction, sideConfig: SideConfig) {
        sides[direction] = sideConfig
    }

    operator fun get(direction: Direction): SideConfig {
        return sides[direction] ?: SideConfig.None
    }


    enum class Side {
        Top,
        Bottom,
        Front,
        Back,
        Left,
        Right
    }

    enum class SideConfig(val displayName: String) {
        InputItem("Input Item"),
        InputPower("Input Power"),
        OutputItem("Output Item"),
        None("None");

        val nextIndex: Int get() = (this.ordinal + 1) % (values().size)

        val next: SideConfig get() = values()[nextIndex]
    }

    override fun serializeNBT(): CompoundTag {
        val tag = CompoundTag()
        var i = 0
        tag.putBoolean("autoImport", autoImport)
        tag.putBoolean("autoExtract", autoExport)
        sides.forEach { (t, u) ->
            val side = CompoundTag()
            side.putInt("key", t.ordinal)
            side.putInt("value", u.ordinal)
            tag.put("side_${i++}", side)
        }
        tag.putInt("count", i)
        return tag
    }


    override fun deserializeNBT(nbt: CompoundTag) {
        val count = nbt.getInt("count")
        this.autoImport = nbt.getBoolean("autoImport")
        this.autoExport = nbt.getBoolean("autoExtract")

        for (i in 0 until count) {
            val side = nbt.getCompound("side_${i}")
            val key = Direction.values()[side.getInt("key")]
            val value = SideConfig.values()[side.getInt("value")]
            sides[key] = value
        }
    }
}