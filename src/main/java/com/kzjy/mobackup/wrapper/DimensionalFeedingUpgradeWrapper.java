package com.kzjy.mobackup.wrapper;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.annotation.Nullable;

import com.kzjy.mobackup.core.RSBridge;
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
import net.neoforged.neoforge.event.EventHooks;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.HungerLevel;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

public class DimensionalFeedingUpgradeWrapper extends FeedingUpgradeWrapper implements IPriorityRoutingUpgrade {
    private static final int COOLDOWN = 100;
    private static final int STILL_HUNGRY_COOLDOWN = 10;
    private static final int FEEDING_RANGE = 3;
    private BlockPos currentBlockPos = null;

    public DimensionalFeedingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    @Override
    public boolean getDefaultNetworkFirst() {
        return true;
    }

    @Override
    public void save() {
        super.save();
    }

    @Override
    public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (isInCooldown(level)) {
            return;
        }

        currentBlockPos = pos.immutable();

        boolean hungryPlayer = false;
        if (entity instanceof Player carrier) {
            if (feedPlayerAndGetHungryCustom(carrier, carrier, level)) {
                hungryPlayer = true;
            }
        } else {
            AtomicBoolean stillHungryPlayer = new AtomicBoolean(false);
            level.getEntities(EntityType.PLAYER, new AABB(pos).inflate(FEEDING_RANGE), p -> true)
                    .forEach(target -> stillHungryPlayer.set(stillHungryPlayer.get() || feedPlayerAndGetHungryCustom(target, null, level)));
            hungryPlayer = stillHungryPlayer.get();
        }

        if (hungryPlayer) {
            setCooldown(level, STILL_HUNGRY_COOLDOWN);
            return;
        }

