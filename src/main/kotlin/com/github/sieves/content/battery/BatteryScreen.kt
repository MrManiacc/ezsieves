package com.github.sieves.content.battery

import com.github.sieves.content.api.ApiScreen
import com.github.sieves.content.api.gui.ConfigWidget
import com.github.sieves.content.tile.internal.Configuration
import com.github.sieves.util.resLoc
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraftforge.energy.CapabilityEnergy
import java.text.NumberFormat

@Suppress("DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES")
class BatteryScreen(
    val container: BatteryContainer, playerInv: Inventory
) : ApiScreen<BatteryContainer, BatteryTile>(container, playerInv), ConfigWidget<BatteryTile> {
    override val texture: ResourceLocation = "textures/gui/battery_gui.png".resLoc
    override fun renderMain(stack: PoseStack, mouseX: Double, mouseY: Double) {
        blit(stack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight)
    }

    override fun renderOverlayWidgets(stack: PoseStack, mouseX: Double, mouseY: Double) {
        renderCharger(stack, mouseX, mouseY)
        val power = (tile().getStoredPower() / tile().getTotalPower().toFloat()) * 68
        blit(stack, this.leftPos + 165, this.topPos + 8, 0, 114, 3, power.toInt())
    }

    override fun renderToolTips(stack: PoseStack, mouseX: Double, mouseY: Double) {
        if (isHovered(83, 56, 9, 11, mouseX, mouseY)) {
            val item = tile().items.getStackInSlot(0)
            if (item.isEmpty) return
            val cap = item.getCapability(CapabilityEnergy.ENERGY)
            if (cap.isPresent) {
                val energy = cap.resolve().get()
                drawTooltip(
                    stack,
                    "power: §6${
                        NumberFormat.getIntegerInstance().format(energy.energyStored)
                    }FE/ ${NumberFormat.getIntegerInstance().format(energy.maxEnergyStored)}FE",
                    mouseX,
                    mouseY
                )
            }
        }

        if (isHovered(163, 8, 5, 67, mouseX, mouseY)) {
            drawTooltip(
                stack,
                "power: §6${
                    NumberFormat.getIntegerInstance().format(tile().getStoredPower())
                }FE/${NumberFormat.getIntegerInstance().format(tile().getTotalPower())}",
                mouseX,
                mouseY
            )
        }
    }


    private fun renderCharger(stack: PoseStack, mouseX: Double, mouseY: Double) {
        val item = tile().items.getStackInSlot(0)
        if (item.isEmpty) return
        val cap = item.getCapability(CapabilityEnergy.ENERGY)
        if (cap.isPresent) {
            val energy = cap.resolve().get()
            val percent = (energy.energyStored / energy.maxEnergyStored.toFloat()) * 11f
            blit(stack, this.leftPos + 83, this.topPos + 56, 59, 0, 9, percent.toInt())
        }
    }


    //ConfigWidget
    override val config: Configuration get() = container.tile.getConfig()
    override var configWidth: Float = 0f
    override var configure: Boolean = false

    /**
     * Pass a reference to our tile
     */
    override val tile: () -> BatteryTile
        get() = { container.tile }
}
