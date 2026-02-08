package com.gregtechceu.gtceu.common.machine.steam;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.PredicatedImageWidget;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.steam.SimpleSteamMachine;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A specialized steam sieve machine with a scrollable output panel.
 * <p>
 * Because the ore sieve can have up to 36 chanced outputs, the standard auto-layout
 * would overflow the GUI. This machine shows input slots and a progress bar in the
 * main view, with all output slots inside a scrollable panel that can be toggled
 * via an expand/collapse button.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleSteamSieveMachine extends SimpleSteamMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SimpleSteamSieveMachine.class, SimpleSteamMachine.MANAGED_FIELD_HOLDER);

    /** Number of columns in the expanded scrollable panel */
    private static final int EXPANDED_COLS = 9;
    /** Maximum visible rows before scrollbar kicks in */
    private static final int MAX_VISIBLE_ROWS = 4;

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

        // --- Layout constants ---
        int guiWidth = 176;
        int guiHeight = 166;
        int playerInvY = 84;
        int inputX = 7;
        int inputY = 32;
        int progressX = 33;  // after input column + gap
        int progressY = 41;  // vertically centered in recipe area
        // "Expand outputs" button to the right of progress bar
        int expandBtnX = 61;
        int expandBtnY = 40;

        var modularUI = new ModularUI(guiWidth, guiHeight, this, entityPlayer)
                .background(GuiTextures.BACKGROUND_STEAM.get(isHighPressure));

        // Title label
        modularUI.widget(new LabelWidget(5, 5, getBlockState().getBlock().getDescriptionId()));

        // --- Input slots ---
        for (int i = 0; i < maxInputs; i++) {
            modularUI.widget(new SlotWidget(importItems, i, inputX, inputY + i * 18)
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

        // --- Output panel (scrollable, initially hidden) ---
        var outputPanel = createOutputPanel(totalOutputSlots);
        outputPanel.setVisible(false);
        outputPanel.setActive(false);

        // --- Expand/collapse toggle button ---
        modularUI.widget(new ButtonWidget(expandBtnX, expandBtnY, 54, 18,
                new GuiTextureGroup(GuiTextures.VANILLA_BUTTON),
                cd -> {
                    boolean show = !outputPanel.isVisible();
                    outputPanel.setVisible(show);
                    outputPanel.setActive(show);
                }).setHoverTooltips("gtceu.gui.sieve.expand_outputs"));

        modularUI.widget(outputPanel);

        // --- Player inventory ---
        modularUI.widget(UITemplate.bindPlayerInventory(entityPlayer.getInventory(),
                GuiTextures.SLOT_STEAM.get(isHighPressure), 7, playerInvY, true));

        return modularUI;
    }

    /**
     * Creates the scrollable output panel containing all output slots.
     * Appears as a chest-like overlay centered in the GUI.
     */
    private WidgetGroup createOutputPanel(int totalSlots) {
        int padding = 6;
        int slotSize = 18;
        int scrollBarWidth = 8;
        int cols = EXPANDED_COLS;
        int totalRows = (totalSlots + cols - 1) / cols;
        int visibleRows = Math.min(totalRows, MAX_VISIBLE_ROWS);

        int contentWidth = cols * slotSize;
        int contentHeight = visibleRows * slotSize;

        // Title bar height + bottom padding
        int titleHeight = 16;
        int panelInnerWidth = contentWidth + scrollBarWidth + 4;
        int panelWidth = panelInnerWidth + padding * 2;
        int panelHeight = titleHeight + contentHeight + padding * 2;

        // Center the panel in the 176x166 GUI
        int panelX = (176 - panelWidth) / 2;
        int panelY = (166 - panelHeight) / 2 - 8;

        var panel = new WidgetGroup(panelX, panelY, panelWidth, panelHeight);
        panel.setBackground(GuiTextures.BACKGROUND);

        // Title
        panel.addWidget(new LabelWidget(padding + 2, padding,
                "gtceu.gui.sieve.all_outputs"));

        // Close button
        panel.addWidget(new ButtonWidget(
                panelWidth - padding - 12, padding, 12, 12,
                GuiTextures.CLOSE_ICON,
                cd -> {
                    panel.setVisible(false);
                    panel.setActive(false);
                }));

        // Scrollable slot area
        var scrollable = new DraggableScrollableWidgetGroup(
                padding, padding + titleHeight,
                panelInnerWidth, contentHeight);
        scrollable.setYScrollBarWidth(scrollBarWidth);
        scrollable.setYBarStyle(GuiTextures.SLIDER_BACKGROUND_VERTICAL, GuiTextures.BUTTON);

        for (int i = 0; i < totalSlots; i++) {
            int col = i % cols;
            int row = i / cols;
            scrollable.addWidget(new SlotWidget(exportItems, i,
                    col * slotSize, row * slotSize)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }

        panel.addWidget(scrollable);
        return panel;
    }
}
