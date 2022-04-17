package com.github.toysol.content.render

import com.github.toysol.content.block.SieveBlock.Companion.FACING
import com.github.toysol.content.tile.SieveTile
import com.github.toysol.registry.Registry
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import net.minecraft.client.Minecraft
import net.minecraft.client.model.PlayerModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.HumanoidArm
import kotlin.random.Random

class SieveRenderer : BlockEntityRenderer<SieveTile> {
    private var rot = 0.0
    private var fullRot = 0.0
    private val player = Minecraft.getInstance().entityModels.bakeLayer(ModelLayers.PLAYER)
    private val model = PlayerModel<AbstractClientPlayer>(player, false)
    private var decreasing = false

    override fun render(
        pBlockEntity: SieveTile,
        pPartialTick: Float,
        stack: PoseStack,
        pBufferSource: MultiBufferSource,
        pPackedLight: Int,
        pPackedOverlay: Int
    ) {
        val front = pBlockEntity.blockState.getValue(FACING)
        val normal = front.normal
        val input = pBlockEntity.getItemInSlot(0)
        val tool = pBlockEntity.getItemInSlot(1)
        val realPercent = ((pBlockEntity.progress / pBlockEntity.targetProgress.toFloat()) * 100)
        val percent = ((pBlockEntity.progress / pBlockEntity.targetProgress.toFloat()) * 360)
        val scaled = percent * 0.01
        val realScaled = realPercent * 0.01
        if (rot > 270 || decreasing) {
            rot -= Minecraft.getInstance().deltaFrameTime * 2
            decreasing = true
        } else rot += Minecraft.getInstance().deltaFrameTime * 2

        if (rot < 180 && decreasing) {
            decreasing = false
        }
        var visble = true
        if(realPercent <=10) visble = false
        if (pBlockEntity.getItemInSlot(0).isEmpty || pBlockEntity.getItemInSlot(1).isEmpty) visble = false
        fullRot += Minecraft.getInstance().deltaFrameTime * ((realPercent * 0.5))
        fullRot %= 360
        rot %= 360
//        rot = 0.0
        stack.pushPose()
//        player.render(stack, pBufferSource.getBuffer(), pPackedLight, pPackedOverlay)
        stack.translate(0.5 + (normal.x * 0.25), 0.9, 0.5 + (normal.z * 0.25))
        if (visble) {
            when (front) {
                Direction.NORTH -> {

                    model.rightArm.xRot = Math.toRadians(rot).toFloat()
                    model.rightArm.zRot = 0F
                    model.leftArm.xRot = Math.toRadians(rot).toFloat()
                    stack.mulPose(
                        Quaternion.fromXYZ(
                            Math.toRadians(180.0).toFloat(),
                            Math.toRadians(0.0).toFloat(),
                            0f
                        )
                    )
                }
                Direction.SOUTH -> {
                    model.rightArm.xRot = Math.toRadians(rot).toFloat()
                    model.leftArm.xRot = Math.toRadians(rot).toFloat()
                    model.rightArm.zRot = 0F
                    stack.mulPose(
                        Quaternion.fromXYZ(
                            Math.toRadians(180.0).toFloat(),
                            Math.toRadians(180.0).toFloat(),
                            0f
                        )
                    )
                }
                Direction.EAST -> {
                    model.rightArm.zRot = Math.toRadians(rot).toFloat()
                    model.leftArm.xRot = Math.toRadians(rot).toFloat()
                    model.rightArm.xRot = 0F
                    stack.mulPose(
                        Quaternion.fromXYZ(
                            0f,
                            Math.toRadians(90.0).toFloat(),
                            Math.toRadians(180.0).toFloat(),
                        )
                    )
                }
                Direction.WEST -> {
                    model.rightArm.xRot = Math.toRadians(rot).toFloat()
                    model.leftArm.xRot = Math.toRadians(rot).toFloat()
                    model.rightArm.zRot = 0F
                    stack.mulPose(
                        Quaternion.fromXYZ(
                            0f,
                            Math.toRadians(270.0).toFloat(),
                            Math.toRadians(180.0).toFloat(),
                        )
                    )
                }
            }
            stack.scale(0.45f, 0.45f, 0.45f)
            model.renderToBuffer(
                stack,
                pBufferSource.getBuffer(model.renderType(Minecraft.getInstance().player!!.skinTextureLocation)),
                pPackedLight,
                pPackedOverlay,
                1f,
                1f,
                1f,
                1f
            )
        }
        stack.popPose()
        stack.pushPose()
//        stack.translate(0.5 + (normal.x * 0.25), 0.9, 0.5 + (normal.z * 0.25))

        stack.translate(0.5, 0.8, 0.5)
        var factor = 0.08
        val value = if (visble) fullRot else 0.0
        when (front) {
            Direction.NORTH -> {
                stack.mulPose(
                    Quaternion.fromXYZ(
                        Math.toRadians(0.0).toFloat(),
                        Math.toRadians(270 - value).toFloat(),
                        Math.toRadians(180.0).toFloat(),
                    )
                )
            }
            Direction.SOUTH -> {
                stack.mulPose(
                    Quaternion.fromXYZ(
                        Math.toRadians(0.0).toFloat(),
                        Math.toRadians(90.0 - value).toFloat(),
                        Math.toRadians(180.0).toFloat(),
                    )
                )

            }
            Direction.EAST -> {
                stack.mulPose(
                    Quaternion.fromXYZ(
                        Math.toRadians(0.0).toFloat(),
                        Math.toRadians(270.0 - value).toFloat(),
                        Math.toRadians(180.0).toFloat(),
                    )
                )
            }
            Direction.WEST -> {
                stack.mulPose(
                    Quaternion.fromXYZ(
                        Math.toRadians(0.0).toFloat(),
                        Math.toRadians(270.0 - value).toFloat(),
                        Math.toRadians(180.0).toFloat(),
                    )
                )

            }
        }


        stack.scale(0.5f, 0.5f, 0.5f)
        Minecraft.getInstance().itemRenderer.renderStatic(
            tool,
            ItemTransforms.TransformType.GROUND,
            15728880,
            OverlayTexture.NO_OVERLAY,
            stack,
            pBufferSource,
            0
        )
        stack.popPose()

        stack.pushPose()
        stack.translate(0.5, 0.5, 0.5)
        stack.mulPose(Quaternion.fromXYZ(0f, 0f, 0f))
        val scale = 0.1f + (realScaled.toFloat() * 0.2f)
        if (visble)
            stack.scale(rot.toFloat() / 360, rot.toFloat() / 360, rot.toFloat() / 360)
        else
            stack.scale(0.8f, 0.8f, 0.8f)
        val x: Double = pBlockEntity.blockPos.x.toDouble() + 0.5
        val y: Double = pBlockEntity.blockPos.y.toDouble() + 0.5
        val z: Double = pBlockEntity.blockPos.z.toDouble() + 0.5
        if (realPercent >= 90) {
            Minecraft.getInstance().level?.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0)
        }
        if (realPercent >= 98) {
//            Minecraft.getInstance().level?.playSound(
//                Minecraft.getInstance().player,
//                x,
//                y,
//                z,
//                Registry.Sounds.Oof,
//                SoundSource.BLOCKS,
//                1f,
//                1f
//            )
            Minecraft.getInstance().level?.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0)

        }
        Minecraft.getInstance().itemRenderer.renderStatic(
            input,
            ItemTransforms.TransformType.GROUND,
            15728880,
            OverlayTexture.NO_OVERLAY,
            stack,
            pBufferSource,
            0
        )
        stack.popPose()
    }
}