        setCooldown(level, COOLDOWN);
    }

    private boolean feedPlayerAndGetHungryCustom(Player targetPlayer, @Nullable Player actionPlayer, Level level) {
        int hungerLevel = 20 - targetPlayer.getFoodData().getFoodLevel();
        if (hungerLevel == 0) {
            return false;
        }

        boolean fed;
        if (isNetworkFirst()) {
            fed = tryFeedingFromRsNetwork(level, hungerLevel, targetPlayer, actionPlayer);
            if (!fed) {
                fed = tryFeedingFromBackpackStorage(level, hungerLevel, targetPlayer, actionPlayer);
            }
        } else {
            fed = tryFeedingFromBackpackStorage(level, hungerLevel, targetPlayer, actionPlayer);
            if (!fed) {
                fed = tryFeedingFromRsNetwork(level, hungerLevel, targetPlayer, actionPlayer);
            }
        }

        return fed && targetPlayer.getFoodData().getFoodLevel() < 20;
    }

    private boolean tryFeedingFromRsNetwork(Level level, int hungerLevel, Player targetPlayer, @Nullable Player actionPlayer) {
        Network network = RSBridge.getNetwork(level, getUpgradeStack(), actionPlayer, this.currentBlockPos, BuiltinPermission.EXTRACT);
        if (network == null) return false;

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) return false;

        boolean isHurt = targetPlayer.getHealth() < targetPlayer.getMaxHealth() - 0.1F;
        Actor actor = RSBridge.getActor(actionPlayer);

        for (ResourceAmount resourceAmount : new ArrayList<>(storage.getAll())) {
            if (resourceAmount.amount() <= 0) continue;

            if (resourceAmount.resource() instanceof ItemResource itemResource) {
                ItemStack candidate = itemResource.toItemStack(1);

                if (isEdibleCustom(candidate, targetPlayer) && getFilterLogic().matchesFilter(candidate)
                        && (isHungryEnoughForFoodCustom(hungerLevel, candidate, targetPlayer) || (shouldFeedImmediatelyWhenHurt() && hungerLevel > 0 && isHurt))) {

                    long actuallyExtracted = storage.extract(itemResource, 1, Action.EXECUTE, actor);
                    if (actuallyExtracted < 1) continue;

                    ItemStack foodToEat = candidate.copyWithCount(1);
                    boolean consumed = executeFeedPlayer(level, targetPlayer, actionPlayer, foodToEat);

                    if (consumed) {
                        return true;
                    } else {
                        long refunded = storage.insert(itemResource, 1, Action.EXECUTE, actor);
                        if (refunded < 1) {
                            SafetyRollbackHelper.fallbackToBackpackOrPlayer(foodToEat, storageWrapper, targetPlayer);
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean tryFeedingFromBackpackStorage(Level level, int hungerLevel, Player targetPlayer, @Nullable Player actionPlayer) {
        ITrackedContentsItemHandler inventory = storageWrapper.getInventoryForUpgradeProcessing();
        return InventoryHelper.iterate(inventory, (slot, stack) -> {
            boolean isHurt = targetPlayer.getHealth() < targetPlayer.getMaxHealth() - 0.1F;
            if (isEdibleCustom(stack, targetPlayer) && getFilterLogic().matchesFilter(stack)
                    && (isHungryEnoughForFoodCustom(hungerLevel, stack, targetPlayer) || (shouldFeedImmediatelyWhenHurt() && hungerLevel > 0 && isHurt))) {

                ItemStack extractedFood = inventory.extractItem(slot, 1, false);
                if (extractedFood.isEmpty()) return false;

                boolean consumed = executeFeedPlayer(level, targetPlayer, actionPlayer, extractedFood);
                if (consumed) {
                    return true;
                } else {
                    ItemStack unhandled = inventory.insertItem(extractedFood, false);
                    if (!unhandled.isEmpty()) {
                        SafetyRollbackHelper.fallbackToBackpackOrPlayer(unhandled, storageWrapper, targetPlayer);
                    }
                }
            }
            return false;
        }, () -> false, ret -> ret);
    }

    private boolean executeFeedPlayer(Level level, Player targetPlayer, @Nullable Player actionPlayer, ItemStack singleFoodStack) {
        ItemStack mainHandItem = targetPlayer.getMainHandItem();
        targetPlayer.getInventory().items.set(targetPlayer.getInventory().selected, singleFoodStack);

        try {
            ItemStack useCopy = singleFoodStack.copy();
            if (useCopy.use(level, targetPlayer, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME) {
                ItemStack resultItem = EventHooks.onItemUseFinish(targetPlayer, useCopy.copy(), 0,
                        useCopy.getItem().finishUsingItem(useCopy.copy(), level, targetPlayer));

                if (!resultItem.isEmpty()) {
                    handleLeftovers(resultItem, targetPlayer, actionPlayer, level);
                }
                return true;
            }
            return false;
        } finally {
            targetPlayer.stopUsingItem();
            targetPlayer.getInventory().items.set(targetPlayer.getInventory().selected, mainHandItem);
        }
    }

    private void handleLeftovers(ItemStack leftover, Player targetPlayer, @Nullable Player actionPlayer, Level level) {
        if (leftover.isEmpty()) return;

        Network network = RSBridge.getNetwork(level, getUpgradeStack(), actionPlayer, this.currentBlockPos, BuiltinPermission.INSERT);
        var backpackInv = storageWrapper.getInventoryForUpgradeProcessing();

        if (isNetworkFirst()) {
            if (network != null) leftover = RSRoutingHelper.insertIntoRs(network, leftover, false, actionPlayer);
            if (!leftover.isEmpty()) leftover = backpackInv.insertItem(leftover, false);
        } else {
            leftover = backpackInv.insertItem(leftover, false);
            if (!leftover.isEmpty() && network != null) {
                leftover = RSRoutingHelper.insertIntoRs(network, leftover, false, actionPlayer);
            }
        }

        if (!leftover.isEmpty()) {
            SafetyRollbackHelper.fallbackToBackpackOrPlayer(leftover, storageWrapper, targetPlayer);
        }
    }

    private static boolean isEdibleCustom(ItemStack stack, LivingEntity player) {
        if (stack.getItem() == Items.OMINOUS_BOTTLE) return false;
        FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, player);
        return foodProperties != null && foodProperties.nutrition() >= 1;
    }

    private boolean isHungryEnoughForFoodCustom(int hungerLevel, ItemStack stack, Player player) {
        FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, player);
        if (foodProperties == null) return false;

        HungerLevel feedAtHungerLevel = getFeedAtHungerLevel();
        if (feedAtHungerLevel == HungerLevel.ANY) return true;

        int nutrition = foodProperties.nutrition();
        return (feedAtHungerLevel == HungerLevel.HALF ? (nutrition / 2) : nutrition) <= hungerLevel;
    }
}