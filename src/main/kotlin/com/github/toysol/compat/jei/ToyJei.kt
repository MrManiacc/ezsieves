package com.github.sieves.compat.jei

import com.github.sieves.Toys
import com.github.sieves.recipes.SieveRecipe
import com.github.sieves.registry.Registry
import com.github.sieves.registry.Registry.RecipeTypes.Sieve
import com.github.sieves.util.resLoc
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.RecipeType.*
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.Container
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeType

@JeiPlugin
class ToyJei : IModPlugin {
    private val pluginId = "sieves".resLoc

    override fun getPluginUid(): ResourceLocation {
        return pluginId
    }

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        val guiHelper: IGuiHelper = registration.jeiHelpers.guiHelper
        registration.addRecipeCategories(SieveRecipeCategory(guiHelper))
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        register("sieve", Sieve, registration)
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        registration.addRecipeCatalyst(
            VanillaTypes.ITEM_STACK, ItemStack(Registry.Blocks.Sieve), mezz.jei.api.recipe.RecipeType(
                "sieve".resLoc, SieveRecipe::class.java
            )
        )

    }

    private inline fun <reified T : Recipe<C>, C : Container> register(
        name: String,
        type: RecipeType<T>,
        registration: IRecipeRegistration
    ) {
        val rm = Minecraft.getInstance().level?.recipeManager ?: return
        val types = rm.getAllRecipesFor(type)
        registration.addRecipes(
            create(Toys.ModId, name, T::class.java),
            types
        )
    }
}