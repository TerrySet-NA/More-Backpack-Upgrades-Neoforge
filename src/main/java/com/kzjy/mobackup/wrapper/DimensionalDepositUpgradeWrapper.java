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
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
// import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterType;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("null")
public class DimensionalDepositUpgradeWrapper extends DepositUpgradeWrapper {

    public DimensionalDepositUpgradeWrapper(IStorageWrapper backpackWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(backpackWrapper, upgrade, upgradeSaveHandler);
    }

    /**
     * 執行次元卸貨：將背包中符合過濾設定（ALLOW / BLOCK / INVENTORY）的物品存入 RS 網路
     */
    public List<ItemStack> depositToRsNetwork(Network network, Player player) {
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
        DepositFilterType filterType = getFilterLogic().getDepositFilterType();

        for (int slot = 0; slot < backpackInventory.getSlots(); slot++) {
            ItemStack stackInSlot = backpackInventory.getStackInSlot(slot);
            if (stackInSlot.isEmpty()) {
                continue;
            }

            // 先以 SIMULATE 探測背包是否允許提取該物品（防範鎖定槽位等特殊限制）
            ItemStack simExtract = backpackInventory.extractItem(slot, stackInSlot.getCount(), true);
            if (simExtract.isEmpty()) {
                continue;
            }

            ItemResource resource = ItemResource.ofItemStack(simExtract);

            // 過濾模式判斷：
            // 1. INVENTORY 模式：目標 RS 網路中本來就已有此物品才允許存入
            // 2. ALLOW / BLOCK 模式：由 FilterLogic 內部判斷白名單 / 黑名單
            boolean matches;
            if (filterType == DepositFilterType.INVENTORY) {
                matches = storage.extract(resource, 1, Action.SIMULATE, actor) > 0;
            } else {
                matches = getFilterLogic().matchesFilter(simExtract);
            }

            if (!matches) {
                continue;
            }

            // 1. 模擬插入 RS 網路，計算 RS 還能吃下幾顆
            long simInserted = storage.insert(resource, simExtract.getCount(), Action.SIMULATE, actor);
            if (simInserted <= 0) {
                continue;
            }

            // 2. 從背包正式提取等量物品
            int toExtract = (int) simInserted;
            ItemStack extracted = backpackInventory.extractItem(slot, toExtract, false);
            if (extracted.isEmpty()) {
                continue;
            }

            // 3. 正式寫入 RS 網路
            long actuallyInserted = storage.insert(resource, extracted.getCount(), Action.EXECUTE, actor);

            // 4. 終極安全回滾（Rollback）：若有任何未被 RS 吃下的剩餘，立刻塞回背包，0 損耗
            if (actuallyInserted < extracted.getCount()) {
                int leftover = extracted.getCount() - (int) actuallyInserted;
                ItemStack refund = extracted.copyWithCount(leftover);
                ItemStack unhandled = backpackInventory.insertItem(refund, false);
                if (!unhandled.isEmpty()) {
                    CapabilityHelper.runOnCapability(player, Capabilities.ItemHandler.ENTITY, null,
                            playerInv -> InventoryHelper.insertOrDropItem(player, unhandled, playerInv));
                }
            }

            if (actuallyInserted > 0) {
                transferredStacks.add(extracted.copyWithCount((int) actuallyInserted));
                // MoBackup.LOGGER.info("[MoBackup-Debug] 次元卸貨 -> 成功將 {} x{} 存入 RS 網路",
                //         extracted.getHoverName().getString(), actuallyInserted);
            }
        }

        return transferredStacks;
    }

    public void performDepositAndNotify(Network network, Player player) {
        List<ItemStack> transferredStacks = depositToRsNetwork(network, player);
        int stacksDeposited = transferredStacks.size();
        String translKey = stacksDeposited > 0 ? "gui.sophisticatedbackpacks.status.stacks_deposited" : "gui.sophisticatedbackpacks.status.nothing_to_deposit";
        player.displayClientMessage(Component.translatable(translKey, stacksDeposited), true);
    }
}