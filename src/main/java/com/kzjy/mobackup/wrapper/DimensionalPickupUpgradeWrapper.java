/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.wrapper;

import com.kzjy.mobackup.core.PickupContext;
import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.item.DimensionalMagnetUpgradeItem;
import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * 次元拾取升級的邏輯實現 (MC 1.21.1 / RS 2.x)
 * 支援透過 IPriorityRoutingUpgrade 切換【網路優先】與【背包優先】雙向路由
 */
@SuppressWarnings("null")
public class DimensionalPickupUpgradeWrapper extends PickupUpgradeWrapper implements IPriorityRoutingUpgrade {

    public DimensionalPickupUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
                                           Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    private Network cachedNetwork;
    private long lastNetworkCheckTime = -1;
    private static final int NETWORK_CHECK_INTERVAL = 20;

    private Network getCachedNetwork(Level level) {
        long gameTime = level.getGameTime();
        if (cachedNetwork == null || lastNetworkCheckTime < 0
                || gameTime - lastNetworkCheckTime >= NETWORK_CHECK_INTERVAL) {
            lastNetworkCheckTime = gameTime;
            cachedNetwork = RSBridge.getNetwork(level, getUpgradeStack());
        }
        return cachedNetwork;
    }

    private Boolean networkFirstCache = null;

    @Override
    public boolean isNetworkFirst() {
        if (networkFirstCache == null) {
            CustomData customData = upgrade.get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.contains(TAG_NETWORK_FIRST)) {
                networkFirstCache = customData.copyTag().getBoolean(TAG_NETWORK_FIRST);
            } else {
                networkFirstCache = true; // 預設：RS 網路優先
            }
        }
        return networkFirstCache;
    }

    @Override
    public void setNetworkFirst(boolean networkFirst) {
        this.networkFirstCache = networkFirst;
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, tag -> {
            tag.putBoolean(TAG_NETWORK_FIRST, networkFirst);
        });
        save();
    }

    @Override
    public ItemStack pickup(Level world, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !getFilterLogic().matchesFilter(stack)) {
            return stack;
        }

        // 背包已裝磁吸升級時，避免拾取升級重複路由到 RS
        if (storageWrapper.getUpgradeHandler().hasUpgrade(DimensionalMagnetUpgradeItem.TYPE)) {
            return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
        }

        Player playerCtx = PickupContext.current();

        if (isNetworkFirst()) {
            // === 模式 A：網路優先 (RS -> 背包) ===
            Network network = getCachedNetwork(world);
            if (network != null) {
                stack = insertIntoRsNetwork(network, stack, simulate, playerCtx);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY; // 全額寫入 RS
                }
            }
            // RS 裝不下或未連線，剩餘物資降級存入背包
            return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
        } else {
            // === 模式 B：背包優先 (背包 -> RS 溢出) ===
            stack = storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY; // 全額存入背包
            }

            // 背包滿了，溢出物資自動排入 RS 網路
            Network network = getCachedNetwork(world);
            if (network != null) {
                stack = insertIntoRsNetwork(network, stack, simulate, playerCtx);
            }
            return stack;
        }
    }

    /**
     * 封裝 RS 2.x 插入邏輯（支援模擬與真實寫入，並綁定玩家 Actor）
     */
    private ItemStack insertIntoRsNetwork(Network network, ItemStack stack, boolean simulate, @Nullable Player player) {
        if (network == null || stack.isEmpty()) {
            return stack;
        }

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) {
            return stack;
        }

        ItemResource resource = ItemResource.ofItemStack(stack);
        Action action = simulate ? Action.SIMULATE : Action.EXECUTE;
        Actor actor = player != null ? new PlayerActor(player) : Actor.EMPTY;

        long inserted = storage.insert(resource, stack.getCount(), action, actor);
        if (inserted <= 0) {
            return stack;
        }

        int remainingCount = stack.getCount() - (int) inserted;
        if (remainingCount <= 0) {
            return ItemStack.EMPTY;
        }

        return stack.copyWithCount(remainingCount);
    }
}