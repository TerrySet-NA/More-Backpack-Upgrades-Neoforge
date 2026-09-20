package com.kzjy.mobackup.client.gui;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeTab;

public class DimensionalMagnetUpgradeTab extends MagnetUpgradeTab {

    public DimensionalMagnetUpgradeTab(MagnetUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen,
                                       int slotsPerRow, ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton) {
        super(upgradeContainer, position, screen,
                Component.translatable("gui.mobackup.upgrade.dimensional_magnet"),
                Component.translatable("gui.mobackup.upgrade.dimensional_magnet.tooltip"));

        // 1. 進階版過濾器控制項 (x + 3, y + 44)
        this.filterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(
                screen,
                new Position(x + 3, y + 44),
                getContainer().getFilterLogicContainer(),
                slotsPerRow,
                contentsFilterButton
        ));

        // 2. 優先級切換按鈕 (x + 39, y + 24)
        addHideableChild(ModGuiControls.createPriorityButton(new Position(x + 39, y + 24), getContainer()));
    }
}