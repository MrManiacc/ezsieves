package com.github.sieves.content.api.tab

import com.github.sieves.content.api.ApiTab
import com.github.sieves.registry.Registry
import com.github.sieves.registry.internal.ListenerRegistry
import com.github.sieves.registry.internal.net.TabBindPacket
import com.github.sieves.registry.internal.net.TabClickedPacket
import com.github.sieves.registry.internal.net.TabUpdatePacket
import com.github.sieves.util.*
import com.github.sieves.util.Log.debug
import com.github.sieves.util.Log.error
import com.github.sieves.util.Log.info
import com.github.sieves.util.logicalServer
import com.github.sieves.util.physicalClient
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.network.NetworkEvent.Context
import net.minecraftforge.server.ServerLifecycleHooks
import thedarkcolour.kotlinforforge.forge.runWhenOn
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet


object TabRegistry : ListenerRegistry() {
    private val containerRegistry: MutableMap<Class<AbstractContainerScreen<*>>, MutableSet<ApiTab>> = HashMap()
    private val tabRegistry: MutableMap<ResourceLocation, ApiTab> = HashMap()
    private val tabFactories: MutableMap<ResourceLocation, TabFactory> = HashMap()
    private val activeTabs: MutableMap<UUID, MutableSet<ApiTab>> = HashMap()
    private const val tickTarget = 20 //Every half a second the client and server is
    private var serverTickTime = 0
    private var clientTickTime = 0

    /**
     * Regsters all the shitz
     */
    override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {
        info { "Registering the tab registry..." }
        Registry.Net.SyncTab.serverListener(::onSyncServer)
        Registry.Net.SyncTab.clientListener(::onSyncClient)
        Registry.Net.BindTab.clientListener(::onBind)
        Registry.Net.BindTab.serverListener(::onBind)
        Registry.Net.ClickTab.serverListener(::onClick)
        runWhenOn(Dist.CLIENT) {
            forgeBus.addListener(TabRegistry::preRender)
            forgeBus.addListener(TabRegistry::postRender)
        }
        forgeBus.addListener(::onClientTick)
        forgeBus.addListener(::onServerTick)

    }

    /**
     * Called on the server upon clicking a button
     */
    private fun onClick(tabClickedPacket: TabClickedPacket, context: Context): Boolean {
        val tabOpt = getBoundTab(tabClickedPacket.uuid, tabClickedPacket.key)
        if (tabOpt.isEmpty) return false
        val tab = tabOpt.get()
        tab.getSpec().serverClick(tab)
        return true
    }

    /**
     * Gets the tab of the given resource type that is bounds to the player with the given uuid
     */
    fun getBoundTab(uuid: UUID, resourceLocation: ResourceLocation): Optional<ApiTab> {
        if (!activeTabs.containsKey(uuid)) return Optional.empty()
        for (tab in activeTabs[uuid]!!) {
            if (tab.key == resourceLocation) return Optional.of(tab)
        }
        return Optional.empty()
    }


    /**
     * Registers the tab using the appropriate properties
     */
    fun registerTab(tab: ApiTab) {
        tabRegistry[tab.key] = tab
        tab.getSpec().targets.forEach {
            containerRegistry.getOrPut(it) { HashSet() }.add(tab)
        }
        info { "Registered tab with key ${tab.key}, and properties ${tab.getSpec()}" }
    }

    /**
     * Registers the tab provider
     */
    fun registerTabFactory(resourceLocation: ResourceLocation, tabFactory: TabFactory) {
        tabFactories[resourceLocation] = tabFactory
    }

    /**
     * This is the magical method that will bind a given tab to a player.
     * This will likely be done with capabilities in the future? or some other means
     * when push update is true, we will update the server or client depending on the [direction]
     */
    fun bindTab(
        uuid: UUID, resourceLocation: ResourceLocation, direction: NetDir = NetDir.ToClient, pushUpdate: Boolean = true
    ) {
        if (!tabFactories.containsKey(resourceLocation)) {
            error { "Attempting to bind to unregistered factory of tab: $resourceLocation" }
            return
        }
        val factory = tabFactories[resourceLocation] ?: error("This really should not happen ever")
        val tab = factory.create(uuid)
        if (tab.getSpec().hasInitializer) {
            tab.getSpec().initializer(tab)
        }
        debug { "Created new tab and bound to: $uuid, for tab:  $tab" }
        activeTabs.getOrPut(uuid) { hashSetOf() }.add(tab)
        val packet = Registry.Net.BindTab {
            this.key = tab.key
            this.uuid = uuid
        }
        if (pushUpdate) if (direction == NetDir.ToClient) {
            Registry.Net.sendToClient(packet, uuid)
            debug { "Sent bind packet to client: $packet" }
        } else {
            Registry.Net.sendToServer(packet)
        }

    }

