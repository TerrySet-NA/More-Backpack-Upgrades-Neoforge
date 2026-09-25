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
import com.kzjy.mobackup.util.RSRoutingHelper;
import com.kzjy.mobackup.util.SafetyRollbackHelper;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
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
    private BlockPos currentBlockPos = null;

    public DimensionalAlchemyUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    @Override
    public void save() {
        super.save();
    }

    @Override
    public boolean getDefaultNetworkFirst() {
        return true;
    }

    @Override
    public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (level.isClientSide() || nextCheckTime > level.getGameTime()) {
            return;
        }

        currentBlockPos = pos.immutable();

        Player actionPlayer = entity instanceof Player p ? p : null;

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

        if (entity instanceof LivingEntity livingEntity) {
            applyTo(livingEntity, level, actionPlayer);
        } else {
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(CHECK_RADIUS), this::entityMatches);
            for (LivingEntity livingEntity : entities) {
                applyTo(livingEntity, level, null);
                if (applying) break;
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

    private void handleRemainingStack(Level level, ItemStack remainingStack, @Nullable Player refundTargetPlayer, @Nullable Player actionPlayer) {
        if (remainingStack.isEmpty()) return;

        Network network = RSBridge.getNetwork(level, getUpgradeStack(), actionPlayer, this.currentBlockPos, BuiltinPermission.INSERT);
        var backpackInv = storageWrapper.getInventoryForUpgradeProcessing();

        if (isNetworkFirst()) {
            if (network != null) remainingStack = RSRoutingHelper.insertIntoRs(network, remainingStack, false, actionPlayer);
            if (!remainingStack.isEmpty()) remainingStack = backpackInv.insertItem(remainingStack, false);
        } else {
            remainingStack = backpackInv.insertItem(remainingStack, false);
            if (!remainingStack.isEmpty() && network != null) {
                remainingStack = RSRoutingHelper.insertIntoRs(network, remainingStack, false, actionPlayer);
            }
        }

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

        if (isNetworkFirst()) {
            ItemStack extracted = extractFromRsNetwork(level, matcher, actionPlayer);
            if (!extracted.isEmpty()) {
                if (startApplying(extracted, def, livingEntity, actionPlayer)) {
                    return true;
                } else {
                    handleRemainingStack(level, extracted, refundTargetPlayer, actionPlayer);
                }
            }
            return consumeFromBackpack(livingEntity, def, matcher, level, refundTargetPlayer, actionPlayer);
        } else {
            if (consumeFromBackpack(livingEntity, def, matcher, level, refundTargetPlayer, actionPlayer)) {
                return true;
            }
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

    private ItemStack extractFromRsNetwork(Level level, Predicate<ItemStack> matcher, @Nullable Player actionPlayer) {
        Network network = RSBridge.getNetwork(level, getUpgradeStack(), actionPlayer, this.currentBlockPos, BuiltinPermission.EXTRACT);
        if (network == null) return ItemStack.EMPTY;

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) return ItemStack.EMPTY;

        Actor actor = RSBridge.getActor(actionPlayer);
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

    @Override
    public void triggerItemUseEffects(Level level) {
        if (applyingToEntity == null || stackBeingApplied.isEmpty()) return;

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
        return applyTimePassed > effectDelay && remainingApplyTime % 4 == 0;
    }

    private boolean entityMatches(LivingEntity livingEntity) {
        if (!livingEntity.isAlive()) return false;
        return switch (getEntityMatch()) {
            case PLAYERS -> (livingEntity instanceof Player);
            case ENTITIES -> !(livingEntity instanceof Player);
            case PLAYERS_AND_ENTITIES -> true;
        };
    }
}