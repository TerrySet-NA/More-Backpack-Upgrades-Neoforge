/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.util;

import javax.annotation.Nullable;
import com.kzjy.mobackup.core.RSBridge;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

public class RSRoutingHelper {

    /**
     * 向 RS 網路注入物品（含 Simulate / Execute 判定與 Actor 處理）
     */
    public static ItemStack insertIntoRs(Network network, ItemStack stack, boolean simulate, @Nullable Player player) {
        if (network == null || stack.isEmpty() || !RSBridge.canInsert(network, player)) {
            return stack;
        }

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) {
            return stack;
        }

        ItemResource resource = ItemResource.ofItemStack(stack);
        Action action = simulate ? Action.SIMULATE : Action.EXECUTE;
        Actor actor = RSBridge.getActor(player);

        long inserted = storage.insert(resource, stack.getCount(), action, actor);
        if (inserted <= 0) {
            return stack;
        }

        int remainingCount = stack.getCount() - (int) inserted;
        return remainingCount <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remainingCount);
    }

    /**
     * 雙向級聯物品注入：根據 networkFirst 決定 RS 與 背包 的先後順序
     */
    public static ItemStack routeItemInsertion(Level level, ItemStack upgradeStack, IItemHandler backpackInv,
                                               ItemStack stack, boolean networkFirst, boolean simulate, @Nullable Player player) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        if (networkFirst) {
            Network network = RSBridge.getNetwork(level, upgradeStack, player, BuiltinPermission.INSERT);
            if (network != null) {
                stack = insertIntoRs(network, stack, simulate, player);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
            return backpackInv.insertItem(0, stack, simulate); // 依 handler 實作 insertItem
        } else {
            stack = backpackInv.insertItem(0, stack, simulate);
            if (stack.isEmpty()) return ItemStack.EMPTY;

            Network network = RSBridge.getNetwork(level, upgradeStack, player, BuiltinPermission.INSERT);
            if (network != null) {
                stack = insertIntoRs(network, stack, simulate, player);
            }
            return stack;
        }
    }
}