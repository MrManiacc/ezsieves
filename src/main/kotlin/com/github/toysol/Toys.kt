package com.github.toysol

import com.github.toysol.registry.Registry
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(Toys.ModId)
object Toys {
    const val ModId: String = "toysol"

    init {
        Registry.register(ModId, MOD_BUS, FORGE_BUS)
    }


}