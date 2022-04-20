package com.github.sieves.content.api.tab

import com.github.sieves.content.api.ApiTab
import com.github.sieves.util.Log
import com.github.sieves.util.isClicked
import com.github.sieves.util.isHovered
import com.github.sieves.util.resLoc
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Vector3f
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.*
import java.util.function.Supplier
import kotlin.collections.HashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import com.github.sieves.registry.Registry.Net as Net

/**
 * This is used to render our tab
 */
@Suppress("UNCHECKED_CAST")
open class Tab(
    override val key: ResourceLocation, private val properties: TagSpec,
) : ApiTab {
    private val nbt: MutableMap<String, CompoundTag> = HashMap()
    private val uuid: UUID get() = getProperty("tab_data").get().getUUID("owner")

    /**
     * This will clone the properties and resource location, but ignore the nbt data/properties
     */
    fun cloneWith(uuid: UUID): Tab {
        val new = Tab(key, properties)
        new.getOrPut("tab_data").putUUID("owner", uuid) //Inject the owner uuid
        return new
    }

    /**
     * Force our tab to tick when requested
     */
    override val isTicking: Boolean = properties.isClientTicking || properties.isServerTicking

    /**
     * Called on the tick world last on the client
     */
    override fun tickClient(player: Player) {
        if (properties.isClientTicking) properties.clientTicker.get()(player, this)
    }

    /**
     * Called on the tick server event
     */
    override fun tickServer(serverPlayer: ServerPlayer) {
        if (properties.isServerTicking) properties.serverTicker.get()(serverPlayer, this)
    }

    /**
     * Renders our item
     */
    override fun preRender(
        stack: PoseStack,
        offsetX: Float,
        offsetY: Float,
        mouseX: Double,
        mouseY: Double,
        container: AbstractContainerScreen<*>
    ): Int = with(container) {

        val yOff = offsetY + 20
        if (isHovered((-19f).toInt(), yOff.toInt(), 19, 19, mouseX, mouseY) && properties.drawHover)
            RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 1f)
        else RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.setShaderTexture(0, widgets)
        blit(stack, guiLeft - 19, guiTop + offsetY.toInt() + 20, 237, 168, 19, 21)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        val angle = (System.currentTimeMillis() / 10 % 360).toFloat()
        val rotation = properties.rotationSupplier(this@Tab)
        if (properties.drawSpin) {
            rotation.mul(Vector3f.YP.rotationDegrees(angle))
        }
        if (properties.drawItem) {
            val item = properties.itemstackSupplier(this@Tab)
            val selected = Minecraft.getInstance().player?.inventory?.getSelected()
            if (selected != ItemStack.EMPTY)
                renderItem(-8f, yOff + 10f, 2f, rotation, selected!!, container)
            else
                renderItem(-8f, yOff + 10f, 2f, rotation, item, container)
        }
        RenderSystem.setShaderTexture(0, widgets)
        if (properties.drawToolTip) {
            if (isHovered((-19f).toInt(), yOff.toInt(), 19, 19, mouseX, mouseY) && properties.drawHover) {
                val text = properties.tooltipSupplier(this@Tab)
                renderTooltip(stack, text, mouseX.toInt(), mouseY.toInt())
            }
        }

        if (properties.drawLarger) 27 else 19
    }

    /**
     * Renders our item stuff like tooltiops
     */
    override fun postRender(
        stack: PoseStack,
        offsetX: Float,
        offsetY: Float,
        mouseX: Double,
        mouseY: Double,
        container: AbstractContainerScreen<*>
    ): Int = with(container) {
        val yOff = offsetY + 20
        if (isClicked((-19f).toInt(), yOff.toInt(), 19, 19, mouseX, mouseY)) {
            Net.sendToServer(Net.ClickTab {
                this.key = this@Tab.key
                this.uuid = this@Tab.uuid
            })
//            getSpec().clientClick(this@Tab)
        }
        if (properties.drawLarger) 27 else 19
    }


    /**
     * This is called upon the INBT Serialize event
     */
    override fun CompoundTag.serialize(): CompoundTag {
        putInt("properties", nbt.size)
        var index = 0
        for ((key, value) in nbt) {
            putString("key_$index", key)
            put("value_${index++}", value)
        }
        return this
    }

    /**
     * This is called upon the INBT Deserialize event
     */
    override fun CompoundTag.deserialize() {
        nbt.clear()
        for (index in 0 until getInt("properties")) {
            try {
                val key = getString("key_$index")
                val value = getCompound("value_$index")
                nbt[key] = value
            } catch (ex: Exception) {
                Log.error { "Failed to deserialize tab: $key, error message: ${ex.message}" }
            }

        }
    }

    /**
     * This will display a notification (on client) for the given amount of [time].
     */
    override fun notify(image: ItemStack, component: Component, time: Float) {
        //TODO
    }

    /**
     * Gets the property with the given name
     */
    override fun getProperty(name: String): Optional<CompoundTag> =
        Optional.ofNullable(nbt[name])

    /**
     * Set a property of the given name. When simulate is true, the object won't be replaced,
     * but rather will have it's deserialize method called with the output of to serialize method
     * for the value
     */
    override fun setProperty(name: String, value: CompoundTag) {
        nbt[name] = value
    }

    /**
     * Updates the server on the whereabouts of this tab
     */
    override fun syncToServer() {
        Net.sendToServer(Net.SyncTab {
            this.tab = this@Tab.serializeNBT()
            this.key = this@Tab.key
        })
    }

    /**
     * Syncs the stuffs to the client
     */
    override fun syncToClients() {
        Net.sendToAllClients(Net.SyncTab {
            this.tab = this@Tab.serializeNBT()
            this.key = this@Tab.key
        })
    }

    /**
     * Provide external access to the built propertiers
     */
    override fun getSpec(): TagSpec = properties
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tab) return false

        if (key != other.key) return false
        if (properties != other.properties) return false
        if (nbt != other.nbt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + nbt.hashCode()
        return result
    }

    override fun toString(): String {
        return "Tab(key=$key, properties=$properties, nbt=$nbt)"
    }


    companion object {
        private val widgets = "textures/gui/widgets.png".resLoc

        /**
         * This is used delegate registration using properties
         */
        fun register(
            name: ResourceLocation, supplier: () -> TagSpec
        ): ReadOnlyProperty<Any?, Tab> {
            val tab = Tab(name, supplier())
            TabRegistry.registerTab(tab)
            TabRegistry.registerTabFactory(name) {
                tab.cloneWith(it)
            }
            return object : ReadOnlyProperty<Any?, Tab>, Supplier<Tab>, () -> Tab {
                override fun getValue(thisRef: Any?, property: KProperty<*>): Tab = get()
                override fun invoke(): Tab = get()
                override fun get(): Tab = tab
            }
        }
    }


}