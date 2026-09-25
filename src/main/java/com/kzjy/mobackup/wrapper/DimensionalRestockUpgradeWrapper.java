/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;

import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import com.kzjy.mobackup.util.SafetyRollbackHelper;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;

public class DimensionalRestockUpgradeWrapper extends RestockUpgradeWrapper implements IPriorityRoutingUpgrade {

    public DimensionalRestockUpgradeWrapper(IStorageWrapper backpackWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(backpackWrapper, upgrade, upgradeSaveHandler);
    }

    @Override
    public void save() {
        super.save();
    }

    public void performQuickRestockFromLinkedRs(Player messageTarget, Level safeLevel) {
        if (RSBridge.getCoordinate(getUpgradeStack()) == null) {
            messageTarget.displayClientMessage(Component.translatable("misc.mobackup.network_card.not_found"), true);
            return;
        }

        // 位置以正在操作 GUI 的玩家 (messageTarget) 為準檢測距離，身分以 actionPlayer 為準
        Network linkedNetwork = RSBridge.getNetwork(safeLevel, getUpgradeStack(), messageTarget, messageTarget.blockPosition(), BuiltinPermission.EXTRACT);
        if (linkedNetwork == null) {
            messageTarget.displayClientMessage(Component.translatable("misc.mobackup.no_permission.extract"), true);
            return;
        }

        List<ItemStack> transferred = restockFromRsToBackpack(linkedNetwork, messageTarget);
        int count = transferred.size();
        String key = count > 0 ? "gui.sophisticatedbackpacks.status.stacks_restocked" : "gui.sophisticatedbackpacks.status.nothing_to_restock";
        messageTarget.displayClientMessage(Component.translatable(key, count), true);
    }

    @Override
    public void onHandlerInteract(IItemHandler itemHandler, @Nullable Player player) {
        if (player == null) return;
        List<ItemStack> transferred = restockFromHandlerWithPriority(itemHandler, player);
        int stacksRestocked = transferred.size();
        String translKey = stacksRestocked > 0 ? "gui.sophisticatedbackpacks.status.stacks_restocked" : "gui.sophisticatedbackpacks.status.nothing_to_restock";
        player.displayClientMessage(Component.translatable(translKey, stacksRestocked), true);
    }

    public List<ItemStack> restockFromHandlerWithPriority(IItemHandler sourceHandler, Player player) {
        List<ItemStack> transferred = new ArrayList<>();
        Level level = player.level();
        StorageNetworkComponent targetStorage = null;

        if (isNetworkFirst()) {
            Network linkedNetwork = RSBridge.getNetwork(level, getUpgradeStack(), player, BuiltinPermission.INSERT);
            if (linkedNetwork == null) {
                player.displayClientMessage(Component.translatable("misc.mobackup.no_permission.insert"), true);
                return transferred;
            }
            targetStorage = linkedNetwork.getComponent(StorageNetworkComponent.class);
            if (targetStorage == null) return transferred;
        }

        ITrackedContentsItemHandler backpackInventory = storageWrapper.getInventoryForUpgradeProcessing();
        Actor actor = RSBridge.getActor(player);

        for (int slot = 0; slot < sourceHandler.getSlots(); slot++) {
            ItemStack inSlot = sourceHandler.getStackInSlot(slot);
            if (inSlot.isEmpty() || !getFilterLogic().matchesFilter(inSlot)) continue;

            ItemStack simExtract = sourceHandler.extractItem(slot, inSlot.getCount(), true);
            if (simExtract.isEmpty()) continue;

            int availableCount = simExtract.getCount();
            int toRs = 0;
            int toBackpack = 0;

            if (isNetworkFirst()) {
                ItemResource res = ItemResource.ofItemStack(simExtract);
                long rsCanAccept = targetStorage.insert(res, availableCount, Action.SIMULATE, actor);
                toRs = (int) Math.min(availableCount, rsCanAccept);
            } else {
                toBackpack = calculateAcceptableForBackpack(backpackInventory, simExtract, availableCount);
            }

            int totalToMove = isNetworkFirst() ? toRs : toBackpack;
            if (totalToMove <= 0) continue;

            ItemStack realExtracted = sourceHandler.extractItem(slot, totalToMove, false);
            if (realExtracted.isEmpty()) continue;

            // 1. 背包模式注入
            if (toBackpack > 0) {
                int countForBackpack = Math.min(toBackpack, realExtracted.getCount());
                ItemStack stackForBackpack = realExtracted.copyWithCount(countForBackpack);
                ItemStack backpackFailed = backpackInventory.insertItem(stackForBackpack, false);

                if (!backpackFailed.isEmpty()) {
                    ItemStack containerRejected = ItemHandlerHelper.insertItem(sourceHandler, backpackFailed, false);
                    if (!containerRejected.isEmpty()) fallbackSafety(containerRejected, player);
                }
                int backpackMoved = countForBackpack - backpackFailed.getCount();
                if (backpackMoved > 0) transferred.add(realExtracted.copyWithCount(backpackMoved));
            }

            // 2. 網路模式注入
            if (toRs > 0 && targetStorage != null) {
                int countForRs = Math.min(toRs, realExtracted.getCount());
                ItemResource res = ItemResource.ofItemStack(realExtracted);
                long actuallyInserted = targetStorage.insert(res, countForRs, Action.EXECUTE, actor);

                if (actuallyInserted < countForRs) {
                    int refund = countForRs - (int) actuallyInserted;
                    ItemStack refundStack = realExtracted.copyWithCount(refund);
                    ItemStack containerRejected = ItemHandlerHelper.insertItem(sourceHandler, refundStack, false);
                    if (!containerRejected.isEmpty()) fallbackSafety(containerRejected, player);
                }
                if (actuallyInserted > 0) transferred.add(realExtracted.copyWithCount((int) actuallyInserted));
            }
        }
        return transferred;
    }

