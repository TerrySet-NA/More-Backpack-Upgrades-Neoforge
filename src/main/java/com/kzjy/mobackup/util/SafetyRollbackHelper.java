/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.util;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

public class SafetyRollbackHelper {

    public static void fallbackToBackpackOrPlayer(ItemStack leftover, IStorageWrapper storageWrapper, @Nullable Player player) {
        if (leftover == null || leftover.isEmpty()) return;

        // 1. 先嘗試塞入背包
        ItemStack remaining = storageWrapper.getInventoryForUpgradeProcessing().insertItem(leftover, false);
        if (remaining.isEmpty()) return;

        // 2. 背包塞不下且有玩家實體時，塞入玩家或以實體生成在腳下
        if (player != null) {
            CapabilityHelper.runOnCapability(player, Capabilities.ItemHandler.ENTITY, null,
                    playerInv -> InventoryHelper.insertOrDropItem(player, remaining, playerInv));
        }
    }
}