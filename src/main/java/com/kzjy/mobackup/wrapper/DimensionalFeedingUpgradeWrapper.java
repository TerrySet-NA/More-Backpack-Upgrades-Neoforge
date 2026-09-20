package com.kzjy.mobackup.wrapper;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.kzjy.mobackup.core.RSBridge;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
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

    public DimensionalFeedingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
                                           Consumer<ItemStack> upgradeSaveHandler) {
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
    // 主 Tick 調度與餵食邏輯
    // =========================================================================

    @Override
    public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (isInCooldown(level)) {
            return;
        }

        boolean hungryPlayer = false;
        if (entity instanceof Player carrier) {
            // 隨身背包模式：動作執行者為攜帶者
            if (feedPlayerAndGetHungryCustom(carrier, carrier, level)) {
                hungryPlayer = true;
            }
        } else {
            // 🎯 規則 3: 背包置於地上時，actionPlayer 嚴格為 null，不拿路過玩家當作 RS 操作者
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
        // 規則 10: 非 XOR 升級，優先不足/失敗時切換另一端
        if (isNetworkFirst()) {
            // RS 優先：1. 先由 RS 網路調取食物；2. 失敗/無食物時降級由背包餵食
            fed = tryFeedingFromRsNetwork(level, hungerLevel, targetPlayer, actionPlayer);
            if (!fed) {
                fed = tryFeedingFromBackpackStorage(level, hungerLevel, targetPlayer, actionPlayer);
            }
        } else {
            // 背包優先：1. 先消耗隨身背包食物；2. 吃光時跨維度自 RS 調取
            fed = tryFeedingFromBackpackStorage(level, hungerLevel, targetPlayer, actionPlayer);
            if (!fed) {
                fed = tryFeedingFromRsNetwork(level, hungerLevel, targetPlayer, actionPlayer);
            }
        }

        return fed && targetPlayer.getFoodData().getFoodLevel() < 20;
    }

    /**
     * 從 RS 網路尋找並食用食物
     */
    private boolean tryFeedingFromRsNetwork(Level level, int hungerLevel, Player targetPlayer, @Nullable Player actionPlayer) {
        Network network = RSBridge.getNetwork(level, getUpgradeStack(), actionPlayer, BuiltinPermission.EXTRACT);
        if (network == null) {
            return false;
        }

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) {
            return false;
        }

        boolean isHurt = targetPlayer.getHealth() < targetPlayer.getMaxHealth() - 0.1F;
        // 🎯 規則 1: actionPlayer 為 null 時使用 Actor.EMPTY
        Actor actor = actionPlayer != null ? new PlayerActor(actionPlayer) : Actor.EMPTY;

        for (ResourceAmount resourceAmount : new ArrayList<>(storage.getAll())) {
            if (resourceAmount.amount() <= 0) {
                continue;
            }

            if (resourceAmount.resource() instanceof ItemResource itemResource) {
                ItemStack candidate = itemResource.toItemStack(1);

                if (isEdibleCustom(candidate, targetPlayer) && getFilterLogic().matchesFilter(candidate)
                        && (isHungryEnoughForFoodCustom(hungerLevel, candidate, targetPlayer) || (shouldFeedImmediatelyWhenHurt() && hungerLevel > 0 && isHurt))) {

                    // 1. 預先自 RS 抽取 1 份食物
                    long actuallyExtracted = storage.extract(itemResource, 1, Action.EXECUTE, actor);
                    if (actuallyExtracted < 1) {
                        continue;
                    }

                    ItemStack foodToEat = candidate.copyWithCount(1);

                    // 2. 執行食用
                    boolean consumed = executeFeedPlayer(level, targetPlayer, actionPlayer, foodToEat);

                    if (consumed) {
                        return true;
                    } else {
                        // 3. 食用失敗回退：退回 RS；若 RS 拒收，安全兜底給目標玩家
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

    /**
     * 從精妙背包自身儲存餵食
     */
    private boolean tryFeedingFromBackpackStorage(Level level, int hungerLevel, Player targetPlayer, @Nullable Player actionPlayer) {
        ITrackedContentsItemHandler inventory = storageWrapper.getInventoryForUpgradeProcessing();
        return InventoryHelper.iterate(inventory, (slot, stack) -> {
            boolean isHurt = targetPlayer.getHealth() < targetPlayer.getMaxHealth() - 0.1F;
            if (isEdibleCustom(stack, targetPlayer) && getFilterLogic().matchesFilter(stack)
                    && (isHungryEnoughForFoodCustom(hungerLevel, stack, targetPlayer) || (shouldFeedImmediatelyWhenHurt() && hungerLevel > 0 && isHurt))) {

                // 1. 預先從背包提取 1 份
                ItemStack extractedFood = inventory.extractItem(slot, 1, false);
                if (extractedFood.isEmpty()) {
                    return false;
                }

                // 2. 執行食用
                boolean consumed = executeFeedPlayer(level, targetPlayer, actionPlayer, extractedFood);

                if (consumed) {
                    return true;
                } else {
                    // 3. 食用失敗回退：退回背包；若背包拒收，安全兜底給目標玩家
                    ItemStack unhandled = inventory.insertItem(extractedFood, false);
                    if (!unhandled.isEmpty()) {
                        SafetyRollbackHelper.fallbackToBackpackOrPlayer(unhandled, storageWrapper, targetPlayer);
                    }
                }
            }
            return false;
        }, () -> false, ret -> ret);
    }

    /**
     * 玩家食用處理器
     */
    private boolean executeFeedPlayer(Level level, Player targetPlayer, @Nullable Player actionPlayer, ItemStack singleFoodStack) {
        ItemStack mainHandItem = targetPlayer.getMainHandItem();
        targetPlayer.getInventory().items.set(targetPlayer.getInventory().selected, singleFoodStack);

        try {
            ItemStack useCopy = singleFoodStack.copy();
            if (useCopy.use(level, targetPlayer, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME) {
                ItemStack resultItem = EventHooks.onItemUseFinish(targetPlayer, useCopy.copy(), 0,
                        useCopy.getItem().finishUsingItem(useCopy.copy(), level, targetPlayer));

                // 餐具/殘渣回收
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

    /**
     * 餐具回收：依據優先級（RS ⇄ 背包）雙向級聯回存，最後兜底至玩家
     */
    private void handleLeftovers(ItemStack leftover, Player targetPlayer, @Nullable Player actionPlayer, Level level) {
        if (leftover.isEmpty()) {
            return;
        }

        if (isNetworkFirst()) {
            // RS 優先：RS -> 背包 -> 玩家
            leftover = insertIntoRs(leftover, actionPlayer, level);
            if (!leftover.isEmpty()) {
                leftover = storageWrapper.getInventoryForUpgradeProcessing().insertItem(leftover, false);
            }
        } else {
            // 背包優先：背包 -> RS -> 玩家
            leftover = storageWrapper.getInventoryForUpgradeProcessing().insertItem(leftover, false);
            if (!leftover.isEmpty()) {
                leftover = insertIntoRs(leftover, actionPlayer, level);
            }
        }

        if (!leftover.isEmpty()) {
            SafetyRollbackHelper.fallbackToBackpackOrPlayer(leftover, storageWrapper, targetPlayer);
        }
    }

    /**
     * 回收餐具至 RS
     */
    private ItemStack insertIntoRs(ItemStack stack, @Nullable Player actionPlayer, Level level) {
        Network network = RSBridge.getNetwork(level, getUpgradeStack(), actionPlayer, BuiltinPermission.INSERT);
        if (network == null || stack.isEmpty()) {
            return stack;
        }

        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage == null) {
            return stack;
        }

        ItemResource resource = ItemResource.ofItemStack(stack);
        Actor actor = actionPlayer != null ? new PlayerActor(actionPlayer) : Actor.EMPTY;
        long inserted = storage.insert(resource, stack.getCount(), Action.EXECUTE, actor);
        if (inserted <= 0) {
            return stack;
        }

        int remaining = stack.getCount() - (int) inserted;
        return remaining > 0 ? stack.copyWithCount(remaining) : ItemStack.EMPTY;
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