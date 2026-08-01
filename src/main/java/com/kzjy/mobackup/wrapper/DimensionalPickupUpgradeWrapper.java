/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.wrapper;

import com.kzjy.mobackup.MoBackup;
import com.kzjy.mobackup.core.PickupContext;
import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.item.DimensionalMagnetUpgradeItem;
// import com.mojang.logging.LogUtils;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
// import org.slf4j.Logger;

import java.util.function.Consumer;

/**
 * 次元拾取升級的邏輯實現 (MC 1.21.1 / RS 2.x)
 * 拾取到的物品優先推送到 RS 網路，必要時回退到背包
 */
@SuppressWarnings("null")
public class DimensionalPickupUpgradeWrapper extends PickupUpgradeWrapper {

    public DimensionalPickupUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
                                           Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
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

        // 寫入 RS 2.x 網路
        Network network = getCachedNetwork(world);
        if (network != null) {
            StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
            if (storage != null) {
                Action action = simulate ? Action.SIMULATE : Action.EXECUTE;

                // 1. 獲取上下文玩家並封裝為 Actor (空則回退至 Actor.EMPTY 防範介面變更)
                Player playerCtx = PickupContext.current();
                Actor actor = playerCtx != null ? new PlayerActor(playerCtx) : Actor.EMPTY;

                // 2. 包裝為 RS 2.x 統一 ItemResource 並寫入
                ItemResource resource = ItemResource.ofItemStack(stack);
                long inserted = storage.insert(resource, stack.getCount(), action, actor);

                if (inserted > 0) {
                    MoBackup.LOGGER.info("[MoBackup-Debug] 次元拾取 -> 成功推送到 RS 網路 ({} x{})", stack.getHoverName().getString(), inserted);
                    int remainingCount = stack.getCount() - (int) inserted;
                    if (remainingCount <= 0) {
                        return ItemStack.EMPTY; // 100% 寫入 RS 網路
                    }
                    // 部分寫入，剩餘數量繼續降級寫入背包
                    stack = stack.copyWithCount(remainingCount);
                }
            }
        } else {
            MoBackup.LOGGER.warn("[MoBackup-Debug] 次元拾取 -> RS 網路未連線，降級寫入精妙背包");
        }

        // 剩餘物品回退到精妙背包儲存
        return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
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
            MoBackup.LOGGER.info("[MoBackup-Debug] 拾取卡取得 RS 網路實例 -> {}", (cachedNetwork != null ? "【成功】" : "【失敗: null】"));
        }
        return cachedNetwork;
    }
}