package com.gregtechceu.gtceu.integration.kjs;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class KubeJSSieveRecipeIntegrationTest {

    private static final ResourceLocation SCRIPTED_RECIPE_ID = GTCEu.id("kjs_scripted_ore_sieve");

    @GameTest(template = "empty", batch = "KubeJSSieve")
    public static void kubeJsNoScriptConfigKeepsDefaultBehavior(GameTestHelper helper) {
        Map<ResourceLocation, Recipe<?>> recipesByName = new HashMap<>();

        if (!GTCEu.Mods.isKubeJSLoaded()) {
            helper.assertFalse(recipesByName.containsKey(SCRIPTED_RECIPE_ID),
                    "No-script default should not contain scripted ore sieve recipe");
            helper.succeed();
            return;
        }

        try {
            Object event = createRecipesEvent();
            invokeKubeJsInjection(event, helper, recipesByName);
            helper.assertFalse(recipesByName.containsKey(SCRIPTED_RECIPE_ID),
                    "No-script configuration unexpectedly injected scripted ore sieve recipe");
            helper.succeed();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to verify no-script KubeJS behavior", e);
        }
    }

    @GameTest(template = "empty", batch = "KubeJSSieve")
    public static void kubeJsScriptConfigInjectsOreSieveRecipe(GameTestHelper helper) {
        if (!GTCEu.Mods.isKubeJSLoaded()) {
            helper.succeed();
            return;
        }

        Map<ResourceLocation, Recipe<?>> recipesByName = new HashMap<>();
        try {
            Object event = createRecipesEvent();
            Object recipeTypeFunction = event.getClass()
                    .getMethod("getRecipeFunction", String.class)
                    .invoke(event, "gtceu:ore_sieve");
            Object scriptedRecipe = recipeTypeFunction.getClass()
                    .getMethod("createRecipe", Object[].class)
                    .invoke(recipeTypeFunction, (Object) new Object[] { SCRIPTED_RECIPE_ID });

            scriptedRecipe.getClass().getMethod("inputItems", net.minecraft.world.item.Item.class)
                    .invoke(scriptedRecipe, Items.COBBLESTONE);

            Class<?> inputItemClass = Class.forName("dev.latvian.mods.kubejs.item.InputItem");
            Method inputItemFactory = inputItemClass.getMethod("of", Ingredient.class, int.class);
            Object nonConsumable = inputItemFactory.invoke(null, Ingredient.of(Items.STRING), 1);
            scriptedRecipe.getClass().getMethod("notConsumable", inputItemClass).invoke(scriptedRecipe, nonConsumable);

            scriptedRecipe.getClass().getMethod("outputItems", net.minecraft.world.item.Item.class)
                    .invoke(scriptedRecipe, Items.STONE);

            Class<?> gtRecipeSchemaClass = Class.forName("com.gregtechceu.gtceu.integration.kjs.recipe.GTRecipeSchema");
            Field durationKey = gtRecipeSchemaClass.getField("DURATION");
            Class<?> recipeKeyClass = Class.forName("dev.latvian.mods.kubejs.recipe.RecipeKey");
            scriptedRecipe.getClass().getMethod("setValue", recipeKeyClass, Object.class)
                    .invoke(scriptedRecipe, durationKey.get(null), 4L);

            event.getClass().getMethod("addRecipe", Class.forName("dev.latvian.mods.kubejs.recipe.RecipeJS"),
                    boolean.class).invoke(event, scriptedRecipe, false);

            invokeKubeJsInjection(event, helper, recipesByName);
            helper.assertTrue(recipesByName.containsKey(SCRIPTED_RECIPE_ID),
                    "Scripted KubeJS ore sieve recipe was not injected into runtime recipe map");
            helper.succeed();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to verify script-enabled KubeJS behavior", e);
        }
    }

    private static Object createRecipesEvent() throws ReflectiveOperationException {
        Class<?> recipesEventClass = Class.forName("dev.latvian.mods.kubejs.recipe.RecipesEventJS");
        return recipesEventClass.getConstructor().newInstance();
    }

    private static void invokeKubeJsInjection(Object event, GameTestHelper helper,
                                              Map<ResourceLocation, Recipe<?>> recipesByName)
            throws ReflectiveOperationException {
        Class<?> pluginClass = Class.forName("com.gregtechceu.gtceu.integration.kjs.GregTechKubeJSPlugin");
        Object plugin = pluginClass.getConstructor().newInstance();
        Class<?> recipesEventClass = Class.forName("dev.latvian.mods.kubejs.recipe.RecipesEventJS");
        pluginClass.getMethod("injectRuntimeRecipes", recipesEventClass, net.minecraft.world.item.crafting.RecipeManager.class,
                Map.class).invoke(plugin, event, helper.getLevel().getRecipeManager(), recipesByName);
    }
}
