/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.client.gui;

import com.kzjy.mobackup.mixin.RefillUpgradeTabAccessor;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public class DimensionalRefillUpgradeTab extends RefillUpgradeTab {

    public DimensionalRefillUpgradeTab(RefillUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen,
                                       int slotsPerRow) {
        super(upgradeContainer, position, screen, Config.SERVER.advancedRefillUpgrade.slotsInRow.get(), "dimensional_refill");

        // 1. 將過濾槽位控制項下移 20px (移至 y + 44)
        ((RefillUpgradeTabAccessor) this).mobackup$getFilterLogicControl()
                .setPosition(new Position(x + 3, y + 44));

        // 2. 呼叫基類刷新尺寸
        refreshOpenTabDimension();

        // 3. 槽位實體座標同步刷新
        moveSlotsToTab();

        // 4. 頂部工具列放入優先級切換按鈕 (x + 3, y + 24)
        addHideableChild(ModGuiControls.createPriorityButton(new Position(x + 3, y + 24), getContainer()));
    }
}