/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.wrapper;

import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.mixin.AlchemyUpgradeWrapperAccessor;
import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyFilterAttribute;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("null")
public class DimensionalAlchemyUpgradeWrapper extends AlchemyUpgradeWrapper implements IPriorityRoutingUpgrade {

    private static final int CHECK_INTERVAL = 5;
    private static final int CHECK_RADIUS = 3;

    private long nextCheckTime = 0;
    private boolean applying = false;
    private LivingEntity applyingToEntity = null;
    private int remainingApplyTime = 0;
    private ItemStack stackBeingApplied = ItemStack.EMPTY;
    private AlchemyItemDefinition defBeingApplied = null;

    public DimensionalAlchemyUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    private Network cachedNetwork;
    private long lastNetworkCheckTime = -1;
    private static final int NETWORK_CHECK_INTERVAL = 20;

    private Network getCachedNetwork(Level level) {
        long gameTime = level.getGameTime();
        if (cachedNetwork == null || lastNetworkCheckTime < 0
                || gameTime - lastNetworkCheckTime >= NETWORK_CHECK_INTERVAL) {
            lastNetworkCheckTime = gameTime;
            cachedNetwork = RSBridge.getNetwork(level, getUpgradeStack());
        }
        return cachedNetwork;
    }

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

    @Override
    public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (level.isClientSide() || nextCheckTime > level.getGameTime()) {
            return;
        }

        if (remainingApplyTime > 0) {
            remainingApplyTime--;
            if (remainingApplyTime <= 0) {
                if (defBeingApplied != null && defBeingApplied.hasItemUseEffects()) {
                    triggerItemUseEffects(level);
                }
                applying = false;
                ItemStack remainingStack = defBeingApplied != null && applyingToEntity != null
                        ? defBeingApplied.finishUsing().apply(stackBeingApplied, applyingToEntity)
                        : ItemStack.EMPTY;

                stackBeingApplied = ItemStack.EMPTY;
                applyingToEntity = null;
                defBeingApplied = null;

                if (!remainingStack.isEmpty()) {
                    handleRemainingStack(level, remainingStack, entity instanceof Player p ? p : null);
                }

                nextCheckTime = level.getGameTime() + CHECK_INTERVAL;
            } else if (shouldTriggerItemUseEffects()) {
                triggerItemUseEffects(level);
            }
            return;
        }

