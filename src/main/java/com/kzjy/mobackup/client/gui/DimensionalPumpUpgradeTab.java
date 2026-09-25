/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.client.gui;

import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.mixin.FluidFilterControlInvoker;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.FluidFilterControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeTab;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions.createToggleButtonDefinition;
import static net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions.getBooleanStateData;

public class DimensionalPumpUpgradeTab extends PumpUpgradeTab {

    private static final ButtonDefinition.Toggle<Boolean> INTERACT_WITH_FLUID_HANDLERS = createToggleButtonDefinition(getBooleanStateData(
            GuiHelper.getButtonStateData(new UV(0, 112), TranslationHelper.INSTANCE.translUpgradeButton("interact_with_tanks_and_pipes"), Dimension.SQUARE_16,
                    new Position(1, 1)),
            GuiHelper.getButtonStateData(new UV(16, 112), TranslationHelper.INSTANCE.translUpgradeButton("do_not_interact_with_tanks_and_pipes"),
                    Dimension.SQUARE_16, new Position(1, 1))));

    private static final ButtonDefinition.Toggle<Boolean> INTERACT_WITH_WORLD = createToggleButtonDefinition(getBooleanStateData(
            GuiHelper.getButtonStateData(new UV(176, 0), TranslationHelper.INSTANCE.translUpgradeButton("interact_with_world"), Dimension.SQUARE_16,
                    new Position(1, 1)),
            GuiHelper.getButtonStateData(new UV(192, 0), TranslationHelper.INSTANCE.translUpgradeButton("do_not_interact_with_world"), Dimension.SQUARE_16,
                    new Position(1, 1))));

    private static final ButtonDefinition.Toggle<Boolean> INTERACT_WITH_HAND = createToggleButtonDefinition(getBooleanStateData(
            GuiHelper.getButtonStateData(new UV(208, 0), TranslationHelper.INSTANCE.translUpgradeButton("interact_with_hand"), Dimension.SQUARE_16,
                    new Position(1, 1)),
            GuiHelper.getButtonStateData(new UV(224, 0), TranslationHelper.INSTANCE.translUpgradeButton("do_not_interact_with_hand"), Dimension.SQUARE_16,
                    new Position(1, 1))));

    private final FluidFilterControl fluidFilterControl;

    public FluidFilterControl getFluidFilterControl() {
        return fluidFilterControl;
    }

    public DimensionalPumpUpgradeTab(PumpUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
        super(upgradeContainer, position, screen,
                Component.translatable("gui.mobackup.upgrade." + (RSBridge.isDimensional(upgradeContainer.getUpgradeStack()) ? "dimensional_pump" : "network_pump")),
                Component.translatable("gui.mobackup.upgrade." + (RSBridge.isDimensional(upgradeContainer.getUpgradeStack()) ? "dimensional_pump.tooltip" : "network_pump.tooltip")));

        // 按鈕 1: IS_INPUT 已由父類 super 註冊在 (x + 3, y + 24)
        // 按鈕 2: 優先級切換按鈕放在 (x + 21, y + 24)
        addHideableChild(ModGuiControls.createPriorityButton(new Position(x + 21, y + 24), getContainer()));

        // 按鈕 3: 容器/管道互動 (x + 3, y + 44)
        addHideableChild(new ToggleButton<>(new Position(x + 3, y + 44), INTERACT_WITH_FLUID_HANDLERS,
                button -> getContainer().setInteractWithFluidHandlers(!getContainer().shouldInteractWithFluidHandlers()),
                () -> getContainer().shouldInteractWithFluidHandlers()));

        // 按鈕 4: 世界方塊互動 (x + 21, y + 44)
        addHideableChild(new ToggleButton<>(new Position(x + 21, y + 44), INTERACT_WITH_WORLD,
                button -> getContainer().setInteractWithWorld(!getContainer().shouldInteractWithWorld()),
                () -> getContainer().shouldInteractWithWorld()));

        // 按鈕 5: 玩家手持互動 (x + 39, y + 44)
        addHideableChild(new ToggleButton<>(new Position(x + 39, y + 44), INTERACT_WITH_HAND,
                button -> getContainer().setInteractWithHand(!getContainer().shouldInteractWithHand()),
                () -> getContainer().shouldInteractWithHand()));

        // 下方流體過濾槽 (y + 64)
        fluidFilterControl = FluidFilterControlInvoker.callInit(new Position(x + 3, y + 64), getContainer().getFluidFilterContainer());
        addHideableChild(fluidFilterControl);
    }
}