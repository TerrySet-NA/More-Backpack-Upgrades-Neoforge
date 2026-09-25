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
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeTab;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer;

public class DimensionalRestockUpgradeTab extends RestockUpgradeTab {

    @SuppressWarnings("null")
    public DimensionalRestockUpgradeTab(ContentsFilteredUpgradeContainer<RestockUpgradeWrapper> upgradeContainer, Position position, StorageScreenBase<?> screen) {
        super(upgradeContainer, position, screen,
                Component.translatable("gui.mobackup.upgrade." + (RSBridge.isDimensional(upgradeContainer.getUpgradeStack()) ? "dimensional_restock" : "network_restock")),
                Component.translatable("gui.mobackup.upgrade." + (RSBridge.isDimensional(upgradeContainer.getUpgradeStack()) ? "dimensional_restock.tooltip" : "network_restock.tooltip")));

        // 2. 一鍵將背包中目標物品存入 RS 網路按鈕 (x + 3, y + 24)
        addHideableChild(ModGuiControls.createQuickRestockButton(new Position(x + 3, y + 24), getContainer()));
        
        // 1. 外部卸貨來源切換按鈕 (RS 網路 ⇄ 隨身背包) (x + 21, y + 24)
        addHideableChild(ModGuiControls.createPriorityButton(new Position(x + 21, y + 24), getContainer()));

        this.filterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(
                screen,
                new Position(x + 3, y + 44),
                getContainer().getFilterLogicContainer(),
                Config.SERVER.advancedRestockUpgrade.slotsInRow.get(),
                SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE
        ));
    }
}