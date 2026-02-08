package com.gregtechceu.gtceu.common.machine.steam;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.PredicatedImageWidget;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.steam.SimpleSteamMachine;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A specialized steam sieve machine with a vertically extended GUI.
 * <p>
 * Because the ore sieve can have up to 36 chanced outputs, the standard 176x166 GUI
 * would overflow. This machine dynamically increases the GUI height to fit a scrollable
 * output slot area between the input/progress section and the player inventory.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleSteamSieveMachine extends SimpleSteamMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SimpleSteamSieveMachine.class, SimpleSteamMachine.MANAGED_FIELD_HOLDER);

    /** Number of columns in the output slot grid */
    private static final int OUTPUT_COLS = 9;
    /** Maximum visible rows for output slots (reduced to keep GUI reasonable height) */
    private static final int MAX_VISIBLE_ROWS = 3;

    public SimpleSteamSieveMachine(IMachineBlockEntity holder, boolean isHighPressure, Object... args) {
        super(holder, isHighPressure, args);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        int totalOutputSlots = getRecipeType().getMaxOutputs(ItemRecipeCapability.CAP);
        int maxInputs = getRecipeType().getMaxInputs(ItemRecipeCapability.CAP);
        int cols = OUTPUT_COLS;
        int totalRows = (totalOutputSlots + cols - 1) / cols;
        int visibleRows = Math.min(totalRows, MAX_VISIBLE_ROWS);

        // --- Layout geometry ---
        // Layout from top to bottom:
        // y=5: Title
        // y=20: Input slots (3 slots = 54px)
        // y=74: Output scrollable area (3 rows max = 54px)
        // y=134: Player inventory (76px height including backpack + hotbar)
        // Total: ~210px
        int guiWidth = 176;
        int titleY = 5;
        int inputY = 20;
        int inputX = 7;
        int progressX = 33;
        int progressY = 29;

        // Output scrollable area below input slots
        int slotSize = 18;
        int scrollBarWidth = 8;
        int outputAreaX = 7;
        int outputAreaY = inputY + maxInputs * slotSize + 4;
        int outputContentW = cols * slotSize;
        int outputContentH = visibleRows * slotSize;
        int outputWidgetW = outputContentW + scrollBarWidth + 4;

        // Player inventory (76px = 54px backpack + 4px gap + 18px hotbar)
        int playerInvY = outputAreaY + outputContentH + 6;
        int guiHeight = playerInvY + 76;

        var modularUI = new ModularUI(guiWidth, guiHeight, this, entityPlayer)
                .background(GuiTextures.BACKGROUND_STEAM.get(isHighPressure));

        // Title label
        modularUI.widget(new LabelWidget(5, titleY, getBlockState().getBlock().getDescriptionId()));

        // --- Input slots ---
        for (int i = 0; i < maxInputs; i++) {
            modularUI.widget(new SlotWidget(importItems, i, inputX, inputY + i * slotSize)
                    .setBackgroundTexture(GuiTextures.SLOT_STEAM.get(isHighPressure)));
        }

        // --- Progress bar ---
        var progressTexture = getRecipeType().getRecipeUI().getProgressBarTexture();
        modularUI.widget(new ProgressWidget(recipeLogic::getProgressPercent,
                progressX, progressY, 20, 20, progressTexture));

        // --- No-steam indicator ---
        modularUI.widget(new PredicatedImageWidget(progressX + 1, progressY + 1, 18, 18,
                GuiTextures.INDICATOR_NO_STEAM.get(isHighPressure))
                .setPredicate(recipeLogic::isWaiting));

        // --- Scrollable output slots ---
        var scrollable = new DraggableScrollableWidgetGroup(
                outputAreaX, outputAreaY, outputWidgetW, outputContentH);
        scrollable.setYScrollBarWidth(scrollBarWidth);
        scrollable.setYBarStyle(GuiTextures.SLIDER_BACKGROUND_VERTICAL, GuiTextures.BUTTON);

        for (int i = 0; i < totalOutputSlots; i++) {
            int col = i % cols;
            int row = i / cols;
            scrollable.addWidget(new SlotWidget(exportItems, i,
                    col * slotSize, row * slotSize)
                    .setBackgroundTexture(GuiTextures.SLOT_STEAM.get(isHighPressure)));
        }
        modularUI.widget(scrollable);

        // --- Player inventory ---
        modularUI.widget(UITemplate.bindPlayerInventory(entityPlayer.getInventory(),
                GuiTextures.SLOT_STEAM.get(isHighPressure), 7, playerInvY, true));

        return modularUI;
    }
}
