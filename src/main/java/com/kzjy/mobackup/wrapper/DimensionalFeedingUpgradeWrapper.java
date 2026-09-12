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
// import com.mojang.logging.LogUtils;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.EventHooks;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.HungerLevel;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
// import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@SuppressWarnings("null")
public class DimensionalFeedingUpgradeWrapper extends FeedingUpgradeWrapper {
    private static final int COOLDOWN = 100;
    private static final int STILL_HUNGRY_COOLDOWN = 10;
    private static final int FEEDING_RANGE = 3;

    private Network cachedNetwork;
    private long lastNetworkCheckTime = -1;
    private static final int NETWORK_CHECK_INTERVAL = 20;

    public DimensionalFeedingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
                                           Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    private Network getCachedNetwork(Level level) {
        long gameTime = level.getGameTime();
        if (cachedNetwork == null || lastNetworkCheckTime < 0
                || gameTime - lastNetworkCheckTime >= NETWORK_CHECK_INTERVAL) {
            lastNetworkCheckTime = gameTime;
            cachedNetwork = RSBridge.getNetwork(level, getUpgradeStack());
            // MoBackup.LOGGER.info("[MoBackup-Debug] 餵食卡取得 RS 網路實例 -> {}", (cachedNetwork != null ? "【成功】" : "【失敗: null】"));
        }
        return cachedNetwork;
    }

    @Override
    public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (isInCooldown(level)) {
            return;
        }

        boolean hungryPlayer = false;
        if (!(entity instanceof Player)) {
            AtomicBoolean stillHungryPlayer = new AtomicBoolean(false);
            level.getEntities(EntityType.PLAYER, new AABB(pos).inflate(FEEDING_RANGE), p -> true)
                    .forEach(p -> stillHungryPlayer.set(stillHungryPlayer.get() || feedPlayerAndGetHungryCustom(p, level)));
            hungryPlayer = stillHungryPlayer.get();
        } else {
            if (feedPlayerAndGetHungryCustom((Player) entity, level)) {
                hungryPlayer = true;
            }
        }

        if (hungryPlayer) {
            setCooldown(level, STILL_HUNGRY_COOLDOWN);
            return;
        }

