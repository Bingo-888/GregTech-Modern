package com.gregtechceu.gtceu.integration.exnihilo;

import com.gregtechceu.gtceu.GTCEu;

/**
 * Integration module for Ex Nihilo: Sequentia mod.
 * Handles sieve recipe conversion and related functionality.
 */
public class ExNihiloModule {

    private ExNihiloModule() {}

    public static void init() {
        if (!GTCEu.Mods.isExNihiloLoaded()) {
            return;
        }
        GTCEu.LOGGER.info("Ex Nihilo: Sequentia integration initialized");
    }
}
