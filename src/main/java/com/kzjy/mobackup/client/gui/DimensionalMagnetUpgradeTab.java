/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.client.gui;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeTab;

// 直接繼承 MagnetUpgradeTab，不再繼承 .Advanced
public class DimensionalMagnetUpgradeTab extends MagnetUpgradeTab {

    @SuppressWarnings("null")
    public DimensionalMagnetUpgradeTab(MagnetUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen,
                                       int slotsPerRow, ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton) {
        // 在 super 中自訂分頁標題與懸浮提示文字
        super(upgradeContainer, position, screen,
                Component.translatable("gui.mobackup.upgrade.dimensional_magnet"),
                Component.translatable("gui.mobackup.upgrade.dimensional_magnet.tooltip"));

        // 1. 加入進階版過濾器控制項（原版 Advanced 裡做的事情）
        this.filterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(
                screen,
                new Position(x + 3, y + 44),
                getContainer().getFilterLogicContainer(),
                slotsPerRow,
                contentsFilterButton
        ));

        // 2. 加入我們的共用優先級切換按鈕
        addHideableChild(ModGuiControls.createPriorityButton(new Position(x + 39, y + 24), getContainer()));
    }
}