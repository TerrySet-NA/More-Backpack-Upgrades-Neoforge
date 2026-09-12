/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.client.gui;

import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
// import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
// import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;

public class DimensionalRefillUpgradeTab extends RefillUpgradeTab {

    public DimensionalRefillUpgradeTab(RefillUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen,
                                       int slotsPerRow) {
        super(upgradeContainer, position, screen, Config.SERVER.advancedRefillUpgrade.slotsInRow.get(), "dimensional_refill");
    }
}