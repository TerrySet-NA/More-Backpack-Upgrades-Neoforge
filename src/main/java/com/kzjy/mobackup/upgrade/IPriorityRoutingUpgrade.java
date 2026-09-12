/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.upgrade;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public interface IPriorityRoutingUpgrade {
    String TAG_NETWORK_FIRST = "PriorityNetwork";

    boolean isNetworkFirst();
    void setNetworkFirst(boolean networkFirst);

    // 靜態輔助方法：供按鈕預覽判斷
    @SuppressWarnings("null")
    static boolean isNetworkFirst(ItemStack upgradeStack) {
        CustomData data = upgradeStack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains(TAG_NETWORK_FIRST)) {
            return data.copyTag().getBoolean(TAG_NETWORK_FIRST);
        }
        return true;
    }
}