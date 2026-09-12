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
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
// import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
// import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeTab;

public class DimensionalFeedingUpgradeTab extends FeedingUpgradeTab {

    @SuppressWarnings("null")
    public DimensionalFeedingUpgradeTab(FeedingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen,
                                       int slotsPerRow) {
        super(upgradeContainer, position, screen,
                Component.translatable("gui.mobackup.upgrade.dimensional_feeding"),
                Component.translatable("gui.mobackup.upgrade.dimensional_feeding.tooltip"));

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

        this.filterLogicControl = addHideableChild(new FilterLogicControl.Advanced(
                screen,
                new Position(x + 3, y + 44),
                getContainer().getFilterLogicContainer(),
                Config.SERVER.advancedFeedingUpgrade.slotsInRow.get()
        ));
    }
}