    public void performRestockAndNotify(Network clickedNetwork, @Nullable Player player) {
        if (player == null) return;
        Level level = player.level();

        if (!RSBridge.validateClickedNetwork(clickedNetwork, player, BuiltinPermission.EXTRACT)) {
            player.displayClientMessage(Component.translatable("misc.mobackup.no_permission.extract"), true);
            return;
        }

        List<ItemStack> transferredStacks = new ArrayList<>();

        if (isNetworkFirst()) {
            Network linkedNetworkRaw = RSBridge.getNetwork(level, getUpgradeStack());
            if (linkedNetworkRaw != null && linkedNetworkRaw.equals(clickedNetwork)) {
                player.displayClientMessage(Component.translatable("gui.sophisticatedbackpacks.status.nothing_to_restock"), true);
                return;
            }
            Network linkedNetwork = RSBridge.getNetwork(level, getUpgradeStack(), player, BuiltinPermission.INSERT);
            if (linkedNetwork == null) {
                player.displayClientMessage(Component.translatable("misc.mobackup.no_permission.insert"), true);
                return;
            }
            transferredStacks.addAll(restockFromRsToTargetRs(clickedNetwork, linkedNetwork, player));
        } else {
            transferredStacks.addAll(restockFromRsToBackpack(clickedNetwork, player));
        }

        int count = transferredStacks.size();
        String translKey = count > 0 ? "gui.sophisticatedbackpacks.status.stacks_restocked" : "gui.sophisticatedbackpacks.status.nothing_to_restock";
        player.displayClientMessage(Component.translatable(translKey, count), true);
    }

    public List<ItemStack> restockFromRsToBackpack(Network network, @Nullable Player player) {
        List<ItemStack> transferredStacks = new ArrayList<>();
        if (network == null) return transferredStacks;
        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) return transferredStacks;

        ITrackedContentsItemHandler backpackInventory = storageWrapper.getInventoryForUpgradeProcessing();
        Actor actor = RSBridge.getActor(player);

