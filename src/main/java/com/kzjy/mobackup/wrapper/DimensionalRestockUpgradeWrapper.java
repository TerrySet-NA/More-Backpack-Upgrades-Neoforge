/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.wrapper;

// import com.kzjy.mobackup.MoBackup;
// import com.kzjy.mobackup.core.RSBridge;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
// import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("null")
public class DimensionalRestockUpgradeWrapper extends RestockUpgradeWrapper {

    public DimensionalRestockUpgradeWrapper(IStorageWrapper backpackWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(backpackWrapper, upgrade, upgradeSaveHandler);
    }

    /**
     * 從指定 RS 網路中盡量全部取貨（批次循環裝滿背包，絕對防吞物、防爆倉蒸發）
     */
    public List<ItemStack> restockFromRsNetwork(Network network, Player player) {
        List<ItemStack> transferredStacks = new ArrayList<>();
        if (network == null) {
            return transferredStacks;
        }

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) {
            return transferredStacks;
        }

        ITrackedContentsItemHandler backpackInventory = storageWrapper.getInventoryForUpgradeProcessing();
        Actor actor = new PlayerActor(player);

        // 遍歷 RS 網路中所有物資
        for (ResourceAmount resourceAmount : storage.getAll()) {
            if (resourceAmount.amount() <= 0) {
                continue;
            }

            if (resourceAmount.resource() instanceof ItemResource itemResource) {
                ItemStack sample = itemResource.toItemStack(1);

                // 檢查是否符合過濾器配置（過濾清單或背包現存物資）
                if (!getFilterLogic().matchesFilter(sample)) {
                    continue;
                }

                int maxStackSize = sample.getMaxStackSize();

                // 批次循環提取：持續抽取該物品，直到背包空間全滿或 RS 庫存歸零
                while (true) {
                    // 1. 第一層模擬：探測背包當前還能塞下幾顆該物品（以一組為單位探測）
                    ItemStack probeStack = itemResource.toItemStack(maxStackSize);
                    ItemStack remainder = backpackInventory.insertItem(probeStack, true);
                    int acceptable = maxStackSize - remainder.getCount();

                    if (acceptable <= 0) {
                        // 背包已無任何空間容納此物品，退出該物品的抽取
                        break;
                    }

                    // 2. 第二層模擬：向 RS 網路模擬提取 acceptable 數量
                    long simExtracted = storage.extract(itemResource, acceptable, Action.SIMULATE, actor);
                    if (simExtracted <= 0) {
                        // RS 網路該物品已被抽光，退出
                        break;
                    }

                    // 3. 再次核算安全數量
                    int safeCount = (int) simExtracted;
                    ItemStack toInsert = itemResource.toItemStack(safeCount);
                    ItemStack doubleCheckRemainder = backpackInventory.insertItem(toInsert, true);
                    int trulyAcceptable = safeCount - doubleCheckRemainder.getCount();

                    if (trulyAcceptable <= 0) {
                        break;
                    }

                    // 4. 正式提取與寫入背包
                    long actuallyExtracted = storage.extract(itemResource, trulyAcceptable, Action.EXECUTE, actor);
                    if (actuallyExtracted <= 0) {
                        break;
                    }

                    ItemStack extractedStack = itemResource.toItemStack(actuallyExtracted);
                    ItemStack unhandled = backpackInventory.insertItem(extractedStack, false);

                    // 5. 終極防吞物保護（Rollback）：若有任何溢出未進背包，立刻倒灌回 RS
                    if (!unhandled.isEmpty()) {
                        ItemResource unhandledRes = ItemResource.ofItemStack(unhandled);
                        storage.insert(unhandledRes, unhandled.getCount(), Action.EXECUTE, actor);
                        // MoBackup.LOGGER.warn("[MoBackup-Safety] 取貨異常剩餘，已安全回存 RS: {} x{}", unhandled.getHoverName().getString(), unhandled.getCount());
                    }

                    int finalTransferred = (int) actuallyExtracted - unhandled.getCount();
                    if (finalTransferred > 0) {
                        transferredStacks.add(extractedStack.copyWithCount(finalTransferred));
                        // MoBackup.LOGGER.info("[MoBackup-Debug] 次元取貨 -> 成功從 RS 提取物資到背包: {} x{}", extractedStack.getHoverName().getString(), finalTransferred);
                    }

                    // 若本輪實際提取數量少於背包所需（說明 RS 已空），或背包產生拒收，立即結束當前物品循環
                    if (finalTransferred < acceptable || !unhandled.isEmpty()) {
                        break;
                    }
                }
            }
        }

        return transferredStacks;
    }

    public void performRestockAndNotify(Network network, Player player) {
        List<ItemStack> transferredStacks = restockFromRsNetwork(network, player);
        int stacksRestocked = transferredStacks.size();
        String translKey = stacksRestocked > 0 ? "gui.sophisticatedbackpacks.status.stacks_restocked" : "gui.sophisticatedbackpacks.status.nothing_to_restock";
        player.displayClientMessage(Component.translatable(translKey, stacksRestocked), true);
    }
}