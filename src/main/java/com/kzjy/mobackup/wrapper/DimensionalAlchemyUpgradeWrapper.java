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
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.mixin.AlchemyUpgradeWrapperAccessor;
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

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyFilterAttribute;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper;

public class DimensionalAlchemyUpgradeWrapper extends AlchemyUpgradeWrapper implements IPriorityRoutingUpgrade {

    private static final int CHECK_INTERVAL = 5;
    private static final int CHECK_RADIUS = 3;

    private long nextCheckTime = 0;
    private boolean applying = false;
    private LivingEntity applyingToEntity = null;
    private int remainingApplyTime = 0;
    private ItemStack stackBeingApplied = ItemStack.EMPTY;
    private AlchemyItemDefinition defBeingApplied = null;
    private @Nullable Player actionPlayerBeingApplied = null;

    public DimensionalAlchemyUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    // =========================================================================
    // 優先級狀態持久化
    // =========================================================================

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

    // =========================================================================
    // 主 Tick 調度與施藥狀態機
    // =========================================================================

    @Override
    public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (level.isClientSide() || nextCheckTime > level.getGameTime()) {
            return;
        }

        // 規則 3: 若背包在地上 (entity 不是 Player)，actionPlayer 嚴格為 null
        Player actionPlayer = entity instanceof Player p ? p : null;

        // 處理施用倒數狀態
        if (remainingApplyTime > 0) {
            if (applyingToEntity == null || !applyingToEntity.isAlive() || applyingToEntity.isRemoved()) {
                abortAndRefund(level);
                return;
            }

            remainingApplyTime--;
            if (remainingApplyTime <= 0) {
                if (defBeingApplied != null && defBeingApplied.hasItemUseEffects()) {
                    triggerItemUseEffects(level);
                }
                applying = false;

                ItemStack remainingStack = defBeingApplied != null && applyingToEntity != null
                        ? defBeingApplied.finishUsing().apply(stackBeingApplied, applyingToEntity)
                        : ItemStack.EMPTY;

                Player refundTargetPlayer = applyingToEntity instanceof Player p ? p : actionPlayerBeingApplied;
                Player rsActor = actionPlayerBeingApplied;

                stackBeingApplied = ItemStack.EMPTY;
                applyingToEntity = null;
                defBeingApplied = null;
                actionPlayerBeingApplied = null;

                if (!remainingStack.isEmpty()) {
                    handleRemainingStack(level, remainingStack, refundTargetPlayer, rsActor);
                }

                nextCheckTime = level.getGameTime() + CHECK_INTERVAL;
            } else if (shouldTriggerItemUseEffects()) {
                triggerItemUseEffects(level);
            }
            return;
        }