        setCooldown(level, COOLDOWN);
    }

    private boolean feedPlayerAndGetHungryCustom(Player player, Level level) {
        int hungerLevel = 20 - player.getFoodData().getFoodLevel();
        if (hungerLevel == 0) {
            return false;
        }

        // 1. 優先嘗試從 RS 網路提取食物餵食
        boolean fed = tryFeedingFromRsNetwork(level, hungerLevel, player);

        // 2. RS 網路未連線或無合適食物，降級從背包本體儲存餵食
        if (!fed) {
            fed = tryFeedingFromBackpackStorage(level, hungerLevel, player);
        }

        return fed && player.getFoodData().getFoodLevel() < 20;
    }

    /**
     * 從 RS 2.x 網路尋找並食用食物
     */
    private boolean tryFeedingFromRsNetwork(Level level, int hungerLevel, Player player) {
        Network network = getCachedNetwork(level);
        if (network == null) {
            return false;
        }

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) {
            return false;
        }

        boolean isHurt = player.getHealth() < player.getMaxHealth() - 0.1F;
        Actor actor = new PlayerActor(player);

        for (ResourceAmount resourceAmount : storage.getAll()) {
            if (resourceAmount.amount() <= 0) {
                continue;
            }

            if (resourceAmount.resource() instanceof ItemResource itemResource) {
                ItemStack candidate = itemResource.toItemStack(1);

                if (isEdibleCustom(candidate, player) && getFilterLogic().matchesFilter(candidate)
                        && (isHungryEnoughForFoodCustom(hungerLevel, candidate, player) || shouldFeedImmediatelyWhenHurt() && hungerLevel > 0 && isHurt)) {

                    // 1. 模擬提取
                    long simExtracted = storage.extract(itemResource, 1, Action.SIMULATE, actor);
                    if (simExtracted < 1) {
                        continue;
                    }

                    // 2. 執行食用
                    boolean consumed = executeFeedPlayer(level, player, candidate, () -> {
                        storage.extract(itemResource, 1, Action.EXECUTE, actor);
                        // MoBackup.LOGGER.info("[MoBackup-Debug] 次元餵食 -> 成功從 RS 網路提取食物供玩家食用 ({})", candidate.getHoverName().getString());
                    }, null);

                    if (consumed) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 降級備援：從精妙背包自身儲存餵食
     */
    private boolean tryFeedingFromBackpackStorage(Level level, int hungerLevel, Player player) {
        ITrackedContentsItemHandler inventory = storageWrapper.getInventoryForUpgradeProcessing();
        return InventoryHelper.iterate(inventory, (slot, stack) -> {
            boolean isHurt = player.getHealth() < player.getMaxHealth() - 0.1F;
            if (isEdibleCustom(stack, player) && getFilterLogic().matchesFilter(stack)
                    && (isHungryEnoughForFoodCustom(hungerLevel, stack, player) || shouldFeedImmediatelyWhenHurt() && hungerLevel > 0 && isHurt)) {

                return executeFeedPlayer(level, player, stack, () -> {
                    stack.shrink(1);
                    inventory.setStackInSlot(slot, stack);
                }, inventory);
            }
            return false;
        }, () -> false, ret -> ret);
    }

    /**
     * 玩家食用處理器與餐具（空碗、空瓶）回收邏輯
     */
    private boolean executeFeedPlayer(Level level, Player player, ItemStack foodStack, Runnable onConsumed, @Nullable ITrackedContentsItemHandler inventory) {
        ItemStack mainHandItem = player.getMainHandItem();
        player.getInventory().items.set(player.getInventory().selected, foodStack);

        ItemStack singleItemCopy = foodStack.copy();
        singleItemCopy.setCount(1);

        if (singleItemCopy.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME) {
            onConsumed.run();

            ItemStack resultItem = EventHooks.onItemUseFinish(player, singleItemCopy.copy(), 0,
                    singleItemCopy.getItem().finishUsingItem(singleItemCopy, level, player));

            // 如果食用後留下了殘渣（例如喝完蜂蜜的玻璃瓶、吃完燉湯的碗）
            if (!resultItem.isEmpty()) {
                handleLeftovers(resultItem, player, level, inventory);
            }

            player.getInventory().items.set(player.getInventory().selected, mainHandItem);
            return true;
        }

        player.getInventory().items.set(player.getInventory().selected, mainHandItem);
        return false;
    }

    /**
     * 餐具回收：優先送回 RS 網路，塞不下則送入背包，背包滿了再給玩家/掉落
     */
    private void handleLeftovers(ItemStack leftover, Player player, Level level, @Nullable ITrackedContentsItemHandler inventory) {
        Network network = getCachedNetwork(level);
        if (network != null) {
            StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
            if (storage != null) {
                ItemResource resource = ItemResource.ofItemStack(leftover);
                long inserted = storage.insert(resource, leftover.getCount(), Action.EXECUTE, new PlayerActor(player));
                if (inserted >= leftover.getCount()) {
                    return; // 100% 成功送回 RS 網路
                }
                leftover = leftover.copyWithCount((int) (leftover.getCount() - inserted));
            }
        }

        ITrackedContentsItemHandler targetInventory = inventory != null ? inventory : storageWrapper.getInventoryForUpgradeProcessing();
        ItemStack remaining = targetInventory.insertItem(leftover, false);

        if (!remaining.isEmpty()) {
            CapabilityHelper.runOnCapability(player, Capabilities.ItemHandler.ENTITY, null,
                    playerInventory -> InventoryHelper.insertOrDropItem(player, remaining, playerInventory));
        }
    }

    private static boolean isEdibleCustom(ItemStack stack, LivingEntity player) {
        if (stack.getItem() == Items.OMINOUS_BOTTLE) {
            return false;
        }
        FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, player);
        return foodProperties != null && foodProperties.nutrition() >= 1;
    }

    private boolean isHungryEnoughForFoodCustom(int hungerLevel, ItemStack stack, Player player) {
        FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, player);
        if (foodProperties == null) {
            return false;
        }

        HungerLevel feedAtHungerLevel = getFeedAtHungerLevel();
        if (feedAtHungerLevel == HungerLevel.ANY) {
            return true;
        }

        int nutrition = foodProperties.nutrition();
        return (feedAtHungerLevel == HungerLevel.HALF ? (nutrition / 2) : nutrition) <= hungerLevel;
    }
}