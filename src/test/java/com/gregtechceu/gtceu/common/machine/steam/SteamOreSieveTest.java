package com.gregtechceu.gtceu.common.machine.steam;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.steam.SimpleSteamMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.gametest.util.TestUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class SteamOreSieveTest {

    private static GTRecipeType SIEVE_TEST_RECIPE_TYPE;

    @BeforeBatch(batch = "SteamOreSieve")
    public static void prepare(ServerLevel level) {
        SIEVE_TEST_RECIPE_TYPE = TestUtils.createRecipeType("steam_ore_sieve_tests", GTRecipeTypes.SIEVE_RECIPES);
        SIEVE_TEST_RECIPE_TYPE.getAdditionHandler().beginStaging();
        SIEVE_TEST_RECIPE_TYPE.getAdditionHandler().addStaging(SIEVE_TEST_RECIPE_TYPE
                .recipeBuilder(GTCEu.id("test_steam_ore_sieve"))
                .inputItems(new ItemStack(Blocks.COBBLESTONE))
                .notConsumable(new ItemStack(Items.STRING))
                .outputItems(new ItemStack(Blocks.STONE))
                .EUt(GTValues.VA[GTValues.ULV])
                .duration(4)
                .buildRawRecipe());
        SIEVE_TEST_RECIPE_TYPE.getAdditionHandler().completeStaging();
    }

    @GameTest(template = "empty", batch = "SteamOreSieve", timeoutTicks = 80)
    public static void steamOreSieveProcessesBasicRecipe(GameTestHelper helper) {
        SimpleSteamMachine machine = (SimpleSteamMachine) TestUtils.setMachine(helper, new BlockPos(0, 1, 0),
                GTMachines.STEAM_ORE_SIEVE.right());

        machine.setRecipeType(SIEVE_TEST_RECIPE_TYPE);
        machine.steamTank.fillInternal(GTMaterials.Steam.getFluid(1000), IFluidHandler.FluidAction.EXECUTE);

        NotifiableItemStackHandler itemIn = machine.importItems;
        NotifiableItemStackHandler itemOut = machine.exportItems;

        itemIn.setStackInSlot(0, new ItemStack(Blocks.COBBLESTONE));
        itemIn.setStackInSlot(1, new ItemStack(Items.STRING));

        helper.succeedOnTickWhen(5, () -> {
            TestUtils.assertEqual(helper, itemOut.getStackInSlot(0), new ItemStack(Blocks.STONE));
            TestUtils.assertEqual(helper, itemIn.getStackInSlot(1), new ItemStack(Items.STRING));
            helper.assertTrue(machine.steamTank.getFluidInTank(0).getAmount() < 1000,
                    "Steam Ore Sieve did not consume steam");
        });
    }

    @GameTest(template = "empty", batch = "SteamOreSieve")
    public static void steamOreSieveSkipsEnsInjectionWithoutExNihilo(GameTestHelper helper) {
        helper.assertFalse(GTCEu.Mods.isExNihiloLoaded(), "Ex Nihilo is unexpectedly loaded in this test environment");

        boolean hasInjectedEnsRecipe = helper.getLevel().getRecipeManager().getAllRecipesFor(GTRecipeTypes.SIEVE_RECIPES)
                .stream()
                .anyMatch(recipe -> recipe.getId().getNamespace().equals(GTCEu.MOD_ID) &&
                        recipe.getId().getPath().startsWith("ens_sieve_"));

        helper.assertFalse(hasInjectedEnsRecipe,
                "Found Ex Nihilo-injected sieve recipes while Ex Nihilo is not loaded");
        helper.succeed();
    }
}