        for (ResourceAmount resourceAmount : new ArrayList<>(storage.getAll())) {
            if (resourceAmount.amount() <= 0) continue;
            if (resourceAmount.resource() instanceof ItemResource itemResource) {
                ItemStack sample = itemResource.toItemStack(1);
                if (!getFilterLogic().matchesFilter(sample)) continue;

                int maxStack = sample.getMaxStackSize();
                while (true) {
                    ItemStack probeStack = itemResource.toItemStack(maxStack);
                    ItemStack remainder = backpackInventory.insertItem(probeStack, true);
                    int acceptable = maxStack - remainder.getCount();
                    if (acceptable <= 0) break;

                    long simExtracted = storage.extract(itemResource, acceptable, Action.SIMULATE, actor);
                    if (simExtracted <= 0) break;

                    int safeCount = (int) Math.min(acceptable, simExtracted);
                    long actuallyExtracted = storage.extract(itemResource, safeCount, Action.EXECUTE, actor);
                    if (actuallyExtracted <= 0) break;

                    ItemStack extractedStack = itemResource.toItemStack((int) actuallyExtracted);
                    ItemStack unhandled = backpackInventory.insertItem(extractedStack, false);

                    if (!unhandled.isEmpty()) {
                        long rsRefunded = storage.insert(ItemResource.ofItemStack(unhandled), unhandled.getCount(), Action.EXECUTE, actor);
                        if (rsRefunded < unhandled.getCount()) {
                            ItemStack ultimateLeftover = unhandled.copyWithCount(unhandled.getCount() - (int) rsRefunded);
                            fallbackSafety(ultimateLeftover, player);
                        }
                    }

                    int finalTransferred = (int) actuallyExtracted - unhandled.getCount();
                    if (finalTransferred > 0) transferredStacks.add(extractedStack.copyWithCount(finalTransferred));
                    if (finalTransferred < acceptable || !unhandled.isEmpty()) break;
                }
            }
        }
        return transferredStacks;
    }

    private List<ItemStack> restockFromRsToTargetRs(Network sourceNetwork, Network targetNetwork, @Nullable Player player) {
        List<ItemStack> transferredStacks = new ArrayList<>();
        StorageNetworkComponent sourceStorage = sourceNetwork.getComponent(StorageNetworkComponent.class);
        StorageNetworkComponent targetStorage = targetNetwork.getComponent(StorageNetworkComponent.class);
        if (sourceStorage == null || targetStorage == null) return transferredStacks;

        Actor actor = RSBridge.getActor(player);

        for (ResourceAmount ra : new ArrayList<>(sourceStorage.getAll())) {
            if (ra.amount() <= 0) continue;
            if (ra.resource() instanceof ItemResource itemResource) {
                ItemStack sample = itemResource.toItemStack(1);
                if (!getFilterLogic().matchesFilter(sample)) continue;

                while (true) {
                    long canInsert = targetStorage.insert(itemResource, sample.getMaxStackSize(), Action.SIMULATE, actor);
                    if (canInsert <= 0) break;
                    long extracted = sourceStorage.extract(itemResource, canInsert, Action.EXECUTE, actor);
                    if (extracted <= 0) break;

                    long actuallyInserted = targetStorage.insert(itemResource, extracted, Action.EXECUTE, actor);
                    if (actuallyInserted < extracted) {
                        long toRefund = extracted - actuallyInserted;
                        long refunded = sourceStorage.insert(itemResource, toRefund, Action.EXECUTE, actor);
                        if (refunded < toRefund) {
                            ItemStack lostStack = itemResource.toItemStack((int) (toRefund - refunded));
                            fallbackSafety(lostStack, player);
                        }
                    }
                    if (actuallyInserted > 0) transferredStacks.add(itemResource.toItemStack((int) actuallyInserted));
                    if (actuallyInserted < canInsert) break;
                }
            }
        }
        return transferredStacks;
    }

    private int calculateAcceptableForBackpack(ITrackedContentsItemHandler backpackInventory, ItemStack sample, int available) {
        ItemStack probeStack = sample.copyWithCount(available);
        ItemStack remainder = backpackInventory.insertItem(probeStack, true);
        return available - remainder.getCount();
    }

    private void fallbackSafety(ItemStack stack, @Nullable Player player) {
        SafetyRollbackHelper.fallbackToBackpackOrPlayer(stack, storageWrapper, player);
    }
}