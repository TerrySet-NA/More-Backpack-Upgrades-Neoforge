/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.registry;

import com.kzjy.mobackup.Config;
import com.kzjy.mobackup.MoBackup;
// import com.kzjy.mobackup.item.DimensionalDepositUpgradeItem;
import com.kzjy.mobackup.item.DimensionalMagnetUpgradeItem;
import com.kzjy.mobackup.item.DimensionalPickupUpgradeItem;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoBackup.MOD_ID);

    public static final DeferredItem<DimensionalMagnetUpgradeItem> DIMENSIONAL_MAGNET_UPGRADE = ITEMS.register(
            "dimensional_magnet_upgrade",
            () -> new DimensionalMagnetUpgradeItem(
                    Config.COMMON.dimensionalMagnetRange::get,
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedMagnetUpgrade.filterSlots::get));

    public static final DeferredItem<DimensionalPickupUpgradeItem> DIMENSIONAL_PICKUP_UPGRADE = ITEMS.register(
            "dimensional_pickup_upgrade",
            () -> new DimensionalPickupUpgradeItem(
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedPickupUpgrade.filterSlots::get));

//     public static final DeferredItem<DimensionalDepositUpgradeItem> DIMENSIONAL_DEPOSIT_UPGRADE = ITEMS.register(
//             "dimensional_deposit_upgrade",
//             () -> new DimensionalDepositUpgradeItem(
//                     net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedDepositUpgrade.filterSlots::get));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}