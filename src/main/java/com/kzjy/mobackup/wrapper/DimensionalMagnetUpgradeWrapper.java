/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.wrapper;

// import com.kzjy.mobackup.MoBackup;
// import com.kzjy.mobackup.MoBackup;
import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("null")
public class DimensionalMagnetUpgradeWrapper extends MagnetUpgradeWrapper implements IPriorityRoutingUpgrade {

    private static final String PREVENT_REMOTE_MOVEMENT = "PreventRemoteMovement";
    private static final String ALLOW_MACHINE_MOVEMENT = "AllowMachineRemoteMovement";
    private static final int COOLDOWN_TICKS = 10;
    private static final int FULL_COOLDOWN_TICKS = 40;


    public DimensionalMagnetUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
                                           Consumer<ItemStack> upgradeSaveHandler) {
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
            ItemStack currentStack = getUpgradeStack();
            cachedNetwork = RSBridge.getNetwork(level, currentStack);
            // MoBackup.LOGGER.info("[MoBackup-Debug] 磁吸卡取得 RS 網路實例 -> {}", (cachedNetwork != null ? "【成功】" : "【失敗: null】"));
        }
        return cachedNetwork;
    }

    @Override
    public void tick(@Nullable Entity entity, Level world, BlockPos pos) {
        if (isInCooldown(world)) {
            return;
        }

        int cooldown = shouldPickupItems() ? pickupItemsCustom(entity, world, pos) : FULL_COOLDOWN_TICKS;

        if (shouldPickupXp() && canFillStorageWithXpCustom()) {
            cooldown = Math.min(cooldown, pickupXpOrbsCustom(entity, world, pos));
        }

        setCooldown(world, cooldown);
    }

    private boolean canFillStorageWithXpCustom() {
        return storageWrapper.getFluidHandler().map(fluidHandler -> 
            fluidHandler.fill(new FluidStack(ModFluids.XP_STILL.get(), 1), IFluidHandler.FluidAction.SIMULATE) > 0
        ).orElse(false);
    }

    private int pickupXpOrbsCustom(@Nullable Entity entity, Level world, BlockPos pos) {
        List<ExperienceOrb> xpEntities = world.getEntitiesOfClass(ExperienceOrb.class,
                new AABB(pos).inflate(upgradeItem.getRadius()), e -> true);
        if (xpEntities.isEmpty()) {
            return COOLDOWN_TICKS;
        }

        int cooldown = COOLDOWN_TICKS;
        for (ExperienceOrb xpOrb : xpEntities) {
            if (xpOrb.isAlive() && !canNotPickupCustom(xpOrb, entity) && !tryToFillTankCustom(xpOrb, entity, world)) {
                cooldown = FULL_COOLDOWN_TICKS;
                break;
            }
        }
        return cooldown;
    }

    private boolean tryToFillTankCustom(ExperienceOrb xpOrb, @Nullable Entity entity, Level world) {
        int amountToTransfer = XpHelper.experienceToLiquid(xpOrb.getValue());

        return storageWrapper.getFluidHandler().map(fluidHandler -> {
            int amountAdded = fluidHandler.fill(
                new FluidStack(ModFluids.XP_STILL.get(), amountToTransfer),
                IFluidHandler.FluidAction.EXECUTE
            );

            if (amountAdded > 0) {
                Vec3 pos = xpOrb.position();
                xpOrb.value = 0;
                xpOrb.discard();

                if (entity instanceof Player player) {
                    playXpPickupSound(world, player);
                }

                if (amountToTransfer > amountAdded) {
                    world.addFreshEntity(new ExperienceOrb(world, pos.x(), pos.y(), pos.z(),
                            (int) XpHelper.liquidToExperience(amountToTransfer - amountAdded)));
                }
                return true;
            }
            return false;
        }).orElse(false);
    }

    private int pickupItemsCustom(@Nullable Entity entity, Level world, BlockPos pos) {
        List<ItemEntity> itemEntities = world.getEntitiesOfClass(ItemEntity.class,
                new AABB(pos).inflate(upgradeItem.getRadius()), e -> true);
        if (itemEntities.isEmpty()) {
            return COOLDOWN_TICKS;
        }

        Player player = entity instanceof Player ? (Player) entity : null;

        int cooldown = FULL_COOLDOWN_TICKS;
        for (ItemEntity itemEntity : itemEntities) {
            if (!itemEntity.isAlive() || !getFilterLogic().matchesFilter(itemEntity.getItem())
                    || canNotPickupCustom(itemEntity, entity)) {
                continue;
            }
            if (tryToInsertItemCustom(itemEntity, world, player)) {
                if (player != null) {
                    playItemPickupSound(world, player);
                }
                cooldown = COOLDOWN_TICKS;
            }
        }
        return cooldown;
    }

    private boolean canNotPickupCustom(Entity pickedUpEntity, @Nullable Entity entity) {
        CompoundTag data = pickedUpEntity.getPersistentData();
        return entity instanceof Player ? data.contains(PREVENT_REMOTE_MOVEMENT)
                : data.contains(PREVENT_REMOTE_MOVEMENT) && !data.contains(ALLOW_MACHINE_MOVEMENT);
    }

    private ItemStack insertIntoRsNetwork(Network network, ItemStack stack, boolean simulate, @Nullable Player player) {
        if (network == null || stack.isEmpty()) {
            return stack;
        }

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) {
            // MoBackup.LOGGER.warn("[MoBackup-Debug] RS 網路缺乏 StorageNetworkComponent 模組");
            return stack;
        }

        ItemResource resource = ItemResource.ofItemStack(stack);
        Action action = simulate ? Action.SIMULATE : Action.EXECUTE;

        long inserted = storage.insert(resource, stack.getCount(), action, Actor.EMPTY);
        // MoBackup.LOGGER.info("[MoBackup-Debug] RS storage.insert 回傳寫入數量: {}", inserted);

        if (inserted <= 0) {
            return stack;
        }

        int remainingCount = stack.getCount() - (int) inserted;
        if (remainingCount <= 0) {
            return ItemStack.EMPTY;
        }

        return stack.copyWithCount(remainingCount);
    }

    private static void playItemPickupSound(Level world, @Nonnull Player player) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                0.2F, (world.random.nextFloat() - world.random.nextFloat()) * 1.4F + 2.0F);
    }

    private static void playXpPickupSound(Level world, @Nonnull Player player) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.1F, (world.random.nextFloat() - world.random.nextFloat()) * 0.35F + 0.9F);
    }

    private Boolean networkFirstCache = null;

    public boolean isNetworkFirst() {
        if (networkFirstCache == null) {
            net.minecraft.world.item.component.CustomData customData = upgrade.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (customData != null && customData.contains("PriorityNetwork")) {
                networkFirstCache = customData.copyTag().getBoolean("PriorityNetwork");
            } else {
                networkFirstCache = true; // 預設：網路優先
            }
        }
        return networkFirstCache;
    }

    public void setNetworkFirst(boolean networkFirst) {
        this.networkFirstCache = networkFirst;
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, upgrade, tag -> {
            tag.putBoolean("PriorityNetwork", networkFirst);
        });
        save();
        // MoBackup.LOGGER.info("[MoBackup-Debug] 磁吸卡優先級已更新為: {}", networkFirst ? "【RS 網路優先】" : "【精妙背包優先】");
    }

    private boolean tryToInsertItemCustom(ItemEntity itemEntity, Level world, @Nullable Player player) {
        ItemStack stack = itemEntity.getItem();
        int originalCount = stack.getCount();
        boolean networkFirst = isNetworkFirst();

        if (networkFirst) {
            // === 模式 A：網路優先 (RS -> 背包) ===
            Network network = getCachedNetwork(world);
            if (network != null) {
                ItemStack remaining = insertIntoRsNetwork(network, stack, false, player);
                if (remaining.isEmpty()) {
                    itemEntity.setItem(ItemStack.EMPTY);
                    itemEntity.discard();
                    return true;
                }
                stack = remaining;
                itemEntity.setItem(stack);
            }
            return insertIntoBackpack(itemEntity, stack);
        } else {
            // === 模式 B：背包優先 (背包 -> RS 溢出) ===
            IItemHandlerSimpleInserter inventory = storageWrapper.getInventoryForUpgradeProcessing();
            ItemStack remaining = inventory.insertItem(stack, false);
            if (remaining.isEmpty()) {
                itemEntity.setItem(ItemStack.EMPTY);
                itemEntity.discard();
                return true;
            }

            // 背包塞滿後的溢出物資，自動存入 RS 網路
            itemEntity.setItem(remaining);
            Network network = getCachedNetwork(world);
            if (network != null) {
                ItemStack afterRs = insertIntoRsNetwork(network, remaining, false, player);
                itemEntity.setItem(afterRs);
                if (afterRs.isEmpty()) {
                    itemEntity.discard();
                    return true;
                }
            }
            return remaining.getCount() < originalCount;
        }
    }

    private boolean insertIntoBackpack(ItemEntity itemEntity, ItemStack stack) {
        IItemHandlerSimpleInserter inventory = storageWrapper.getInventoryForUpgradeProcessing();
        ItemStack remaining = inventory.insertItem(stack, true);
        if (remaining.getCount() != stack.getCount()) {
            remaining = inventory.insertItem(stack, false);
            itemEntity.setItem(remaining);
            if (remaining.isEmpty()) {
                itemEntity.discard();
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack pickup(Level world, ItemStack stack, boolean simulate) {
        if (!shouldPickupItems() || !getFilterLogic().matchesFilter(stack)) {
            return stack;
        }

        if (isNetworkFirst()) {
            // 網路優先：先 RS，後背包
            Network network = getCachedNetwork(world);
            if (network != null) {
                stack = insertIntoRsNetwork(network, stack, simulate, null);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
            return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
        } else {
            // 背包優先：先背包，後 RS
            stack = storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            Network network = getCachedNetwork(world);
            if (network != null) {
                stack = insertIntoRsNetwork(network, stack, simulate, null);
            }
            return stack;
        }
    }
}