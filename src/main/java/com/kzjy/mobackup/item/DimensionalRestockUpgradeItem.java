/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.item;

// import com.kzjy.mobackup.MoBackup;
import com.kzjy.mobackup.wrapper.DimensionalRestockUpgradeWrapper;
import net.minecraft.network.chat.Component;
// import net.minecraft.world.InteractionResult;
// import net.minecraft.world.item.Item;
// import net.minecraft.world.item.ItemStack;
// import net.minecraft.world.item.TooltipFlag;
// import net.minecraft.world.item.context.UseOnContext;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
// import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeItem;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeItem;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
// import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
// import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;
// import java.util.Set;

@SuppressWarnings("null")
public class DimensionalRestockUpgradeItem extends RestockUpgradeItem implements IMoBackupUpgrade {
    public static final UpgradeType<RestockUpgradeWrapper> TYPE = new UpgradeType<>(
            DimensionalRestockUpgradeWrapper::new);

    public DimensionalRestockUpgradeItem() {
        super(Config.SERVER.advancedRestockUpgrade.filterSlots::get);
    }

    @Override
    public UpgradeType<RestockUpgradeWrapper> getType() {
        return TYPE;
    }

    @Override
    public List<UpgradeConflictDefinition> getUpgradeConflicts() {
        return List.of(new UpgradeConflictDefinition(
                item -> item instanceof RestockUpgradeItem,
                0,
                Component.translatable("gui.sophisticatedbackpacks.status.restock_only_one_allowed")
        ));
    }

    @Override
    public int getUpgradesPerStorage(String storageType) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getUpgradesInGroupPerStorage(String storageType) {
        return Integer.MAX_VALUE;
    }
}