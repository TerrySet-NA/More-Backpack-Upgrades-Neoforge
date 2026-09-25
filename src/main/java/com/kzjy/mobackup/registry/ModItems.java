/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.registry;

import java.util.function.Supplier;

import com.kzjy.mobackup.MoBackup;
import com.kzjy.mobackup.item.DimensionalAlchemyUpgradeItem;
import com.kzjy.mobackup.item.DimensionalDepositUpgradeItem;
import com.kzjy.mobackup.item.DimensionalFeedingUpgradeItem;
import com.kzjy.mobackup.item.DimensionalMagnetUpgradeItem;
import com.kzjy.mobackup.item.DimensionalPickupUpgradeItem;
import com.kzjy.mobackup.item.DimensionalPumpUpgradeItem;
import com.kzjy.mobackup.item.DimensionalRefillUpgradeItem;
import com.kzjy.mobackup.item.DimensionalRestockUpgradeItem;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;

@SuppressWarnings("null")
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoBackup.MOD_ID);

    // =========================================================================
    // 8 種次元版升級 (跨維度連線：IS_DIMENSIONAL = true)
    // =========================================================================
    public static final DeferredItem<DimensionalMagnetUpgradeItem> DIMENSIONAL_MAGNET_UPGRADE = ITEMS.register(
            "dimensional_magnet_upgrade",
            () -> new DimensionalMagnetUpgradeItem(
                    Config.SERVER.advancedMagnetUpgrade.magnetRange::get,
                    Config.SERVER.advancedMagnetUpgrade.filterSlots::get,
                    true));

    public static final DeferredItem<DimensionalPickupUpgradeItem> DIMENSIONAL_PICKUP_UPGRADE = ITEMS.register(
            "dimensional_pickup_upgrade",
            () -> new DimensionalPickupUpgradeItem(
                    Config.SERVER.advancedPickupUpgrade.filterSlots::get,
                    true));

    public static final DeferredItem<DimensionalDepositUpgradeItem> DIMENSIONAL_DEPOSIT_UPGRADE = ITEMS.register(
            "dimensional_deposit_upgrade",
            () -> new DimensionalDepositUpgradeItem(true));

    public static final DeferredItem<DimensionalFeedingUpgradeItem> DIMENSIONAL_FEEDING_UPGRADE = ITEMS.register(
            "dimensional_feeding_upgrade",
            () -> new DimensionalFeedingUpgradeItem(true));

    public static final DeferredItem<DimensionalRefillUpgradeItem> DIMENSIONAL_REFILL_UPGRADE = ITEMS.register(
            "dimensional_refill_upgrade",
            () -> new DimensionalRefillUpgradeItem(true));

    public static final DeferredItem<DimensionalRestockUpgradeItem> DIMENSIONAL_RESTOCK_UPGRADE = ITEMS.register(
            "dimensional_restock_upgrade",
            () -> new DimensionalRestockUpgradeItem(true));

    public static final DeferredItem<DimensionalPumpUpgradeItem> DIMENSIONAL_PUMP_UPGRADE = ITEMS.register(
            "dimensional_pump_upgrade",
            () -> new DimensionalPumpUpgradeItem(true));

    public static final DeferredItem<DimensionalAlchemyUpgradeItem> DIMENSIONAL_ALCHEMY_UPGRADE = ITEMS.register(
            "dimensional_alchemy_upgrade",
            () -> new DimensionalAlchemyUpgradeItem(
                    Config.SERVER.advancedAlchemyUpgrade.filterSlots::get,
                    true));

    // =========================================================================
    // 8 種網路版升級 (同維度連線：IS_DIMENSIONAL = false，共用 Item 類別)
    // =========================================================================
    public static final DeferredItem<DimensionalMagnetUpgradeItem> NETWORK_MAGNET_UPGRADE = ITEMS.register(
            "network_magnet_upgrade",
            () -> new DimensionalMagnetUpgradeItem(
                    Config.SERVER.advancedMagnetUpgrade.magnetRange::get,
                    Config.SERVER.advancedMagnetUpgrade.filterSlots::get,
                    false));

    public static final DeferredItem<DimensionalPickupUpgradeItem> NETWORK_PICKUP_UPGRADE = ITEMS.register(
            "network_pickup_upgrade",
            () -> new DimensionalPickupUpgradeItem(
                    Config.SERVER.advancedPickupUpgrade.filterSlots::get,
                    false));

    public static final DeferredItem<DimensionalDepositUpgradeItem> NETWORK_DEPOSIT_UPGRADE = ITEMS.register(
            "network_deposit_upgrade",
            () -> new DimensionalDepositUpgradeItem(false));

    public static final DeferredItem<DimensionalFeedingUpgradeItem> NETWORK_FEEDING_UPGRADE = ITEMS.register(
            "network_feeding_upgrade",
            () -> new DimensionalFeedingUpgradeItem(false));

    public static final DeferredItem<DimensionalRefillUpgradeItem> NETWORK_REFILL_UPGRADE = ITEMS.register(
            "network_refill_upgrade",
            () -> new DimensionalRefillUpgradeItem(false));

    public static final DeferredItem<DimensionalRestockUpgradeItem> NETWORK_RESTOCK_UPGRADE = ITEMS.register(
            "network_restock_upgrade",
            () -> new DimensionalRestockUpgradeItem(false));

    public static final DeferredItem<DimensionalPumpUpgradeItem> NETWORK_PUMP_UPGRADE = ITEMS.register(
            "network_pump_upgrade",
            () -> new DimensionalPumpUpgradeItem(false));

    public static final DeferredItem<DimensionalAlchemyUpgradeItem> NETWORK_ALCHEMY_UPGRADE = ITEMS.register(
            "network_alchemy_upgrade",
            () -> new DimensionalAlchemyUpgradeItem(
                    Config.SERVER.advancedAlchemyUpgrade.filterSlots::get,
                    false));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}