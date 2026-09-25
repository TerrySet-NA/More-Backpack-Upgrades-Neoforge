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
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeTab;

public class DimensionalAlchemyUpgradeTab extends AlchemyUpgradeTab {
    private static final int TOP_POS = 44;

    public DimensionalAlchemyUpgradeTab(AlchemyUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
        super(upgradeContainer, position, screen,
                Component.translatable("gui.mobackup.upgrade." + (RSBridge.isDimensional(upgradeContainer.getUpgradeStack()) ? "dimensional_alchemy" : "network_alchemy")),
                Component.translatable("gui.mobackup.upgrade." + (RSBridge.isDimensional(upgradeContainer.getUpgradeStack()) ? "dimensional_alchemy.tooltip" : "network_alchemy.tooltip")),
                true);

        // 按鈕 1: 缺少任意效果 / 缺少全部效果 (x + 3, y + 44)
        addHideableChild(new ToggleButton<>(new Position(x + 3, y + TOP_POS), ANY_EFFECT_MISSING,
                button -> getContainer().toggleMatchAll(), () -> getContainer().shouldMatchAll()));

        // 按鈕 2: 匹配持續時間 (x + 21, y + 44)
        addHideableChild(new ToggleButton<>(new Position(x + 3 + 18, y + TOP_POS), MATCH_DURATION,
                button -> getContainer().toggleMatchDuration(), () -> getContainer().shouldMatchDuration()));

        // 按鈕 3: 匹配效果等級 (x + 39, y + 44)
        addHideableChild(new ToggleButton<>(new Position(x + 3 + 18 * 2, y + TOP_POS), MATCH_AMPLIFIER,
                button -> getContainer().toggleMatchAmplifier(), () -> getContainer().shouldMatchAmplifier()));

        // 按鈕 4: 優先級切換按鈕 (x + 3, y + 24)
        addHideableChild(ModGuiControls.createPriorityButton(new Position(x + 3, y + 24), getContainer()));

        // 按鈕 5: 目標實體類型 (x + 21, y + 24)
        if (getContainer().hasEntityMatchOption()) {
            addHideableChild(new ToggleButton<>(new Position(x + 3 + 18, y + TOP_POS), ENTITY_MATCH,
                    button -> getContainer().toggleEntityMatch(), () -> getContainer().getEntityMatch()));
        }
    }
}