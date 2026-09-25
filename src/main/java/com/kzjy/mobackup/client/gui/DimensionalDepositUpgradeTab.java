/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.client.gui;

import com.kzjy.mobackup.core.RSBridge;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterLogicControl;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public class DimensionalDepositUpgradeTab extends DepositUpgradeTab {

    public DimensionalDepositUpgradeTab(DepositUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
        super(upgradeContainer, position, screen,
                Component.translatable("gui.mobackup.upgrade." + (RSBridge.isDimensional(upgradeContainer.getUpgradeStack()) ? "dimensional_deposit" : "network_deposit")),
                Component.translatable("gui.mobackup.upgrade." + (RSBridge.isDimensional(upgradeContainer.getUpgradeStack()) ? "dimensional_deposit.tooltip" : "network_deposit.tooltip")));

        // 2. 一鍵將背包中目標物品存入 RS 網路按鈕 (x + 3, y + 24)
        addHideableChild(ModGuiControls.createQuickDepositButton(new Position(x + 3, y + 24), getContainer()));

        // 1. 外部卸貨來源切換按鈕 (RS 網路 ⇄ 隨身背包) (x + 21, y + 24)
        addHideableChild(ModGuiControls.createPriorityButton(new Position(x + 21, y + 24), getContainer()));

        // 原版過濾模式控制項
        this.filterLogicControl = addHideableChild(new DepositFilterLogicControl.Advanced(
                screen,
                new Position(x + 3, y + 44),
                getContainer().getFilterLogicContainer(),
                Config.SERVER.advancedDepositUpgrade.slotsInRow.get()
        ));
    }
}