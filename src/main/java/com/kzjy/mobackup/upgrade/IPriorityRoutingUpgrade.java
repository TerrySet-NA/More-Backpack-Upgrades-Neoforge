/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.upgrade;

import com.kzjy.mobackup.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;

public interface IPriorityRoutingUpgrade {

    ItemStack getUpgradeStack();

    void save();

    default boolean getDefaultNetworkFirst() {
        return false;
    }

    default boolean isNetworkFirst() {
        return getUpgradeStack().getOrDefault(ModDataComponents.NETWORK_FIRST.get(), getDefaultNetworkFirst());
    }

    default void setNetworkFirst(boolean networkFirst) {
        getUpgradeStack().set(ModDataComponents.NETWORK_FIRST.get(), networkFirst);
        save();
    }

    // 靜態輔助方法：供 GUI / 按鈕直接讀寫 ItemStack，全面使用 Data Component
    static boolean isNetworkFirst(ItemStack upgradeStack) {
        return upgradeStack.getOrDefault(ModDataComponents.NETWORK_FIRST.get(), true);
    }

    static void setNetworkFirst(ItemStack upgradeStack, boolean networkFirst) {
        upgradeStack.set(ModDataComponents.NETWORK_FIRST.get(), networkFirst);
    }
}