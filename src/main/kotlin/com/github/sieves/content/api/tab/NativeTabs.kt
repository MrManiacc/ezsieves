package com.github.sieves.content.api.tab

import com.github.sieves.content.api.ApiTab
import com.github.sieves.registry.Registry
import com.github.sieves.registry.internal.IRegister
import com.github.sieves.util.Log
import com.github.sieves.util.logicalServer
import net.minecraft.client.gui.screens.inventory.FurnaceScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * Stores all of our internal tabs
 */
object NativeTabs : IRegister {

    internal object HungerModule : IRegister {
        internal val Properties = TagSpec()
            .withItem { ItemStack(Items.COOKED_RABBIT) }
            .withTooltip { TranslatableComponent("tab.sieves.player_hunger") }
            .withHover()
            .withSpin()
            .withInit {
                if (logicalServer) {
                    println("Init!")
                    val hello = CompoundTag()
                    hello.putString("key", "fag")
                    it.setProperty("secret", hello)
                    it.syncToClients()
                }
            }
            .withClientTick { player, tab ->
                tab.getProperty("secret").ifPresent {
                    println("Got secret: ${it.getString("key")}")
                }
            }
            .withTarget<FurnaceScreen>()
            .withTarget<InventoryScreen>()
            .build()
    }

    internal object PowerModule : IRegister {

        /**
         * This tab is used to send out power from a linked location
         */
        internal val Properties = TagSpec()
            .withItem { ItemStack(Registry.Items.Battery) }
            .withTooltip { TranslatableComponent("tab.sieves.player_power") }
            .withHover()
            .withSpin()
            .withTarget<FurnaceScreen>()
            .withTarget<InventoryScreen>()
            .withInit(::init)
            .withClientTick(::tickClient)
            .withServerTick(::tickServer)
            .withServerClick { player, tab ->
                Log.info { "Got click on server: ${tab.getSpec()}" }
            }
            .build()



        private fun init(apiTab: ApiTab) {
            Log.info { "Initializing the player power tab!" }
        }

        private fun tickClient(player: Player, tab: ApiTab) {

        }

        private fun tickServer(player: ServerPlayer, tab: ApiTab) {
        }
    }


}