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
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeTab;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer;

public class DimensionalRestockUpgradeTab extends RestockUpgradeTab {

    @SuppressWarnings("null")
    public DimensionalRestockUpgradeTab(ContentsFilteredUpgradeContainer<RestockUpgradeWrapper> upgradeContainer, Position position, StorageScreenBase<?> screen,
                                       ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton) {
        super(upgradeContainer, position, screen,
                Component.translatable("gui.mobackup.upgrade.dimensional_restock"),
                Component.translatable("gui.mobackup.upgrade.dimensional_restock.tooltip"));

        this.filterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(
                screen,
                new Position(x + 3, y + 24),
                getContainer().getFilterLogicContainer(),
                Config.SERVER.advancedRestockUpgrade.slotsInRow.get(),
                SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE
        ));
    }
}