    /**
     * Sync our bindings but don't push the update
     */
    private fun onBind(packet: TabBindPacket, context: Context): Boolean {
        bindTab(packet.uuid, packet.key, context.dir, false)
        return true
    }

    /**
     * This will return true if our local player is bound/active for the current player.
     * this only works on the client
     */
    private fun isLocalPlayerBound(resourceLocation: ResourceLocation): Boolean {
        if (!physicalClient) return false
        val uuid = Minecraft.getInstance().player?.uuid ?: return false
        val active = activeTabs[uuid] ?: return false
        for (tab in active) {
            if (tab.key == resourceLocation) return true
        }
        return false
    }

    /**
     * rends all of our rendering stuff
     */
    private fun preRender(event: ScreenEvent.DrawScreenEvent.Pre) {
//        val screen = event.screen
//        if (screen is AbstractContainerScreen<*>) {
//            val x = 19
//            var y = 0
//            val uuid = Minecraft.getInstance().player?.uuid ?: return
//            if (!activeTabs.containsKey(uuid)) return
//            for (group in activeTabs[Minecraft.getInstance().player?.uuid!!]!!) {
//                group.preRender(
//                    event.poseStack, x.toFloat(), y.toFloat(), event.mouseX.toDouble(), event.mouseY.toDouble(), screen
//                )
//                y += 25
//            }
//        }
    }

    /**
     * Renders all of our tool tips and overlayed stuff
     */
    private fun postRender(event: ScreenEvent.DrawScreenEvent.Post) {
        val screen = event.screen
        if (screen is AbstractContainerScreen<*>) {
            val x = 19
            var y = 0
            val uuid = Minecraft.getInstance().player?.uuid ?: return
            if (!activeTabs.containsKey(uuid)) return
            for (group in activeTabs[Minecraft.getInstance().player?.uuid!!]!!) {
                y += group.preRender(
                    event.poseStack,
                    x.toFloat(),
                    y.toFloat(),
                    event.mouseX.toDouble(),
                    event.mouseY.toDouble(),
                    screen
                )
            }
            y = 0
            for (group in activeTabs[Minecraft.getInstance().player?.uuid!!]!!) {
                y += group.postRender(
                    event.poseStack, x.toFloat(), y.toFloat(), event.mouseX.toDouble(), event.mouseY.toDouble(), screen
                )

            }
        }
    }

    /**
     * Updates the tab on the client from the server
     */
    private fun onSyncClient(packet: TabUpdatePacket, context: Context): Boolean {
        val tab = get(packet.key)
        if (tab.isEmpty) return false
        tab.get().deserializeNBT(packet.tab)
        info { "Finished syncing client with key ${packet.key}, properties: ${tab.get().getSpec()}" }
        return true
    }

    /**
     * Updates the tab on the client from the server
     */
    private fun onSyncServer(packet: TabUpdatePacket, context: Context): Boolean {
        val tab = get(packet.key)
        if (tab.isEmpty) return false
        tab.get().deserializeNBT(packet.tab)
        info { "Finished syncing server with key ${packet.key}, properties: ${tab.get().getSpec()}" }
        return true
    }

    /**
     * Call all the ticking tab's tick methods
     */
    private fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (!physicalClient) return
        clientTickTime++
        if (clientTickTime > tickTarget) {
            clientTickTime = 0
            for (tab in this.activeTabs) {
                tab.value.forEach {
                    val player = Minecraft.getInstance().player ?: return@forEach
                    if (it.getSpec().isClientTicking) it.tickClient(player)
                }
            }
        }
    }

    /**
     * Call all the ticking tab's tick methods
     */

    private fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (!logicalServer) return
        serverTickTime++
        if (serverTickTime > tickTarget) {
            serverTickTime = 0
            for (tab in this.activeTabs) {
                val player = tab.key.asPlayer
                if (player.isEmpty) continue
                tab.value.forEach {
                    if (it.getSpec().isServerTicking) it.tickServer(player.get())
                }
            }
        }
    }

    /**
     * Get the playerlist from the server hook
     */
    private fun players(): List<ServerPlayer> = ServerLifecycleHooks.getCurrentServer().playerList.players
    operator fun get(clazz: Class<AbstractContainerScreen<*>>): Set<ApiTab> {
        return containerRegistry[clazz] ?: emptySet()
    }

    operator fun get(resourceLocation: ResourceLocation): Optional<ApiTab> {
        return Optional.ofNullable(tabRegistry[resourceLocation])
    }


}

