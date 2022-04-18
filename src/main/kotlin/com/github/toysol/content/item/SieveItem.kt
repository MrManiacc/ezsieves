package com.github.sieves.content.item

import com.github.sieves.registry.Registry
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab

/**
 * Our item that will be displayed in game
 */
class SieveItem : BlockItem(
    Registry.Blocks.Sieve,
    Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1).fireResistant()
)