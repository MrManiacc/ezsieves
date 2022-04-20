package com.github.sieves.registry

import com.github.sieves.recipes.SieveRecipe
import com.github.sieves.recipes.SieveRecipe.Serializer
import com.github.sieves.util.resLoc
import com.github.sieves.util.tile
import com.github.sieves.compat.top.TopPlugin
import com.github.sieves.content.api.tab.NativeTabs
import com.github.sieves.content.api.tab.Tab
import com.github.sieves.content.api.tab.TabRegistry
import com.github.sieves.content.upgrade.Upgrade
import com.github.sieves.content.battery.*
import com.github.sieves.content.farmer.*
import com.github.sieves.content.link.LinkItem
import com.github.sieves.content.sieve.*
import com.github.sieves.registry.internal.*
import com.github.sieves.registry.internal.Registry
import com.github.sieves.registry.internal.net.*
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Material
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.InterModComms
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent
import net.minecraftforge.network.IContainerFactory
import net.minecraftforge.registries.ForgeRegistries

internal object Registry : ListenerRegistry() {

    /**
     * ========================Blocks registry========================
     */
    object Blocks : Registry<Block>(ForgeRegistries.BLOCKS) {
        val Sieve by register("sieve") { SieveBlock(BlockBehaviour.Properties.of(Material.STONE)) }
        val Battery by register("battery") { BatteryBlock(BlockBehaviour.Properties.of(Material.HEAVY_METAL)) }
        val Farmer by register("farmer") { FarmerBlock(BlockBehaviour.Properties.of(Material.HEAVY_METAL)) }
    }

    /**
     * ========================Tiles registry========================
     */
    object Tiles : Registry<BlockEntityType<*>>(ForgeRegistries.BLOCK_ENTITIES) {
        val Sieve by register("sieve") { tile(Blocks.Sieve) { SieveTile(it.first, it.second) } }
        val Battery by register("battery") { tile(Blocks.Battery) { BatteryTile(it.first, it.second) } }
        val Farmer by register("farmer") { tile(Blocks.Farmer) { FarmerTile(it.first, it.second) } }

    }

    /**
     * ========================Items registry========================
     */
    object Items : Registry<Item>(ForgeRegistries.ITEMS) {
        val CreativeTab = object : CreativeModeTab("sieves") {
            override fun makeIcon(): ItemStack = ItemStack(SpeedUpgrade)
        }
        val Sieve by register("sieve") { SieveItem() }
        val Battery by register("battery") { BatteryItem() }
        val Farmer by register("farmer") { FarmerItem() }
        val Linker by register("linker") { LinkItem() }
        val SpeedUpgrade by register("speed") { Upgrade(0, 16) }
        val EfficiencyUpgrade by register("efficiency") { Upgrade(1, 16) }
    }

    /**
     * ========================Items registry========================
     */
    object Tabs : IRegister {
        val PlayerPower by Tab.register("player_power_tab".resLoc, NativeTabs.PowerModule::Properties)
        val PlayerHunger by Tab.register("player_hunger_tab".resLoc, NativeTabs.HungerModule::Properties)

        override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) =
            TabRegistry.register(modId, modBus, forgeBus)
    }

    /**
     * ========================Containers registry========================
     */
    object Containers : Registry<MenuType<*>>(ForgeRegistries.CONTAINERS) {
        val Sieve: MenuType<SieveContainer> by register("sieve") {
            MenuType(IContainerFactory { id, inv, data ->
                SieveContainer(
                    id,
                    inv,
                    pos = data.readBlockPos()
                )
            })
        }

        val Battery: MenuType<BatteryContainer> by register("battery") {
            MenuType(IContainerFactory { id, inv, data ->
                BatteryContainer(id, inv, data.readBlockPos())
            })
        }

        val Farmer: MenuType<FarmerContainer> by register("farmer") {
            MenuType(IContainerFactory { id, inv, data ->
                FarmerContainer(id, inv, data.readBlockPos())
            })
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
     * Handles our network registrations
     */
    object Net : NetworkRegistry() {
        val Configure by register(0) { ConfigurePacket() }
        val GrowBlock by register(1) { GrowBlockPacket() }
        val HarvestBlock by register(2) { HarvestBlockPacket() }
        val TakeUpgrade by register(3) { TakeUpgradePacket() }
        val SyncTab by register(4) { TabUpdatePacket() }
        val BindTab by register(5) { TabBindPacket() }
        val ClickTab by register(6) { TabClickedPacket() }
    }

    @Sub
    fun onClientSetup(event: FMLClientSetupEvent) {
        ItemBlockRenderTypes.setRenderLayer(Blocks.Sieve, RenderType.cutoutMipped())
        ItemBlockRenderTypes.setRenderLayer(Blocks.Battery, RenderType.cutoutMipped())
        ItemBlockRenderTypes.setRenderLayer(Blocks.Farmer, RenderType.cutoutMipped())
        MenuScreens.register(Containers.Sieve) { menu, inv, comp -> SieveScreen(menu, inv, comp) }
        MenuScreens.register(Containers.Battery) { menu, inv, _ -> BatteryScreen(menu, inv) }
        MenuScreens.register(Containers.Farmer) { menu, inv, _ -> FarmerScreen(menu, inv) }
    }

    @Sub
    fun onRendererRegister(event: RegisterRenderers) {
        event.registerBlockEntityRenderer(Tiles.Sieve) { SieveRenderer() }
        event.registerBlockEntityRenderer(Tiles.Battery) { BatteryRenderer() }
        event.registerBlockEntityRenderer(Tiles.Farmer) { FarmerRenderer() }
    }

    @Sub
    fun onInterModEnqueue(event: InterModEnqueueEvent) {
        if (ModList.get().isLoaded("theoneprobe")) {
            InterModComms.sendTo("theoneprobe", "getTheOneProbe") { TopPlugin() }
        }
    }


}