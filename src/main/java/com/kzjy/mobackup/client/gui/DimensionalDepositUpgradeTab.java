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
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterLogicControl;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public class DimensionalDepositUpgradeTab extends DepositUpgradeTab {

    @SuppressWarnings("null")
    public DimensionalDepositUpgradeTab(DepositUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
        // 呼叫 5 參數的 protected super，傳入自訂標題與說明
        super(upgradeContainer, position, screen,
                Component.translatable("gui.mobackup.upgrade.dimensional_deposit"),
                Component.translatable("gui.mobackup.upgrade.dimensional_deposit.tooltip"));

        this.filterLogicControl = addHideableChild(new DepositFilterLogicControl.Advanced(
                screen,
                new Position(x + 3, y + 24),
                getContainer().getFilterLogicContainer(),
                Config.SERVER.advancedDepositUpgrade.slotsInRow.get()
        ));
    }
}