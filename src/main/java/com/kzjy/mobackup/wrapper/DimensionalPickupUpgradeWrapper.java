/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.wrapper;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.kzjy.mobackup.core.PickupContext;
import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;

public class DimensionalPickupUpgradeWrapper extends PickupUpgradeWrapper implements IPriorityRoutingUpgrade {

    public DimensionalPickupUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
                                           Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    private Boolean networkFirstCache = null;

    @Override
    public boolean isNetworkFirst() {
        if (networkFirstCache == null) {
            CustomData customData = upgrade.get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.contains(TAG_NETWORK_FIRST)) {
                networkFirstCache = customData.copyTag().getBoolean(TAG_NETWORK_FIRST);
            } else {
                networkFirstCache = true;
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

        Player playerCtx = PickupContext.current();

        if (isNetworkFirst()) {
            Network network = RSBridge.getNetwork(world, getUpgradeStack(), playerCtx, BuiltinPermission.INSERT);
            if (network != null && RSBridge.canInsert(network, playerCtx)) {
                stack = insertIntoRsNetwork(network, stack, simulate, playerCtx);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
            return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
        } else {
            stack = storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            Network network = RSBridge.getNetwork(world, getUpgradeStack(), playerCtx, BuiltinPermission.INSERT);
            if (network != null && RSBridge.canInsert(network, playerCtx)) {
                stack = insertIntoRsNetwork(network, stack, simulate, playerCtx);
            }
            return stack;
        }
    }

    private ItemStack insertIntoRsNetwork(Network network, ItemStack stack, boolean simulate, @Nullable Player player) {
        if (network == null || stack.isEmpty()) {
            return stack;
        }

        if (!RSBridge.canInsert(network, player)) {
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