        // 搜尋符合條件之實體並施藥
        if (entity instanceof LivingEntity livingEntity) {
            applyTo(livingEntity, level, actionPlayer);
        } else {
            // 地上背包模式：actionPlayer 嚴格為 null，不拿周圍實體當作 RS 操作身分
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(CHECK_RADIUS), this::entityMatches);
            for (LivingEntity livingEntity : entities) {
                applyTo(livingEntity, level, null);
                if (applying) {
                    break;
                }
            }
        }

        if (!applying) {
            nextCheckTime = level.getGameTime() + CHECK_INTERVAL;
        }
    }

    private void abortAndRefund(Level level) {
        Player refundTargetPlayer = applyingToEntity instanceof Player p ? p : actionPlayerBeingApplied;
        Player rsActor = actionPlayerBeingApplied;

        applying = false;
        remainingApplyTime = 0;
        applyingToEntity = null;
        defBeingApplied = null;
        actionPlayerBeingApplied = null;

        if (!stackBeingApplied.isEmpty()) {
            ItemStack toRefund = stackBeingApplied.copy();
            stackBeingApplied = ItemStack.EMPTY;
            handleRemainingStack(level, toRefund, refundTargetPlayer, rsActor);
        }
        nextCheckTime = level.getGameTime() + CHECK_INTERVAL;
    }

    /**
     * 容器殘留物（空瓶、碗等）依優先級回存
     */
    private void handleRemainingStack(Level level, ItemStack remainingStack, @Nullable Player refundTargetPlayer, @Nullable Player actionPlayer) {
        if (remainingStack.isEmpty()) {
            return;
        }

        if (isNetworkFirst()) {
            remainingStack = insertIntoRsNetwork(level, remainingStack, actionPlayer);
            if (!remainingStack.isEmpty()) {
                remainingStack = storageWrapper.getInventoryForUpgradeProcessing().insertItem(remainingStack, false);
            }
        } else {
            remainingStack = storageWrapper.getInventoryForUpgradeProcessing().insertItem(remainingStack, false);
            if (!remainingStack.isEmpty()) {
                remainingStack = insertIntoRsNetwork(level, remainingStack, actionPlayer);
            }
        }

        // 規則 11: 終極防吞兜底給接收效果的玩家
        if (!remainingStack.isEmpty()) {
            SafetyRollbackHelper.fallbackToBackpackOrPlayer(remainingStack, storageWrapper, refundTargetPlayer);
        }
    }

    private void applyTo(LivingEntity livingEntity, Level level, @Nullable Player actionPlayer) {
        List<AlchemyFilterAttribute> attributes = getFilterAttributes();

        for (AlchemyFilterAttribute filterAttribute : attributes) {
            if (!filterAttribute.filter().isEmpty() && filterAttribute.condition().test(livingEntity, filterAttribute.value())) {
                for (AlchemyItemDefinition def : AlchemyUpgradeWrapperAccessor.getItemDefinitions()) {
                    if (def.filter().test(filterAttribute.filter())
                            && def.canApply().test(livingEntity, filterAttribute.filter(), shouldMatchAllEffects(), shouldMatchEffectAmplifier())) {
                        if (tryConsumeItem(livingEntity, filterAttribute, def, level, actionPlayer)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean tryConsumeItem(LivingEntity livingEntity, AlchemyFilterAttribute filterAttribute,
                                   AlchemyItemDefinition def, Level level, @Nullable Player actionPlayer) {
        Predicate<ItemStack> matcher = stack -> def.filter().test(stack)
                && def.stackMatches().test(stack, filterAttribute.filter(), shouldMatchAllEffects(), shouldMatchEffectDuration(), shouldMatchEffectAmplifier());

        Player refundTargetPlayer = livingEntity instanceof Player p ? p : actionPlayer;

        // 規則 10: 非 XOR 升級，優先級不足/失敗時切換另一端
        if (isNetworkFirst()) {
            // 1. 先查 RS 網路
            ItemStack extracted = extractFromRsNetwork(level, matcher, actionPlayer);
            if (!extracted.isEmpty()) {
                if (startApplying(extracted, def, livingEntity, actionPlayer)) {
                    return true;
                } else {
                    handleRemainingStack(level, extracted, refundTargetPlayer, actionPlayer);
                }
            }
            // 2. 降級查隨身背包
            return consumeFromBackpack(livingEntity, def, matcher, level, refundTargetPlayer, actionPlayer);
        } else {
            // 1. 先查隨身背包
            if (consumeFromBackpack(livingEntity, def, matcher, level, refundTargetPlayer, actionPlayer)) {
                return true;
            }
            // 2. 降級向 RS 網路調取
            ItemStack extracted = extractFromRsNetwork(level, matcher, actionPlayer);
            if (!extracted.isEmpty()) {
                if (startApplying(extracted, def, livingEntity, actionPlayer)) {
                    return true;
                } else {
                    handleRemainingStack(level, extracted, refundTargetPlayer, actionPlayer);
                }
            }
            return false;
        }
    }

    private boolean consumeFromBackpack(LivingEntity livingEntity, AlchemyItemDefinition def, Predicate<ItemStack> matcher,
                                        Level level, @Nullable Player refundTargetPlayer, @Nullable Player actionPlayer) {
        ITrackedContentsItemHandler inv = storageWrapper.getInventoryForUpgradeProcessing();
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (!stack.isEmpty() && matcher.test(stack)) {
                ItemStack extracted = inv.extractItem(slot, 1, false);
                if (!extracted.isEmpty()) {
                    if (startApplying(extracted, def, livingEntity, actionPlayer)) {
                        return true;
                    } else {
                        handleRemainingStack(level, extracted, refundTargetPlayer, actionPlayer);
                    }
                }
            }
        }
        return false;
    }

    private boolean startApplying(ItemStack stack, AlchemyItemDefinition def, LivingEntity livingEntity, @Nullable Player actionPlayer) {
        int duration = def.startUsing().applyAsInt(stack, livingEntity);
        if (duration > 0) {
            remainingApplyTime = duration;
            applying = true;
            stackBeingApplied = stack.copyWithCount(1);
            defBeingApplied = def;
            applyingToEntity = livingEntity;
            actionPlayerBeingApplied = actionPlayer;
            return true;
        }
        return false;
    }

    /**
     * 從 RS 網路提取符合過濾條件的藥水
     */
    private ItemStack extractFromRsNetwork(Level level, Predicate<ItemStack> matcher, @Nullable Player actionPlayer) {
        Network network = RSBridge.getNetwork(level, getUpgradeStack(), actionPlayer, BuiltinPermission.EXTRACT);
        if (network == null) {
            return ItemStack.EMPTY;
        }

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) {
            return ItemStack.EMPTY;
        }

        // 規則 1: actionPlayer 為 null 時使用 Actor.EMPTY
        Actor actor = actionPlayer != null ? new PlayerActor(actionPlayer) : Actor.EMPTY;
        ItemResource matchedResource = null;

        for (ResourceAmount ra : new ArrayList<>(storage.getAll())) {
            if (ra.amount() > 0 && ra.resource() instanceof ItemResource ir) {
                ItemStack testStack = ir.toItemStack(1);
                if (matcher.test(testStack)) {
                    matchedResource = ir;
                    break;
                }
            }
        }

        if (matchedResource != null) {
            long extracted = storage.extract(matchedResource, 1, Action.EXECUTE, actor);
            if (extracted > 0) {
                return matchedResource.toItemStack((int) extracted);
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 回存容器殘留物至 RS
     */
    private ItemStack insertIntoRsNetwork(Level level, ItemStack stack, @Nullable Player actionPlayer) {
        if (stack.isEmpty()) {
            return stack;
        }

        Network network = RSBridge.getNetwork(level, getUpgradeStack(), actionPlayer, BuiltinPermission.INSERT);
        if (network == null) {
            return stack;
        }

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) {
            return stack;
        }

        ItemResource res = ItemResource.ofItemStack(stack);
        Actor actor = actionPlayer != null ? new PlayerActor(actionPlayer) : Actor.EMPTY;
        long inserted = storage.insert(res, stack.getCount(), Action.EXECUTE, actor);
        if (inserted <= 0) {
            return stack;
        }

        int rem = stack.getCount() - (int) inserted;
        return rem <= 0 ? ItemStack.EMPTY : stack.copyWithCount(rem);
    }

    @Override
    public void triggerItemUseEffects(Level level) {
        if (applyingToEntity == null || stackBeingApplied.isEmpty()) {
            return;
        }
        if (stackBeingApplied.getUseAnimation() == UseAnim.DRINK) {
            level.playSound(null, applyingToEntity.getX(), applyingToEntity.getY(), applyingToEntity.getZ(),
                    stackBeingApplied.getDrinkingSound(), applyingToEntity.getSoundSource(), 0.5F,
                    level.random.nextFloat() * 0.1F + 0.9F);
        } else if (stackBeingApplied.getUseAnimation() == UseAnim.EAT) {
            level.playSound(null, applyingToEntity.getX(), applyingToEntity.getY(), applyingToEntity.getZ(),
                    applyingToEntity.getEatingSound(stackBeingApplied), applyingToEntity.getSoundSource(),
                    0.5F + 0.5F * level.random.nextInt(2),
                    (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
        }
    }

    private boolean shouldTriggerItemUseEffects() {
        if (remainingApplyTime < 2 || applyingToEntity == null || stackBeingApplied.isEmpty()) {
            return false;
        }
        int applyTimePassed = stackBeingApplied.getUseDuration(applyingToEntity) - remainingApplyTime;
        int effectDelay = (int) (stackBeingApplied.getUseDuration(applyingToEntity) * 0.21875F);
        boolean canStartTriggering = applyTimePassed > effectDelay;
        return canStartTriggering && remainingApplyTime % 4 == 0;
    }

    private boolean entityMatches(LivingEntity livingEntity) {
        if (!livingEntity.isAlive()) {
            return false;
        }
        return switch (getEntityMatch()) {
            case PLAYERS -> (livingEntity instanceof Player);
            case ENTITIES -> !(livingEntity instanceof Player);
            case PLAYERS_AND_ENTITIES -> true;
        };
    }
}