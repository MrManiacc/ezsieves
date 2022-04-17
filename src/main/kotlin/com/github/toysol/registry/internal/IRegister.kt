package com.github.toysol.registry.internal

import net.minecraftforge.eventbus.api.IEventBus
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS

interface IRegister {
    fun register(modId: String, modBus: IEventBus = MOD_BUS, forgeBus: IEventBus = FORGE_BUS)
}