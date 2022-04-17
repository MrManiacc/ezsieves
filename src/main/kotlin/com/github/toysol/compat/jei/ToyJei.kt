package com.github.toysol.compat.jei

import com.github.toysol.Toys
import com.github.toysol.recipes.SieveRecipe
import com.github.toysol.registry.Registry.RecipeTypes.Sieve
import com.github.toysol.util.resLoc
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.RecipeType.*
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.Container
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeType

@JeiPlugin
class ToyJei : IModPlugin {
    private val pluginId = "toysol".resLoc

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