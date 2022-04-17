package com.github.toysol.content.screen

import com.github.toysol.content.container.SieveContainer
import com.github.toysol.util.resLoc
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.entity.player.Inventory
import java.text.NumberFormat
import kotlin.math.roundToInt

class SieveScreen(
    val container: SieveContainer, playerInv: Inventory, component: Component
) : AbstractContainerScreen<SieveContainer>(container, playerInv, component) {

    init {
        leftPos = 0
        topPos = 0
        imageWidth = 175
        imageHeight = 166
    }


    override fun renderBg(stack: PoseStack, partialTicks: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.setShaderTexture(0, Texture)
        blit(stack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight)
        RenderSystem.setShaderTexture(0, Widgets)

        val progress = container.data.get(0)
        val percent = ((progress / 100f) * 20f).roundToInt()
        blit(stack, this.leftPos + 90, this.topPos + 35, 0, 99, percent, 15)
        val energy = container.data.get(1)
        val energyPercent = ((energy / 100f) * 68).roundToInt()
        blit(stack, this.leftPos + 165, this.topPos + 8, 0, 114, 3, energyPercent)

    }

    override fun render(pPoseStack: PoseStack, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        this.renderBackground(pPoseStack)
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick)

        if (isHovering(
                90,
                35,
                25,
                15,
                pMouseX.toDouble(),
                pMouseY.toDouble()
            )
        ) {
            val progress = container.data.get(0)
            renderTooltip(pPoseStack, TextComponent("progress: §7$progress%"), pMouseX, pMouseY)
        }
        if (isHovering(163, 8, 5, 67, pMouseX.toDouble(), pMouseY.toDouble())) {
            val progress = container.data.get(1)
            val percent = ((progress / 100f) * 50_000)
            renderTooltip(
                pPoseStack,
                TextComponent("power: ${NumberFormat.getIntegerInstance().format(percent)}RF/50,000RF"),
                pMouseX,
                pMouseY
            )
        }
        this.renderTooltip(pPoseStack, pMouseX, pMouseY)

//        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick)
    }

    companion object {
        private val Texture = "textures/gui/sieve_gui.png".resLoc
        private val Widgets = "textures/gui/widgets.png".resLoc
    }
}
