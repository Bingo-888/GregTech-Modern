package com.gregtechceu.gtceu.integration.exnihilo;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import novamachina.exnihilosequentia.world.item.MeshType;
import novamachina.exnihilosequentia.world.item.crafting.EXNRecipeTypes;
import novamachina.exnihilosequentia.world.item.crafting.MeshWithChance;
import novamachina.exnihilosequentia.world.item.crafting.SiftingRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Converts Ex Nihilo: Sequentia sifting recipes to GTCEu ore sieve recipes at runtime.
 * <p>
 * ENS sifting recipes have the structure: (input block, output drop, List&lt;MeshWithChance&gt; rolls).
 * Each roll specifies which mesh tier can produce this drop and at what probability.
 * <p>
 * This loader groups all ENS drops by (input ingredient, mesh type), then creates one GT ore_sieve
 * recipe per group with the input block, the mesh as a non-consumed input, and all drops as
 * chanced outputs.
 * <p>
 * Called during the recipe staging phase in {@code RecipeManagerMixin} and
 * {@code GregTechKubeJSPlugin}, after the vanilla RecipeManager has parsed all recipe JSON.
 */
public class SieveRecipeLoader {

    private SieveRecipeLoader() {}

    /** Duration in ticks for each sieve recipe (10 seconds). */
    private static final int SIEVE_DURATION = 200;

    /** Mapping from ENS MeshType to the corresponding mesh item registry name. */
    private static final Map<MeshType, ResourceLocation> MESH_ITEMS = new EnumMap<>(MeshType.class);

    static {
        MESH_ITEMS.put(MeshType.STRING, new ResourceLocation("exnihilosequentia", "string_mesh"));
        MESH_ITEMS.put(MeshType.FLINT, new ResourceLocation("exnihilosequentia", "flint_mesh"));
        MESH_ITEMS.put(MeshType.IRON, new ResourceLocation("exnihilosequentia", "iron_mesh"));
        MESH_ITEMS.put(MeshType.DIAMOND, new ResourceLocation("exnihilosequentia", "diamond_mesh"));
        MESH_ITEMS.put(MeshType.EMERALD, new ResourceLocation("exnihilosequentia", "emerald_mesh"));
        MESH_ITEMS.put(MeshType.NETHERITE, new ResourceLocation("exnihilosequentia", "netherite_mesh"));
    }

    /**
     * Injects converted sieve recipes from a nested recipe map (used by RecipeManagerMixin).
     * <p>
     * Must be called while the {@code SIEVE_RECIPES} type's staging is active
     * (between {@code beginStaging()} and {@code completeStaging()}).
     *
     * @param recipesByType the nested recipe map from RecipeManager, keyed by RecipeType
     */
    public static void injectSieveRecipes(
                                          @NotNull Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType) {
        if (!GTCEu.Mods.isExNihiloLoaded()) return;

        RecipeType<SiftingRecipe> ensSifting = EXNRecipeTypes.SIFTING;
        Map<ResourceLocation, Recipe<?>> siftingMap = recipesByType.get(ensSifting);
        if (siftingMap == null || siftingMap.isEmpty()) {
            GTCEu.LOGGER.info("No Ex Nihilo sifting recipes found to convert");
            return;
        }
        doInject(siftingMap);
    }

    /**
     * Injects converted sieve recipes from a flat recipe map (used by GregTechKubeJSPlugin).
     * <p>
     * Must be called while the {@code SIEVE_RECIPES} type's staging is active
     * (between {@code beginStaging()} and {@code completeStaging()}).
     *
     * @param allRecipes the flat recipe map from KubeJS, keyed by ResourceLocation
     */
    public static void injectSieveRecipesFlat(@NotNull Map<ResourceLocation, Recipe<?>> allRecipes) {
        if (!GTCEu.Mods.isExNihiloLoaded()) return;

        // Filter ENS sifting recipes from the flat map
        Map<ResourceLocation, Recipe<?>> siftingMap = new HashMap<>();
        RecipeType<SiftingRecipe> ensSifting = EXNRecipeTypes.SIFTING;
        for (var entry : allRecipes.entrySet()) {
            if (entry.getValue().getType() == ensSifting) {
                siftingMap.put(entry.getKey(), entry.getValue());
            }
        }
        if (siftingMap.isEmpty()) {
            GTCEu.LOGGER.info("No Ex Nihilo sifting recipes found to convert");
            return;
        }
        doInject(siftingMap);
    }