        if (entity instanceof LivingEntity livingEntity) {
            applyTo(livingEntity, level);
        } else {
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(CHECK_RADIUS), this::entityMatches);
            for (LivingEntity livingEntity : entities) {
                applyTo(livingEntity, level);
                if (applying) {
                    break;
                }
            }
        }

        if (!applying) {
            nextCheckTime = level.getGameTime() + CHECK_INTERVAL;
        }
    }

    private void handleRemainingStack(Level level, ItemStack remainingStack, @Nullable Player player) {
        if (isNetworkFirst()) {
            Network network = getCachedNetwork(level);
            if (network != null) {
                remainingStack = insertIntoRsNetwork(network, remainingStack, player);
            }
            if (!remainingStack.isEmpty()) {
                storageWrapper.getInventoryForUpgradeProcessing().insertItem(remainingStack, false);
            }
        } else {
            remainingStack = storageWrapper.getInventoryForUpgradeProcessing().insertItem(remainingStack, false);
            if (!remainingStack.isEmpty()) {
                Network network = getCachedNetwork(level);
                if (network != null) {
                    insertIntoRsNetwork(network, remainingStack, player);
                }
            }
        }
    }

    private void applyTo(LivingEntity livingEntity, Level level) {
        Player player = livingEntity instanceof Player p ? p : null;
        List<AlchemyFilterAttribute> attributes = getFilterAttributes();

        for (AlchemyFilterAttribute filterAttribute : attributes) {
            if (!filterAttribute.filter().isEmpty() && filterAttribute.condition().test(livingEntity, filterAttribute.value())) {
                for (AlchemyItemDefinition def : AlchemyUpgradeWrapperAccessor.getItemDefinitions()) {
                    if (def.filter().test(filterAttribute.filter())
                            && def.canApply().test(livingEntity, filterAttribute.filter(), shouldMatchAllEffects(), shouldMatchEffectAmplifier())) {
                        if (tryConsumeItem(livingEntity, filterAttribute, def, level, player)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean tryConsumeItem(LivingEntity livingEntity, AlchemyFilterAttribute filterAttribute,
                                   AlchemyItemDefinition def, Level level, @Nullable Player player) {
        Predicate<ItemStack> matcher = stack -> def.filter().test(stack)
                && def.stackMatches().test(stack, filterAttribute.filter(), shouldMatchAllEffects(), shouldMatchEffectDuration(), shouldMatchEffectAmplifier());

        if (isNetworkFirst()) {
            // 1. 先查 RS 網路
            Network network = getCachedNetwork(level);
            if (network != null) {
                ItemStack extracted = extractFromRsNetwork(network, matcher, player);
                if (!extracted.isEmpty()) {
                    startApplying(extracted, def, livingEntity);
                    return true;
                }
            }
            // 2. RS 網路沒有，降級查背包
            return consumeFromBackpack(livingEntity, def, matcher);
        } else {
            // 1. 先查背包
            if (consumeFromBackpack(livingEntity, def, matcher)) {
                return true;
            }
            // 2. 背包沒有，向 RS 網路調取
            Network network = getCachedNetwork(level);
            if (network != null) {
                ItemStack extracted = extractFromRsNetwork(network, matcher, player);
                if (!extracted.isEmpty()) {
                    startApplying(extracted, def, livingEntity);
                    return true;
                }
            }
            return false;
        }
    }

    private boolean consumeFromBackpack(LivingEntity livingEntity, AlchemyItemDefinition def, Predicate<ItemStack> matcher) {
        IItemHandlerSimpleInserter inv = storageWrapper.getInventoryForUpgradeProcessing();
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (!stack.isEmpty() && matcher.test(stack)) {
                ItemStack single = stack.copyWithCount(1);
                stack.shrink(1);
                inv.setStackInSlot(slot, stack);
                startApplying(single, def, livingEntity);
                return true;
            }
        }
        return false;
    }

    private void startApplying(ItemStack stack, AlchemyItemDefinition def, LivingEntity livingEntity) {
        remainingApplyTime = def.startUsing().applyAsInt(stack, livingEntity);
        if (remainingApplyTime > 0) {
            applying = true;
            stackBeingApplied = stack.copyWithCount(1);
            defBeingApplied = def;
            applyingToEntity = livingEntity;
        }
    }

    private ItemStack extractFromRsNetwork(Network network, Predicate<ItemStack> matcher, @Nullable Player player) {
        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) {
            return ItemStack.EMPTY;
        }
        Actor actor = player != null ? new PlayerActor(player) : Actor.EMPTY;

        for (ResourceAmount ra : storage.getAll()) {
            if (ra.amount() > 0 && ra.resource() instanceof ItemResource ir) {
                ItemStack testStack = toItemStack(ir, 1);
                if (matcher.test(testStack)) {
                    long extracted = storage.extract(ir, 1, Action.EXECUTE, actor);
                    if (extracted > 0) {
                        return testStack.copyWithCount((int) extracted);
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack insertIntoRsNetwork(Network network, ItemStack stack, @Nullable Player player) {
        if (network == null || stack.isEmpty()) {
            return stack;
        }
        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) {
            return stack;
        }

        ItemResource res = ItemResource.ofItemStack(stack);
        Actor actor = player != null ? new PlayerActor(player) : Actor.EMPTY;
        long inserted = storage.insert(res, stack.getCount(), Action.EXECUTE, actor);
        if (inserted <= 0) {
            return stack;
        }
        int rem = stack.getCount() - (int) inserted;
        return rem <= 0 ? ItemStack.EMPTY : stack.copyWithCount(rem);
    }

    private static ItemStack toItemStack(ItemResource resource, int count) {
        if (resource.components().isEmpty()) {
            return new ItemStack(resource.item(), count);
        }
        return new ItemStack(BuiltInRegistries.ITEM.wrapAsHolder(resource.item()), count, resource.components());
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