package com.kzjy.mobackup.wrapper;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.kzjy.mobackup.core.PickupContext;
import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.mixin.ItemEntityAccessor;
import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;

public class DimensionalMagnetUpgradeWrapper extends MagnetUpgradeWrapper implements IPriorityRoutingUpgrade {

    private static final String PREVENT_REMOTE_MOVEMENT = "PreventRemoteMovement";
    private static final String ALLOW_MACHINE_MOVEMENT = "AllowMachineRemoteMovement";
    private static final int COOLDOWN_TICKS = 10;
    private static final int FULL_COOLDOWN_TICKS = 40;

    public DimensionalMagnetUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
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
    public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }

        if (isInCooldownCustom(level, entity)) {
            return;
        }

        int cooldown = shouldPickupItems() ? pickupItems(entity, level, pos) : COOLDOWN_TICKS;

        if (shouldPickupXp() && canFillStorageWithXpCustom()) {
            cooldown = Math.min(cooldown, pickupXpOrbsCustom(entity, level, pos));
        }

        if (!(entity instanceof Player)) {
            setCooldown(level, cooldown);
        }
    }

    private static long nextTickTime = Long.MIN_VALUE;

    private boolean isInCooldownCustom(Level level, @Nullable Entity entity) {
        if (!(entity instanceof Player)) {
            return super.isInCooldown(level);
        }

        long gameTime = level.getGameTime();
        if (gameTime > nextTickTime) {
            nextTickTime = gameTime + COOLDOWN_TICKS;
        }
        return nextTickTime > gameTime;
    }

    private boolean canFillStorageWithXpCustom() {
        return storageWrapper.getFluidHandler().map(fluidHandler ->
                fluidHandler.fill(ModFluids.EXPERIENCE_TAG, 1, (Fluid) ModFluids.XP_STILL.get(), FluidAction.SIMULATE) > 0
        ).orElse(false);
    }

    private int pickupXpOrbsCustom(@Nullable Entity entity, Level level, BlockPos pos) {
        List<ExperienceOrb> xpEntities = level.getEntitiesOfClass(
                ExperienceOrb.class,
                new AABB(pos).inflate(((MagnetUpgradeItem) this.upgradeItem).getRadius()),
                e -> true
        );
        if (xpEntities.isEmpty()) {
            return COOLDOWN_TICKS;
        }

        int cooldown = COOLDOWN_TICKS;
        for (ExperienceOrb xpOrb : xpEntities) {
            if (xpOrb.isAlive() && !canNotPickupCustom(xpOrb, entity) && !tryToFillTankCustom(xpOrb, entity, level)) {
                cooldown = FULL_COOLDOWN_TICKS;
                break;
            }
        }
        return cooldown;
    }

    private boolean tryToFillTankCustom(ExperienceOrb xpOrb, @Nullable Entity entity, Level level) {
        int amountToTransfer = XpHelper.experienceToLiquid((float) xpOrb.getValue());
        return storageWrapper.getFluidHandler().map(fluidHandler -> {
            int amountAdded = fluidHandler.fill(ModFluids.EXPERIENCE_TAG, amountToTransfer, (Fluid) ModFluids.XP_STILL.get(), FluidAction.EXECUTE);
            if (amountAdded > 0) {
                Vec3 pos = xpOrb.position();
                xpOrb.value = 0;
                xpOrb.discard();

                Player player = entity instanceof Player p ? p : null;
                if (player != null) {
                    playXpPickupSound(level, player);
                }

                if (amountToTransfer > amountAdded) {
                    level.addFreshEntity(new ExperienceOrb(level, pos.x(), pos.y(), pos.z(), (int) XpHelper.liquidToExperience(amountToTransfer - amountAdded)));
                }
                return true;
            }
            return false;
        }).orElse(false);
    }

    private int pickupItems(@Nullable Entity entity, Level level, BlockPos pos) {
        List<ItemEntity> itemEntities = level.getEntitiesOfClass(
                ItemEntity.class,
                (new AABB(pos)).inflate((double) ((MagnetUpgradeItem) this.upgradeItem).getRadius()),
                (e) -> true
        );
        if (itemEntities.isEmpty()) {
            return COOLDOWN_TICKS;
        } else {
            Player player = entity instanceof Player p ? p : null;
            int cooldown = FULL_COOLDOWN_TICKS;

            for (ItemEntity itemEntity : itemEntities) {
                int delay = ((ItemEntityAccessor) itemEntity).mobackup$getPickupDelay();
                if (itemEntity.isAlive() && delay != 32767 && this.getFilterLogic().matchesFilter(itemEntity.getItem())
                        && !this.canNotPickupCustom(itemEntity, entity) && this.tryToInsertItemCustom(player, itemEntity, level)) {
                    if (player != null) {
                        playItemPickupSound(level, player);
                    }
                    cooldown = COOLDOWN_TICKS;
                }
            }
            return cooldown;
        }
    }

    private boolean canNotPickupCustom(Entity pickedUpEntity, @Nullable Entity entity) {
        CompoundTag data = pickedUpEntity.getPersistentData();
        return entity instanceof Player ? data.contains(PREVENT_REMOTE_MOVEMENT)
                : data.contains(PREVENT_REMOTE_MOVEMENT) && !data.contains(ALLOW_MACHINE_MOVEMENT);
    }

    private boolean tryToInsertItemCustom(@Nullable Player player, ItemEntity itemEntity, Level level) {
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) {
            return false;
        }

        int originalCount = stack.getCount();
        Item item = stack.getItem();
        boolean networkFirst = isNetworkFirst();

        if (networkFirst) {
            Network network = RSBridge.getNetwork(level, getUpgradeStack(), player, BuiltinPermission.INSERT);
            if (network != null && RSBridge.canInsert(network, player)) {
                ItemStack remaining = insertIntoRsNetwork(network, stack, false, player);
                if (remaining.isEmpty()) {
                    itemEntity.setItem(ItemStack.EMPTY);
                    itemEntity.discard();
                    if (player != null) {
                        player.awardStat(Stats.ITEM_PICKED_UP.get(item), originalCount);
                    }
                    return true;
                }
                stack = remaining;
                itemEntity.setItem(stack);
            }

            IItemHandlerSimpleInserter inventory = storageWrapper.getInventoryForUpgradeProcessing();
            ItemStack remaining = inventory.insertItem(stack, false);
            itemEntity.setItem(remaining);

            int inserted = originalCount - remaining.getCount();
            if (inserted > 0) {
                if (remaining.isEmpty()) {
                    itemEntity.discard();
                }
                if (player != null) {
                    player.awardStat(Stats.ITEM_PICKED_UP.get(item), inserted);
                }
                return true;
            }
            return false;

        } else {
            IItemHandlerSimpleInserter inventory = storageWrapper.getInventoryForUpgradeProcessing();
            ItemStack remaining = inventory.insertItem(stack, false);
            if (remaining.isEmpty()) {
                itemEntity.setItem(ItemStack.EMPTY);
                itemEntity.discard();
                if (player != null) {
                    player.awardStat(Stats.ITEM_PICKED_UP.get(item), originalCount);
                }
                return true;
            }

            itemEntity.setItem(remaining);
            Network network = RSBridge.getNetwork(level, getUpgradeStack(), player, BuiltinPermission.INSERT);
            if (network != null && RSBridge.canInsert(network, player)) {
                ItemStack afterRs = insertIntoRsNetwork(network, remaining, false, player);
                itemEntity.setItem(afterRs);
                if (afterRs.isEmpty()) {
                    itemEntity.discard();
                    if (player != null) {
                        player.awardStat(Stats.ITEM_PICKED_UP.get(item), originalCount);
                    }
                    return true;
                }
            }

            int totalInserted = originalCount - itemEntity.getItem().getCount();
            if (totalInserted > 0) {
                if (player != null) {
                    player.awardStat(Stats.ITEM_PICKED_UP.get(item), totalInserted);
                }
                return true;
            }
            return false;
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

    @Override
    public ItemStack pickup(Level level, ItemStack stack, boolean simulate) {
        if (!shouldPickupItems() || !getFilterLogic().matchesFilter(stack)) {
            return stack;
        }

        // 🛡️ 一次性消費上下文
        Player player = PickupContext.current();

        if (isNetworkFirst()) {
            Network network = RSBridge.getNetwork(level, getUpgradeStack(), player, BuiltinPermission.INSERT);
            if (network != null && RSBridge.canInsert(network, player)) {
                stack = insertIntoRsNetwork(network, stack, simulate, player);
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
            Network network = RSBridge.getNetwork(level, getUpgradeStack(), player, BuiltinPermission.INSERT);
            if (network != null && RSBridge.canInsert(network, player)) {
                stack = insertIntoRsNetwork(network, stack, simulate, player);
            }
            return stack;
        }
    }

    private static void playItemPickupSound(Level level, @Nonnull Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                0.2F, (level.random.nextFloat() - level.random.nextFloat()) * 1.4F + 2.0F);
    }

    private static void playXpPickupSound(Level level, @Nonnull Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.1F, (level.random.nextFloat() - level.random.nextFloat()) * 0.35F + 0.9F);
    }
}