    /**
     * Core injection logic shared by both entry points.
     * Expects a map containing only ENS sifting recipes.
     */
    private static void doInject(Map<ResourceLocation, Recipe<?>> siftingMap) {
        // Collect valid SiftingRecipe instances (skip waterlogged)
        List<SiftingRecipe> siftingRecipes = new ArrayList<>();
        for (Recipe<?> recipe : siftingMap.values()) {
            if (recipe instanceof SiftingRecipe sifting && !sifting.isWaterlogged()) {
                siftingRecipes.add(sifting);
            }
        }
        if (siftingRecipes.isEmpty()) {
            GTCEu.LOGGER.info("No non-waterlogged Ex Nihilo sifting recipes found to convert");
            return;
        }

        // Group drops by (input, mesh) -> list of (drop, chance)
        Map<GroupKey, List<DropEntry>> grouped = groupByInputAndMesh(siftingRecipes);

        // Convert each group into a GT ore_sieve recipe and stage it
        GTRecipeType sieveType = GTRecipeTypes.SIEVE_RECIPES;
        int count = 0;
        for (var entry : grouped.entrySet()) {
            GTRecipe recipe = buildSieveRecipe(sieveType, entry.getKey(), entry.getValue(), count);
            if (recipe != null) {
                sieveType.getAdditionHandler().addStaging(recipe);
                count++;
            }
        }

        GTCEu.LOGGER.info("Converted {} Ex Nihilo sifting recipe groups into GT ore sieve recipes", count);
    }

    /**
     * Groups sifting recipe drops by (input ingredient, mesh type).
     * <p>
     * Each ENS SiftingRecipe has one input, one drop, and multiple rolls (mesh+chance pairs).
     * We expand each roll into a separate group entry so that all drops for the same
     * (input, mesh) combination end up together.
     */
    private static Map<GroupKey, List<DropEntry>> groupByInputAndMesh(List<SiftingRecipe> recipes) {
        Map<GroupKey, List<DropEntry>> grouped = new LinkedHashMap<>();

        for (SiftingRecipe recipe : recipes) {
            Ingredient input = recipe.getInput();
            ItemStack drop = recipe.getDrop();

            for (MeshWithChance roll : recipe.getRolls()) {
                MeshType mesh = roll.getMesh();
                if (mesh == MeshType.NONE) continue;
                if (!MESH_ITEMS.containsKey(mesh)) continue;

                GroupKey key = new GroupKey(input, mesh);
                float chance = roll.getChance();
                grouped.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(new DropEntry(drop, chance));
            }
        }
        return grouped;
    }

    /**
     * Builds a single GT ore_sieve recipe from a group of drops sharing the same input and mesh.
     */
    private static GTRecipe buildSieveRecipe(GTRecipeType sieveType, GroupKey key,
                                             List<DropEntry> drops, int index) {
        ResourceLocation meshItemId = MESH_ITEMS.get(key.mesh);
        if (meshItemId == null) return null;

        ItemStack meshStack = getMeshItemStack(meshItemId);
        if (meshStack.isEmpty()) {
            GTCEu.LOGGER.warn("Could not find mesh item: {}", meshItemId);
            return null;
        }

        // Build a unique recipe ID based on index
        String recipeId = "ens_sieve_" + key.mesh.getSerializedName() + "_" + index;
        GTRecipeBuilder builder = sieveType.recipeBuilder(GTCEu.id(recipeId));

        // Input: the siftable block
        builder.inputItems(key.input);
        // Non-consumed input: the mesh
        builder.notConsumable(meshStack);

        // Add each drop as a chanced output
        for (DropEntry drop : drops) {
            int gtChance = floatChanceToGT(drop.chance);
            if (gtChance <= 0) continue;
            builder.chancedOutput(drop.drop.copy(), gtChance, 0);
        }

        builder.duration(SIEVE_DURATION);

        return builder.buildRawRecipe();
    }

    /**
     * Converts a float chance (0.0-1.0) to GT's integer chance scale (0-10000).
     */
    private static int floatChanceToGT(float chance) {
        return Math.max(1, Math.min(10000, Math.round(chance * 10000)));
    }

    /**
     * Resolves a mesh item's ResourceLocation to an ItemStack via the Forge registry.
     */
    private static ItemStack getMeshItemStack(ResourceLocation itemId) {
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    /**
     * Composite key for grouping sifting drops by input ingredient and mesh type.
     * <p>
     * Uses the serialized JSON form of the Ingredient for equality, since Ingredient
     * itself does not implement equals/hashCode reliably.
     */
    private record GroupKey(Ingredient input, MeshType mesh) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GroupKey other)) return false;
            return mesh == other.mesh && input.toJson().toString().equals(other.input.toJson().toString());
        }

        @Override
        public int hashCode() {
            return Objects.hash(input.toJson().toString(), mesh);
        }
    }

    /**
     * A single drop with its associated chance.
     */
    private record DropEntry(ItemStack drop, float chance) {}
}
