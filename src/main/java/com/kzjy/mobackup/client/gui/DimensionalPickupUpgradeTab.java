/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.client.gui;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;

public class DimensionalPickupUpgradeTab extends PickupUpgradeTab {

    public DimensionalPickupUpgradeTab(ContentsFilteredUpgradeContainer<PickupUpgradeWrapper> upgradeContainer, Position position, StorageScreenBase<?> screen,
                                       int slotsPerRow, ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton) {
        super(upgradeContainer, position, screen,
                Component.translatable("gui.mobackup.upgrade.dimensional_pickup"),
                Component.translatable("gui.mobackup.upgrade.dimensional_pickup.tooltip"));

        // 優先級切換按鈕 (x + 3, y + 24)
        addHideableChild(ModGuiControls.createPriorityButton(new Position(x + 3, y + 24), getContainer()));

        // 過濾面板 (x + 3, y + 44)
        this.filterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(
                screen,
                new Position(x + 3, y + 44),
                getContainer().getFilterLogicContainer(),
                Config.SERVER.advancedPickupUpgrade.slotsInRow.get(),
                SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE
        ));
    }
}