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
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeTab;

public class DimensionalFeedingUpgradeTab extends FeedingUpgradeTab {

    public DimensionalFeedingUpgradeTab(FeedingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
        super(upgradeContainer, position, screen,
                Component.translatable("gui.mobackup.upgrade." + (RSBridge.isDimensional(upgradeContainer.getUpgradeStack()) ? "dimensional_feeding" : "network_feeding")),
                Component.translatable("gui.mobackup.upgrade." + (RSBridge.isDimensional(upgradeContainer.getUpgradeStack()) ? "dimensional_feeding.tooltip" : "network_feeding.tooltip")));

        addHideableChild(new ToggleButton<>(
                new Position(x + 3, y + 24),
                HUNGER_LEVEL,
                button -> getContainer().setFeedAtHungerLevel(getContainer().getFeedAtHungerLevel().next()),
                () -> getContainer().getFeedAtHungerLevel()
        ));

        addHideableChild(new ToggleButton<>(
                new Position(x + 21, y + 24),
                FEED_IMMEDIATELY_WHEN_HURT,
                button -> getContainer().setFeedImmediatelyWhenHurt(!getContainer().shouldFeedImmediatelyWhenHurt()),
                () -> getContainer().shouldFeedImmediatelyWhenHurt()
        ));

        addHideableChild(ModGuiControls.createPriorityButton(new Position(x + 39, y + 24), getContainer()));

        this.filterLogicControl = addHideableChild(new FilterLogicControl.Advanced(
                screen,
                new Position(x + 3, y + 44),
                getContainer().getFilterLogicContainer(),
                Config.SERVER.advancedFeedingUpgrade.slotsInRow.get()
        ));
    }
}