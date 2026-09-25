/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.wrapper;

import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import com.kzjy.mobackup.util.SafetyRollbackHelper;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

public class DimensionalRefillUpgradeWrapper extends RefillUpgradeWrapper implements IPriorityRoutingUpgrade {
    private static final int REFILL_RANGE = 3;
    private static final int COOLDOWN = 5;
    private BlockPos currentBlockPos = null;

    public DimensionalRefillUpgradeWrapper(IStorageWrapper backpackWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(backpackWrapper, upgrade, upgradeSaveHandler);
    }

    @Override
    public void save() {
        super.save();
    }

    @Override
    public boolean getDefaultNetworkFirst() {
        return false; // 補貨升級預設背包優先
    }

    @Override
    public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (isInCooldown(level)) {
            return;
        }

        currentBlockPos = pos.immutable();

        if (entity instanceof Player carrierPlayer) {
            refillItemForCustom(carrierPlayer, carrierPlayer);
        } else {
            level.getEntities(EntityType.PLAYER, new AABB(pos).inflate(REFILL_RANGE), p -> true)
                    .forEach(nearbyPlayer -> refillItemForCustom(nearbyPlayer, null));
        }
        setCooldown(level, COOLDOWN);
    }

    private void refillItemForCustom(Player targetPlayer, @Nullable Player actionPlayer) {
        CapabilityHelper.runOnItemHandler(targetPlayer, playerInvHandler -> InventoryHelper.iterate(getFilterLogic().getFilterHandler(), (slot, filter) -> {
            if (filter.isEmpty()) return;
            tryRefillFilterCustom(targetPlayer, actionPlayer, playerInvHandler, filter, getTargetSlots().getOrDefault(slot, TargetSlot.ANY));
        }));
    }

    private void tryRefillFilterCustom(Player targetPlayer, @Nullable Player actionPlayer, IItemHandler playerInvHandler, ItemStack filter, TargetSlot targetSlot) {
        int missingCount = getMissingCountCustom(targetSlot, targetPlayer, playerInvHandler, filter);
        if (ItemStack.isSameItemSameComponents(targetPlayer.containerMenu.getCarried(), filter)) {
            missingCount -= Math.min(missingCount, targetPlayer.containerMenu.getCarried().getCount());
        }
        if (missingCount <= 0) return;

        if (isNetworkFirst()) {
            int filledFromRs = refillFromRs(targetPlayer, actionPlayer, playerInvHandler, filter, targetSlot, missingCount);
            missingCount -= filledFromRs;

            if (missingCount > 0) {
                refillFromBackpack(targetPlayer, playerInvHandler, filter, targetSlot, missingCount);
            }
        } else {
            int filledFromBackpack = refillFromBackpack(targetPlayer, playerInvHandler, filter, targetSlot, missingCount);
            missingCount -= filledFromBackpack;

            if (missingCount > 0) {
                refillFromRs(targetPlayer, actionPlayer, playerInvHandler, filter, targetSlot, missingCount);
            }
        }
    }

    private int refillFromRs(Player targetPlayer, @Nullable Player actionPlayer, IItemHandler playerInvHandler, ItemStack filter, TargetSlot targetSlot, int maxCount) {
        Network network = RSBridge.getNetwork(targetPlayer.level(), getUpgradeStack(), actionPlayer, currentBlockPos, BuiltinPermission.EXTRACT);
        if (network == null) return 0;

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) return 0;

        ItemResource resource = ItemResource.ofItemStack(filter);
        Actor actor = RSBridge.getActor(actionPlayer);

        long simExtracted = storage.extract(resource, maxCount, Action.SIMULATE, actor);
        if (simExtracted <= 0) return 0;

        ItemStack probeStack = filter.copyWithCount((int) simExtracted);
        int canAccept = simulateFillTargetSlot(targetSlot, targetPlayer, playerInvHandler, probeStack);
        if (canAccept <= 0) return 0;

        long actuallyExtracted = storage.extract(resource, canAccept, Action.EXECUTE, actor);
        if (actuallyExtracted <= 0) return 0;

        ItemStack toFill = filter.copyWithCount((int) actuallyExtracted);
        ItemStack remaining = executeFillTargetSlot(targetSlot, targetPlayer, playerInvHandler, toFill);

        if (!remaining.isEmpty()) {
            long refunded = storage.insert(resource, remaining.getCount(), Action.EXECUTE, actor);
            if (refunded < remaining.getCount()) {
                ItemStack lost = remaining.copyWithCount(remaining.getCount() - (int) refunded);
                SafetyRollbackHelper.fallbackToBackpackOrPlayer(lost, storageWrapper, targetPlayer);
            }
        }

        return (int) actuallyExtracted - remaining.getCount();
    }

    private int refillFromBackpack(Player targetPlayer, IItemHandler playerInvHandler, ItemStack filter, TargetSlot targetSlot, int maxCount) {
        IItemHandler backpackInv = storageWrapper.getInventoryForUpgradeProcessing();

        ItemStack probeExtract = filter.copyWithCount(maxCount);
        ItemStack simExtracted = InventoryHelper.extractFromInventory(probeExtract, backpackInv, true);
        if (simExtracted.isEmpty()) return 0;

        int canAccept = simulateFillTargetSlot(targetSlot, targetPlayer, playerInvHandler, simExtracted);
        if (canAccept <= 0) return 0;

        ItemStack toExtract = filter.copyWithCount(canAccept);
        ItemStack actuallyExtracted = InventoryHelper.extractFromInventory(toExtract, backpackInv, false);
        if (actuallyExtracted.isEmpty()) return 0;

        ItemStack remaining = executeFillTargetSlot(targetSlot, targetPlayer, playerInvHandler, actuallyExtracted);

        if (!remaining.isEmpty()) {
            ItemStack backRemainder = ItemHandlerHelper.insertItem(backpackInv, remaining, false);
            if (!backRemainder.isEmpty()) {
                SafetyRollbackHelper.fallbackToBackpackOrPlayer(backRemainder, storageWrapper, targetPlayer);
            }
        }

        return actuallyExtracted.getCount() - remaining.getCount();
    }

    @Override
    public boolean pickBlock(Player player, ItemStack filter) {
        if (!upgradeItem.supportsBlockPick()) return false;

        if (isNetworkFirst()) {
            if (pickBlockFromRs(player, filter)) return true;
            return super.pickBlock(player, filter);
        } else {
            if (super.pickBlock(player, filter)) return true;
            return pickBlockFromRs(player, filter);
        }
    }

    private boolean pickBlockFromRs(Player player, ItemStack filter) {
        Network network = RSBridge.getNetwork(player.level(), getUpgradeStack(), player, currentBlockPos, BuiltinPermission.EXTRACT);
        if (network == null) return false;

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) return false;

        ItemResource resource = ItemResource.ofItemStack(filter);
        Actor actor = RSBridge.getActor(player);

        long simExtracted = storage.extract(resource, filter.getMaxStackSize(), Action.SIMULATE, actor);
        if (simExtracted <= 0) return false;

        int targetHotbarSlot = player.getInventory().getSuitableHotbarSlot();
        ItemStack selectedItem = player.getInventory().getItem(targetHotbarSlot);

        if (!selectedItem.isEmpty()) {
            boolean stashed = false;

            if (isNetworkFirst() && RSBridge.canInsert(network, player)) {
                ItemResource selectedRes = ItemResource.ofItemStack(selectedItem);
                long simInsert = storage.insert(selectedRes, selectedItem.getCount(), Action.SIMULATE, actor);
                if (simInsert >= selectedItem.getCount()) {
                    long actuallyInserted = storage.insert(selectedRes, selectedItem.getCount(), Action.EXECUTE, actor);
                    if (actuallyInserted >= selectedItem.getCount()) {
                        stashed = true;
                    } else {
                        int left = selectedItem.getCount() - (int) actuallyInserted;
                        SafetyRollbackHelper.fallbackToBackpackOrPlayer(selectedItem.copyWithCount(left), storageWrapper, player);
                        stashed = true;
                    }
                }
            }

            if (!stashed) {
                ItemStack remainder = storageWrapper.getInventoryForUpgradeProcessing().insertItem(selectedItem, false);
                if (remainder.isEmpty()) {
                    stashed = true;
                } else if (canMoveSelectedToInventoryCustom(player, remainder, targetHotbarSlot)) {
                    player.getInventory().add(remainder);
                    stashed = true;
                } else if (RSBridge.canInsert(network, player)) {
                    ItemResource selectedRes = ItemResource.ofItemStack(remainder);
                    long actuallyInserted = storage.insert(selectedRes, remainder.getCount(), Action.EXECUTE, actor);
                    if (actuallyInserted > 0) {
                        int left = remainder.getCount() - (int) actuallyInserted;
                        if (left > 0) {
                            SafetyRollbackHelper.fallbackToBackpackOrPlayer(remainder.copyWithCount(left), storageWrapper, player);
                        }
                        stashed = true;
                    }
                }
            }

            if (!stashed) return false;

            player.getInventory().setItem(targetHotbarSlot, ItemStack.EMPTY);
        }

        player.getInventory().selected = targetHotbarSlot;
        long actuallyExtracted = storage.extract(resource, simExtracted, Action.EXECUTE, actor);
        if (actuallyExtracted > 0) {
            ItemStack extracted = filter.copyWithCount((int) actuallyExtracted);
            player.setItemInHand(InteractionHand.MAIN_HAND, extracted);
            return true;
        }

        return false;
    }

    private boolean canMoveSelectedToInventoryCustom(Player player, ItemStack stackToMove, int targetSlot) {
        int countToAdd = stackToMove.getCount();
        for (int slot = 0; slot < player.getInventory().getContainerSize() - 5; slot++) {
            if (slot == targetSlot) continue;
            ItemStack slotStack = player.getInventory().getItem(slot);
            if (slotStack.isEmpty()) {
                return true;
            } else if (ItemStack.isSameItemSameComponents(slotStack, stackToMove)) {
                countToAdd -= (slotStack.getMaxStackSize() - slotStack.getCount());
                if (countToAdd <= 0) return true;
            }
        }
        return false;
    }

    private int getMissingCountCustom(TargetSlot slot, Player player, IItemHandler playerInvHandler, ItemStack filter) {
        if (slot == TargetSlot.ANY) {
            return InventoryHelper.getCountMissingInHandler(playerInvHandler, filter, filter.getMaxStackSize());
        } else if (slot == TargetSlot.MAIN_HAND) {
            return getMissingCountInSlot(player.getMainHandItem(), filter);
        } else if (slot == TargetSlot.OFF_HAND) {
            return getMissingCountInSlot(player.getOffhandItem(), filter);
        } else {
            int toolbarIndex = slot.ordinal() - TargetSlot.TOOLBAR_1.ordinal();
            if (toolbarIndex >= 0 && toolbarIndex < 9) {
                return getMissingCountInSlot(player.getInventory().getItem(toolbarIndex), filter);
            }
        }
        return 0;
    }

    private static int getMissingCountInSlot(ItemStack stack, ItemStack filter) {
        if (ItemStack.isSameItemSameComponents(stack, filter)) {
            return filter.getMaxStackSize() - stack.getCount();
        }
        return filter.getMaxStackSize();
    }

    private int simulateFillTargetSlot(TargetSlot slot, Player player, IItemHandler playerInvHandler, ItemStack stackToAdd) {
        if (slot == TargetSlot.ANY) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(playerInvHandler, stackToAdd, true);
            return stackToAdd.getCount() - remainder.getCount();
        } else if (slot == TargetSlot.MAIN_HAND) {
            return simulateFillSingleSlot(player.getMainHandItem(), stackToAdd);
        } else if (slot == TargetSlot.OFF_HAND) {
            return simulateFillSingleSlot(player.getOffhandItem(), stackToAdd);
        } else {
            int toolbarIndex = slot.ordinal() - TargetSlot.TOOLBAR_1.ordinal();
            if (toolbarIndex >= 0 && toolbarIndex < 9) {
                return simulateFillSingleSlot(player.getInventory().getItem(toolbarIndex), stackToAdd);
            }
        }
        return 0;
    }

    private static int simulateFillSingleSlot(ItemStack slotStack, ItemStack stackToAdd) {
        int maxStackSize = stackToAdd.getMaxStackSize();
        if (slotStack.isEmpty()) {
            return Math.min(stackToAdd.getCount(), maxStackSize);
        }
        if (ItemStack.isSameItemSameComponents(slotStack, stackToAdd)) {
            return Math.min(stackToAdd.getCount(), maxStackSize - slotStack.getCount());
        }
        return 0;
    }

    private ItemStack executeFillTargetSlot(TargetSlot slot, Player player, IItemHandler playerInvHandler, ItemStack stackToAdd) {
        if (slot == TargetSlot.ANY) {
            return ItemHandlerHelper.insertItemStacked(playerInvHandler, stackToAdd, false);
        } else if (slot == TargetSlot.MAIN_HAND) {
            return executeFillSingleSlot(player::getMainHandItem, s -> player.setItemInHand(InteractionHand.MAIN_HAND, s), stackToAdd);
        } else if (slot == TargetSlot.OFF_HAND) {
            return executeFillSingleSlot(player::getOffhandItem, s -> player.setItemInHand(InteractionHand.OFF_HAND, s), stackToAdd);
        } else {
            int toolbarIndex = slot.ordinal() - TargetSlot.TOOLBAR_1.ordinal();
            if (toolbarIndex >= 0 && toolbarIndex < 9) {
                return executeFillSingleSlot(() -> player.getInventory().getItem(toolbarIndex), s -> player.getInventory().setItem(toolbarIndex, s), stackToAdd);
            }
        }
        return stackToAdd;
    }

    private static ItemStack executeFillSingleSlot(Supplier<ItemStack> getSlot, Consumer<ItemStack> setSlot, ItemStack stackToAdd) {
        ItemStack current = getSlot.get();
        int max = stackToAdd.getMaxStackSize();
        if (current.isEmpty()) {
            int toPut = Math.min(stackToAdd.getCount(), max);
            setSlot.accept(stackToAdd.copyWithCount(toPut));
            return stackToAdd.getCount() > toPut ? stackToAdd.copyWithCount(stackToAdd.getCount() - toPut) : ItemStack.EMPTY;
        }
        if (ItemStack.isSameItemSameComponents(current, stackToAdd)) {
            int space = max - current.getCount();
            int toGrow = Math.min(stackToAdd.getCount(), space);
            current.grow(toGrow);
            return stackToAdd.getCount() > toGrow ? stackToAdd.copyWithCount(stackToAdd.getCount() - toGrow) : ItemStack.EMPTY;
        }
        return stackToAdd;
    }
}