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
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeTab;

public class DimensionalMagnetUpgradeTab extends MagnetUpgradeTab {

    public DimensionalMagnetUpgradeTab(MagnetUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
        super(upgradeContainer, position, screen,
                Component.translatable("gui.mobackup.upgrade." + (RSBridge.isDimensional(upgradeContainer.getUpgradeStack()) ? "dimensional_magnet" : "network_magnet")),
                Component.translatable("gui.mobackup.upgrade." + (RSBridge.isDimensional(upgradeContainer.getUpgradeStack()) ? "dimensional_magnet.tooltip" : "network_magnet.tooltip")));

        // 1. 進階版過濾器控制項 (x + 3, y + 44)
        this.filterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(
                screen,
                new Position(x + 3, y + 44),
                getContainer().getFilterLogicContainer(),
                Config.SERVER.advancedMagnetUpgrade.slotsInRow.get(),
                SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE
        ));

        // 2. 優先級切換按鈕 (x + 39, y + 24)
        addHideableChild(ModGuiControls.createPriorityButton(new Position(x + 39, y + 24), getContainer()));
    }
}