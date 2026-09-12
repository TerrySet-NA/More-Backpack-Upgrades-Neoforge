/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.wrapper;

// import com.kzjy.mobackup.MoBackup;
import com.kzjy.mobackup.core.RSBridge;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
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
// import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeItem;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class DimensionalRefillUpgradeWrapper extends RefillUpgradeWrapper {
    private static final int REFILL_RANGE = 3;
    private static final int COOLDOWN = 5;

    private Network cachedNetwork;
    private long lastNetworkCheckTime = -1;
    private static final int NETWORK_CHECK_INTERVAL = 20;

    public DimensionalRefillUpgradeWrapper(IStorageWrapper backpackWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(backpackWrapper, upgrade, upgradeSaveHandler);
    }

    private Network getCachedNetwork(Level level) {
        long gameTime = level.getGameTime();
        if (cachedNetwork == null || lastNetworkCheckTime < 0
                || gameTime - lastNetworkCheckTime >= NETWORK_CHECK_INTERVAL) {
            lastNetworkCheckTime = gameTime;
            cachedNetwork = RSBridge.getNetwork(level, getUpgradeStack());
            // MoBackup.LOGGER.info("[MoBackup-Debug] 補貨卡取得 RS 網路實例 -> {}", (cachedNetwork != null ? "【成功】" : "【失敗: null】"));
        }
        return cachedNetwork;
    }

    @Override
    public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (isInCooldown(level)) {
            return;
        }

        if (!(entity instanceof Player)) {
            level.getEntities(EntityType.PLAYER, new AABB(pos).inflate(REFILL_RANGE), p -> true).forEach(this::refillItemForCustom);
        } else {
            refillItemForCustom(entity);
        }
        setCooldown(level, COOLDOWN);
    }

    private void refillItemForCustom(Entity entity) {
        CapabilityHelper.runOnItemHandler(entity, playerInvHandler -> InventoryHelper.iterate(getFilterLogic().getFilterHandler(), (slot, filter) -> {
            if (filter.isEmpty()) {
                return;
            }
            tryRefillFilterCustom(entity, playerInvHandler, filter, getTargetSlots().getOrDefault(slot, TargetSlot.ANY));
        }));
    }

    private void tryRefillFilterCustom(@Nonnull Entity entity, IItemHandler playerInvHandler, ItemStack filter, TargetSlot targetSlot) {
        if (!(entity instanceof Player player)) {
            return;
        }

        int missingCount = getMissingCountCustom(targetSlot, player, playerInvHandler, filter);
        if (ItemStack.isSameItemSameComponents(player.containerMenu.getCarried(), filter)) {
            missingCount -= Math.min(missingCount, player.containerMenu.getCarried().getCount());
        }
        if (missingCount <= 0) {
            return;
        }

        // 1. 優先嘗試自 RS 2.x 網路提取物資進行補貨
        Network network = getCachedNetwork(player.level());
        if (network != null) {
            StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
            if (storage != null) {
                ItemResource resource = ItemResource.ofItemStack(filter);
                Actor actor = new PlayerActor(player);

                long simExtracted = storage.extract(resource, missingCount, Action.SIMULATE, actor);
                if (simExtracted > 0) {
                    ItemStack toFill = filter.copyWithCount((int) simExtracted);
                    ItemStack remaining = fillTargetSlotCustom(targetSlot, player, playerInvHandler, toFill);
                    int actuallyFilled = (int) simExtracted - remaining.getCount();

                    if (actuallyFilled > 0) {
                        storage.extract(resource, actuallyFilled, Action.EXECUTE, actor);
                        missingCount -= actuallyFilled;
                        // MoBackup.LOGGER.info("[MoBackup-Debug] 次元補貨 -> 成功從 RS 網路補給 {} x{}", filter.getHoverName().getString(), actuallyFilled);
                    }
                }
            }
        }

        // 2. 剩餘未補齊的數量，降級由背包庫存補齊
        if (missingCount > 0) {
            IItemHandler extractFromHandler = storageWrapper.getInventoryForUpgradeProcessing();
            ItemStack toMove = filter.copyWithCount(missingCount);
            ItemStack extracted = InventoryHelper.extractFromInventory(toMove, extractFromHandler, true);
            if (!extracted.isEmpty()) {
                ItemStack remaining = fillTargetSlotCustom(targetSlot, player, playerInvHandler, extracted);
                if (remaining.getCount() != extracted.getCount()) {
                    ItemStack toExtract = extracted.copyWithCount(extracted.getCount() - remaining.getCount());
                    InventoryHelper.extractFromInventory(toExtract, extractFromHandler, false);
                }
            }
        }
    }

    @Override
    public boolean pickBlock(Player player, ItemStack filter) {
        if (!upgradeItem.supportsBlockPick()) {
            return false;
        }

        // 優先檢查 RS 網路是否擁有目標方塊
        Network network = getCachedNetwork(player.level());
        if (network != null) {
            StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
            if (storage != null) {
                ItemResource resource = ItemResource.ofItemStack(filter);
                Actor actor = new PlayerActor(player);

                // 直接使用 Action.SIMULATE 模擬提取，若 RS 沒有該物品會回傳 0
                long simExtracted = storage.extract(resource, filter.getMaxStackSize(), Action.SIMULATE, actor);
                if (simExtracted > 0) {
                    player.getInventory().selected = player.getInventory().getSuitableHotbarSlot();
                    ItemStack selectedItem = player.getInventory().getSelected();

                    boolean storedSelected = false;
                    if (selectedItem.isEmpty()) {
                        storedSelected = true;
                    } else {
                        // 嘗試將手上原有物品回存 RS 網路
                        ItemResource selectedRes = ItemResource.ofItemStack(selectedItem);
                        long simInsert = storage.insert(selectedRes, selectedItem.getCount(), Action.SIMULATE, actor);
                        if (simInsert >= selectedItem.getCount()) {
                            storage.insert(selectedRes, selectedItem.getCount(), Action.EXECUTE, actor);
                            storedSelected = true;
                        } else if (canMoveSelectedToInventoryCustom(player)) {
                            player.getInventory().add(selectedItem);
                            storedSelected = true;
                        }
                    }

                    if (storedSelected) {
                        storage.extract(resource, simExtracted, Action.EXECUTE, actor);
                        ItemStack extracted = filter.copyWithCount((int) simExtracted);
                        player.setItemInHand(InteractionHand.MAIN_HAND, extracted);
                        // MoBackup.LOGGER.info("[MoBackup-Debug] 次元補貨 pickBlock -> 成功從 RS 提取方塊至手持: {}", extracted.getHoverName().getString());
                        return true;
                    }
                }
            }
        }

        // 降級由背包原生 pickBlock 處理
        return super.pickBlock(player, filter);
    }

    private boolean canMoveSelectedToInventoryCustom(Player player) {
        int countToAdd = player.getMainHandItem().getCount();
        for (int slot = 0; slot < player.getInventory().getContainerSize() - 5; slot++) {
            if (slot == player.getInventory().selected) {
                continue;
            }
            ItemStack slotStack = player.getInventory().getItem(slot);
            if (slotStack.isEmpty()) {
                return true;
            } else if (ItemStack.isSameItemSameComponents(slotStack, player.getMainHandItem())) {
                countToAdd -= (slotStack.getMaxStackSize() - slotStack.getCount());
                if (countToAdd <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- 內部槽位分發器 (避開 TargetSlot private 限制) ---

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

    private ItemStack fillTargetSlotCustom(TargetSlot slot, Player player, IItemHandler playerInvHandler, ItemStack stackToAdd) {
        if (slot == TargetSlot.ANY) {
            return refillAnywhereInInventoryCustom(playerInvHandler, stackToAdd);
        } else if (slot == TargetSlot.MAIN_HAND) {
            return refillSlotCustom(player::getMainHandItem, stackToAdd, s -> player.setItemInHand(InteractionHand.MAIN_HAND, s));
        } else if (slot == TargetSlot.OFF_HAND) {
            return refillSlotCustom(player::getOffhandItem, stackToAdd, s -> player.setItemInHand(InteractionHand.OFF_HAND, s));
        } else {
            int toolbarIndex = slot.ordinal() - TargetSlot.TOOLBAR_1.ordinal();
            if (toolbarIndex >= 0 && toolbarIndex < 9) {
                return refillSlotCustom(() -> player.getInventory().getItem(toolbarIndex), stackToAdd, s -> player.getInventory().setItem(toolbarIndex, s));
            }
        }
        return stackToAdd;
    }

    private static ItemStack refillAnywhereInInventoryCustom(IItemHandler playerInvHandler, ItemStack extracted) {
        AtomicReference<ItemStack> remainingStack = new AtomicReference<>(extracted);
        InventoryHelper.iterate(playerInvHandler, (slot, stack) -> {
            if (ItemStack.isSameItemSameComponents(stack, remainingStack.get())) {
                remainingStack.set(playerInvHandler.insertItem(slot, remainingStack.get(), false));
            }
        }, () -> remainingStack.get().isEmpty());

        ItemStack remaining = remainingStack.get();
        if (!remaining.isEmpty()) {
            ItemStack afterInsert = InventoryHelper.insertIntoInventory(remaining, playerInvHandler, true);
            if (afterInsert.getCount() == remaining.getCount()) {
                return remaining;
            }
            ItemStack toInsert = remaining.copy();
            toInsert.setCount(remaining.getCount() - afterInsert.getCount());
            return InventoryHelper.insertIntoInventory(toInsert, playerInvHandler, false);
        }
        return remaining;
    }

    private static ItemStack refillSlotCustom(Supplier<ItemStack> getSlotContents, ItemStack stackToAdd, Consumer<ItemStack> setSlotContents) {
        ItemStack contents = getSlotContents.get();
        if (contents.isEmpty()) {
            setSlotContents.accept(stackToAdd);
            return ItemStack.EMPTY;
        }
        if (ItemStack.isSameItemSameComponents(contents, stackToAdd)) {
            contents.grow(stackToAdd.getCount());
            return ItemStack.EMPTY;
        }
        return stackToAdd;
    }
}