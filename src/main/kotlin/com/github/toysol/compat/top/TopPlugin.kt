package com.github.toysol.compat.top

import com.github.sieves.content.tile.SieveTile
import mcjty.theoneprobe.api.ITheOneProbe
import mcjty.theoneprobe.apiimpl.styles.TextStyle
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.item.ItemStack
import java.util.function.Function


class TopPlugin : Function<ITheOneProbe, Unit> {
    var formattedName: MutableComponent =
        TextComponent("sieves").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.ITALIC)

    override fun apply(t: ITheOneProbe) = with(t) {
        registerBlockDisplayOverride { probeMode, inf, player, level, blockState, data ->
            val tile = level.getBlockEntity(data.pos)
            if (tile is SieveTile) {
                inf.horizontal()
                    .item(ItemStack(blockState.block.asItem()))
                    .vertical()
                    .text(TextComponent("§6Sieves"), TextStyle().topPadding(5))
                probeMode.name

                inf.text("progress")
                    .progress(tile.percent, 100)

                inf.horizontal()
                    .item(tile.getItemInSlot(0))
                    .text(" + ", TextStyle().topPadding(8))
                    .horizontal()
                    .item(tile.getItemInSlot(1))
                    .text(" → ", TextStyle().topPadding(8))
                    .horizontal()
                    .item(tile.getItemInSlot(0, true))


            }
            true
        }
    }
}