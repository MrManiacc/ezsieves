package com.github.toysol.registry

import com.github.toysol.Toys
import com.github.toysol.content.block.SieveBlock
import com.github.toysol.content.container.SieveContainer
import com.github.toysol.content.item.SieveItem
import com.github.toysol.content.render.SieveRenderer
import com.github.toysol.content.screen.SieveScreen
import com.github.toysol.recipes.SieveRecipe
import com.github.toysol.recipes.SieveRecipe.Serializer
import com.github.toysol.registry.internal.IRegister
import com.github.toysol.registry.internal.MojangRegistry
import com.github.toysol.registry.internal.Registry
import com.github.toysol.content.tile.SieveTile
import com.github.toysol.util.registerAll
import com.github.toysol.util.resLoc
import com.github.toysol.util.tile
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.RenderType
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Material
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.registries.ForgeRegistries

object Registry : IRegister {

    /**
     * ========================Blocks registry========================
     */
    object Blocks : Registry<Block>(ForgeRegistries.BLOCKS) {
        val Sieve by register("sieve") { SieveBlock(BlockBehaviour.Properties.of(Material.WOOD)) }
    }

    /**
     * ========================Tiles registry========================
     */
    object Tiles : Registry<BlockEntityType<*>>(ForgeRegistries.BLOCK_ENTITIES) {
        val Sieve by register("sieve") { tile(Blocks.Sieve) { SieveTile(it.first, it.second) } }
        override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {

            super.register(modId, modBus, forgeBus)

        }
    }

    /**
     * ========================Items registry========================
     */
    object Items : Registry<Item>(ForgeRegistries.ITEMS) {
        val Sieve by register("sieve") { SieveItem() }
    }


    /**
     * ========================Containers registry========================
     */
    object Containers : Registry<MenuType<*>>(ForgeRegistries.CONTAINERS) {
        val Sieve: MenuType<SieveContainer> by register("sieve") {
            MenuType { id, inv ->
                SieveContainer(
                    id,
                    inv,
                )
            }
        }

        override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {
            super.register(modId, modBus, forgeBus)
            modBus.addListener<FMLClientSetupEvent> {
                MenuScreens.register(Sieve) { menu, inv, comp -> SieveScreen(menu, inv, comp) }
                ItemBlockRenderTypes.setRenderLayer(Blocks.Sieve, RenderType.translucent());
            }
        }
    }


    /**
     * Stores our entities
     */
    object Recipes : Registry<RecipeSerializer<*>>(ForgeRegistries.RECIPE_SERIALIZERS) {
        val SieveSerializer by register("sieve") { Serializer }
    }

    object RecipeTypes :
        MojangRegistry<net.minecraft.core.Registry<RecipeType<*>>, RecipeType<*>>(net.minecraft.core.Registry.RECIPE_TYPE_REGISTRY) {
        val Sieve: RecipeType<SieveRecipe> by register("sieve") {
            object : RecipeType<SieveRecipe> {
                override fun toString(): String {
                    return "sieve".resLoc.toString()
                }
            }
        }
    }

    /**
     * Stores our sounds as events that can be played in the world
     */
    object Sounds : Registry<SoundEvent>(ForgeRegistries.SOUND_EVENTS) {
        val Oof by register(Toys.ModId) { SoundEvent("oof".resLoc) }
    }

    /**
     * Stores our configurations
     */
    object Config : IRegister {
        override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {

        }

    }


    override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {
        registerAll(modId, modBus, forgeBus)

        modBus.addListener<FMLClientSetupEvent> {
            ItemBlockRenderTypes.setRenderLayer(Blocks.Sieve, RenderType.cutoutMipped())
        }

        modBus.addListener<RegisterRenderers> {
            it.registerBlockEntityRenderer(Tiles.Sieve) { SieveRenderer() }
        }
    }

}