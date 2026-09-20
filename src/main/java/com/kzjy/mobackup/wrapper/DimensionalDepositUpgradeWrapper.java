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
import com.kzjy.mobackup.duck.IDepositFilterLogicExtension;
import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import com.kzjy.mobackup.util.SafetyRollbackHelper;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterType;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;

public class DimensionalDepositUpgradeWrapper extends DepositUpgradeWrapper implements IPriorityRoutingUpgrade {

    public DimensionalDepositUpgradeWrapper(IStorageWrapper backpackWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(backpackWrapper, upgrade, upgradeSaveHandler);
    }

    private Boolean networkFirstCache = null;

    @Override
    public boolean isNetworkFirst() {
        if (networkFirstCache == null) {
            CustomData customData = upgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (customData.contains(TAG_NETWORK_FIRST)) {
                networkFirstCache = customData.copyTag().getBoolean(TAG_NETWORK_FIRST);
            } else {
                networkFirstCache = false;
            }
        }
        return networkFirstCache;
    }

    @Override
    public void setNetworkFirst(boolean networkFirst) {
        this.networkFirstCache = networkFirst;
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, tag -> tag.putBoolean(TAG_NETWORK_FIRST, networkFirst));
        save();
    }

    public void performQuickDepositToLinkedRs(@Nullable Player actionPlayer, Player messageTarget, Level safeLevel) {
        if (RSBridge.getCoordinate(getUpgradeStack()) == null) {
            messageTarget.displayClientMessage(Component.translatable("misc.refinedstorage.network_card.not_found"), true);
            return;
        }

        Network linkedNetwork = RSBridge.getNetwork(safeLevel, getUpgradeStack(), actionPlayer, BuiltinPermission.INSERT);
        if (linkedNetwork == null) {
            messageTarget.displayClientMessage(Component.translatable("misc.moback.no_permission.insert"), true);
            return;
        }

        List<ItemStack> transferred = depositToRsNetwork(linkedNetwork, actionPlayer);
        int count = transferred.size();
        String key = count > 0 ? "gui.sophisticatedbackpacks.status.stacks_deposited" : "gui.sophisticatedbackpacks.status.nothing_to_deposit";
        messageTarget.displayClientMessage(Component.translatable(key, count), true);
    }

    @Override
    public void onHandlerInteract(IItemHandler itemHandler, @Nullable Player player) {
        if (player == null) return;
        List<ItemStack> transferred = depositToHandlerWithPriority(itemHandler, player);
        int stacksDeposited = transferred.size();
        String translKey = stacksDeposited > 0 ? "gui.sophisticatedbackpacks.status.stacks_deposited" : "gui.sophisticatedbackpacks.status.nothing_to_deposit";
        player.displayClientMessage(Component.translatable(translKey, stacksDeposited), true);
    }

    public List<ItemStack> depositToHandlerWithPriority(IItemHandler targetHandler, @Nullable Player player) {
        List<ItemStack> transferred = new ArrayList<>();
        if (player == null) return transferred;

        Level level = player.level();

        if (getFilterLogic().getDepositFilterType() == DepositFilterType.INVENTORY) {
            getFilterLogic().setInventory(targetHandler);
        }

        if (isNetworkFirst()) {
            Network linkedNetwork = RSBridge.getNetwork(level, getUpgradeStack(), player, BuiltinPermission.EXTRACT);
            if (linkedNetwork == null) {
                player.displayClientMessage(Component.translatable("misc.moback.no_permission.extract"), true);
                return transferred;
            }
            transferred.addAll(depositFromRsToHandler(linkedNetwork, targetHandler, player));
        } else {
            transferred.addAll(super.depositToHandler(targetHandler));
        }

        return transferred;
    }

    private List<ItemStack> depositFromRsToHandler(Network sourceNetwork, IItemHandler targetHandler, @Nullable Player player) {
        List<ItemStack> transferred = new ArrayList<>();
        StorageNetworkComponent sourceStorage = sourceNetwork.getComponent(StorageNetworkComponent.class);
        if (sourceStorage == null) return transferred;

        Actor actor = player != null ? new PlayerActor(player) : Actor.EMPTY;

        for (ResourceAmount ra : new ArrayList<>(sourceStorage.getAll())) {
            if (isHandlerFull(targetHandler)) break;
            if (ra.amount() <= 0) continue;

            if (ra.resource() instanceof ItemResource itemResource) {
                ItemStack sample = itemResource.toItemStack(1);
                if (!getFilterLogic().matchesFilter(sample)) continue;

                int maxStackSize = sample.getMaxStackSize();

                while (true) {
                    ItemStack probeStack = itemResource.toItemStack(maxStackSize);
                    ItemStack remainder = ItemHandlerHelper.insertItem(targetHandler, probeStack, true);
                    int canAccept = maxStackSize - remainder.getCount();
                    if (canAccept <= 0) break;

                    long simExtracted = sourceStorage.extract(itemResource, canAccept, Action.SIMULATE, actor);
                    if (simExtracted <= 0) break;

                    int safeCount = (int) simExtracted;
                    long actuallyExtracted = sourceStorage.extract(itemResource, safeCount, Action.EXECUTE, actor);
                    if (actuallyExtracted <= 0) break;

                    ItemStack toInsert = itemResource.toItemStack((int) actuallyExtracted);
                    ItemStack failed = ItemHandlerHelper.insertItem(targetHandler, toInsert, false);

                    if (!failed.isEmpty()) {
                        long refunded = sourceStorage.insert(itemResource, failed.getCount(), Action.EXECUTE, actor);
                        if (refunded < failed.getCount()) {
                            ItemStack lost = failed.copyWithCount(failed.getCount() - (int) refunded);
                            SafetyRollbackHelper.fallbackToBackpackOrPlayer(lost, storageWrapper, player);
                        }
                    }

                    int actuallyMoved = (int) actuallyExtracted - failed.getCount();
                    if (actuallyMoved > 0) {
                        transferred.add(itemResource.toItemStack(actuallyMoved));
                    }

                    if (actuallyMoved < canAccept || !failed.isEmpty()) break;
                }
            }
        }
        return transferred;
    }

    public void performDepositAndNotify(Network clickedNetwork, @Nullable Player player) {
        if (player == null) return;
        Level level = player.level();

        if (!RSBridge.validateClickedNetwork(clickedNetwork, player, BuiltinPermission.INSERT)) {
            player.displayClientMessage(Component.translatable("misc.moback.no_permission.insert"), true);
            return;
        }

        List<ItemStack> transferredStacks = new ArrayList<>();

        if (isNetworkFirst()) {
            Network linkedNetworkRaw = RSBridge.getNetwork(level, getUpgradeStack());
            if (linkedNetworkRaw != null && linkedNetworkRaw.equals(clickedNetwork)) {
                player.displayClientMessage(Component.translatable("gui.sophisticatedbackpacks.status.nothing_to_deposit"), true);
                return;
            }
            Network linkedNetwork = RSBridge.getNetwork(level, getUpgradeStack(), player, BuiltinPermission.EXTRACT);
            if (linkedNetwork == null) {
                player.displayClientMessage(Component.translatable("misc.moback.no_permission.extract"), true);
                return;
            }
            transferredStacks.addAll(depositFromRsToTargetRs(linkedNetwork, clickedNetwork, player));
        } else {
            transferredStacks.addAll(depositToRsNetwork(clickedNetwork, player));
        }

        int count = transferredStacks.size();
        String translKey = count > 0 ? "gui.sophisticatedbackpacks.status.stacks_deposited" : "gui.sophisticatedbackpacks.status.nothing_to_deposit";
        player.displayClientMessage(Component.translatable(translKey, count), true);
    }

    private List<ItemStack> depositFromRsToTargetRs(Network sourceNetwork, Network targetNetwork, @Nullable Player player) {
        List<ItemStack> transferredStacks = new ArrayList<>();
        StorageNetworkComponent sourceStorage = sourceNetwork.getComponent(StorageNetworkComponent.class);
        StorageNetworkComponent targetStorage = targetNetwork.getComponent(StorageNetworkComponent.class);
        if (sourceStorage == null || targetStorage == null) return transferredStacks;

        if (getFilterLogic() instanceof IDepositFilterLogicExtension ext) {
            ext.mobackup$setRsStorage(targetStorage);
        }

        try {
            Actor actor = player != null ? new PlayerActor(player) : Actor.EMPTY;

            for (ResourceAmount ra : new ArrayList<>(sourceStorage.getAll())) {
                if (ra.amount() <= 0) continue;

                if (ra.resource() instanceof ItemResource itemResource) {
                    ItemStack sample = itemResource.toItemStack(1);
                    if (!getFilterLogic().matchesFilter(sample)) continue;

                    long canInsert = targetStorage.insert(itemResource, ra.amount(), Action.SIMULATE, actor);
                    if (canInsert > 0) {
                        long extracted = sourceStorage.extract(itemResource, canInsert, Action.EXECUTE, actor);
                        if (extracted > 0) {
                            long actuallyInserted = targetStorage.insert(itemResource, extracted, Action.EXECUTE, actor);

                            if (actuallyInserted < extracted) {
                                long toRefund = extracted - actuallyInserted;
                                long refunded = sourceStorage.insert(itemResource, toRefund, Action.EXECUTE, actor);
                                if (refunded < toRefund) {
                                    ItemStack lost = itemResource.toItemStack((int) (toRefund - refunded));
                                    SafetyRollbackHelper.fallbackToBackpackOrPlayer(lost, storageWrapper, player);
                                }
                            }
                            if (actuallyInserted > 0) {
                                transferredStacks.add(itemResource.toItemStack((int) actuallyInserted));
                            }
                        }
                    }
                }
            }
        } finally {
            if (getFilterLogic() instanceof IDepositFilterLogicExtension ext) {
                ext.mobackup$setRsStorage(null);
            }
        }
        return transferredStacks;
    }

    public List<ItemStack> depositToRsNetwork(Network network, @Nullable Player player) {
        List<ItemStack> transferredStacks = new ArrayList<>();
        if (network == null) return transferredStacks;

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) return transferredStacks;

        if (getFilterLogic() instanceof IDepositFilterLogicExtension ext) {
            ext.mobackup$setRsStorage(storage);
        }

        try {
            ITrackedContentsItemHandler backpackInventory = storageWrapper.getInventoryForUpgradeProcessing();
            Actor actor = player != null ? new PlayerActor(player) : Actor.EMPTY;

            for (int slot = 0; slot < backpackInventory.getSlots(); slot++) {
                ItemStack stackInSlot = backpackInventory.getStackInSlot(slot);
                if (stackInSlot.isEmpty()) continue;
                if (!getFilterLogic().matchesFilter(stackInSlot)) continue;

                ItemStack simExtract = backpackInventory.extractItem(slot, stackInSlot.getCount(), true);
                if (simExtract.isEmpty()) continue;

                ItemResource resource = ItemResource.ofItemStack(simExtract);
                long simInserted = storage.insert(resource, simExtract.getCount(), Action.SIMULATE, actor);
                if (simInserted <= 0) continue;

                int toExtract = (int) simInserted;
                ItemStack extracted = backpackInventory.extractItem(slot, toExtract, false);
                if (extracted.isEmpty()) continue;

                long actuallyInserted = storage.insert(resource, extracted.getCount(), Action.EXECUTE, actor);
                if (actuallyInserted < extracted.getCount()) {
                    int leftover = extracted.getCount() - (int) actuallyInserted;
                    ItemStack refund = extracted.copyWithCount(leftover);
                    SafetyRollbackHelper.fallbackToBackpackOrPlayer(refund, storageWrapper, player);
                }
                if (actuallyInserted > 0) {
                    transferredStacks.add(extracted.copyWithCount((int) actuallyInserted));
                }
            }
        } finally {
            if (getFilterLogic() instanceof IDepositFilterLogicExtension ext) {
                ext.mobackup$setRsStorage(null);
            }
        }
        return transferredStacks;
    }

    private boolean isHandlerFull(IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }
}