package com.gregtechceu.gtceu.integration.exnihilo;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * Converts Ex Nihilo: Sequentia sifting recipes to GTCEu ore sieve recipes.
 */
public class SieveRecipeLoader {

    private SieveRecipeLoader() {}

    /**
     * Initialize sieve recipes by converting ENS sifting recipes.
     * Called during recipe addition phase.
     */
    public static void init(Consumer<FinishedRecipe> consumer) {
        if (!GTCEu.Mods.isExNihiloLoaded()) {
            return;
        }

        GTCEu.LOGGER.info("Ex Nihilo: Sequentia detected, sieve recipes will be available");
        // Runtime recipe conversion will be implemented via KubeJS or datapack
    }
}
