package com.github.sieves.content.api

import com.github.sieves.content.api.tab.TagSpec
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.util.INBTSerializable
import java.util.*

/**
 * The base for our tab
 */
interface ApiTab : INBTSerializable<CompoundTag> {

    /**
     * This is the only *required* thing to be overridden, must be unique to the tab
     */
    val key: ResourceLocation

    /**
     * Override if this is a ticking tab
     */
    val isTicking: Boolean get() = false

    /**
     * Called on the tick world last on the client
     */
    fun tickClient(player: Player) = Unit

    /**
     * Called on the tick server event
     */
    fun tickServer(serverPlayer: ServerPlayer) = Unit

    /**
     * This is called upon the INBT Serialize event
     */
    fun CompoundTag.serialize(): CompoundTag = this

    /**
     * This is called upon the INBT Deserialize event
     */
    fun CompoundTag.deserialize() = Unit

    /**
     * This will display a notification (on client) for the given amount of [time].
     */
    fun notify(image: ItemStack, component: Component, time: Float)

    /**
     * Gets the property with the given name
     */
    fun getProperty(name: String): Optional<CompoundTag>

    /**
     * Set a property of the given name. When simulate is true, the object won't be replaced,
     * but rather will have it's deserialize method called with the output of to serialize method
     * for the value
     */
    fun setProperty(name: String, value: CompoundTag)

    /**
     * Attemptes to get the tag or create it
     */
    fun getOrPut(name: String, supplier: () -> CompoundTag = { CompoundTag() }): CompoundTag {
        val attempt = getProperty(name)
        if (attempt.isPresent) return attempt.get()
        val value = supplier()
        setProperty(name, value)
        return value
    }

    /**
     * Updates the server on the whereabouts of this tab
     */
    fun syncToServer()

    /**
     * Syncs the stuffs to the client
     */
    fun syncToClients()

    /**
     * Delegates our serialization to our custom serialize method
     */
    override fun serializeNBT(): CompoundTag = CompoundTag().serialize()


    /**
     * Delegates our serialization to our custom deserialize method
     */
    override fun deserializeNBT(nbt: CompoundTag) = nbt.deserialize()

    /**
     * Provide external access to the built propertiers
     */
    fun getSpec(): TagSpec

    /**
     * Called before the inventory is rendered
     */
    @OnlyIn(Dist.CLIENT)
    fun preRender(
        stack: PoseStack,
        offsetX: Float,
        offsetY: Float,
        mouseX: Double,
        mouseY: Double,
        container: AbstractContainerScreen<*>
    ): Int

    /**
     * Called after the inventory is rendered
     */
    @OnlyIn(Dist.CLIENT)
    fun postRender(
        stack: PoseStack,
        offsetX: Float,
        offsetY: Float,
        mouseX: Double,
        mouseY: Double,
        container: AbstractContainerScreen<*>
    ): Int


    /**
     * Renders an item on the gui at the given position on top of everything.
     */
    fun renderItem(
        x: Float,
        y: Float,
        scale: Float,
        rotation: Quaternion,
        item: ItemStack,
        containerScreen: AbstractContainerScreen<*>
    ) {
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        val poseStack = RenderSystem.getModelViewStack()
        poseStack.pushPose()
        poseStack.translate(containerScreen.guiLeft.toDouble(), containerScreen.guiTop.toDouble(), 100.0f.toDouble())
        poseStack.translate(x.toDouble(), (y.toDouble()), 100.0)
        poseStack.scale(1.0f, -1.0f, 1.0f)
        poseStack.scale(scale, scale, scale)
        RenderSystem.applyModelViewMatrix()
        val blockPoseStack = PoseStack()
        blockPoseStack.pushPose()
        blockPoseStack.mulPose(rotation)
        blockPoseStack.scale(8f, 8f, 8f)
        val bufferSource = Minecraft.getInstance().renderBuffers().bufferSource()
        Minecraft.getInstance().itemRenderer.renderStatic(
            item,
            ItemTransforms.TransformType.FIXED,
            15728880,
            OverlayTexture.NO_OVERLAY,
            blockPoseStack,
            bufferSource,
            0
        )
        bufferSource.endBatch()
        poseStack.popPose()
        RenderSystem.applyModelViewMatrix()
    }


}