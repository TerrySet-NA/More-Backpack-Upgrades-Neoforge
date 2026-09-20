package com.kzjy.mobackup.wrapper;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.kzjy.mobackup.core.PickupContext;
import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;

public class DimensionalPickupUpgradeWrapper extends PickupUpgradeWrapper implements IPriorityRoutingUpgrade {

    public DimensionalPickupUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
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
    // 拾取核心調度（嚴格防穿透）
    // =========================================================================

    @Override
    public ItemStack pickup(Level world, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !getFilterLogic().matchesFilter(stack)) {
            return stack;
        }

        // 獲取當前撿起物品的玩家身分
        Player playerCtx = PickupContext.current();

        if (isNetworkFirst()) {
            // === 模式 A：RS 網路優先 ===
            // 🛡️ 嚴格權限核驗：若無權限，network 直接取回 null，杜絕穿透
            Network network = RSBridge.getNetwork(world, getUpgradeStack(), playerCtx, BuiltinPermission.INSERT);
            if (network != null && RSBridge.canInsert(network, playerCtx)) {
                stack = insertIntoRsNetwork(network, stack, simulate, playerCtx);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
            // RS 無權限、斷電、滿載或未連線，轉入隨身背包
            return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
        } else {
            // === 模式 B：背包優先 ===
            stack = storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            // 背包滿了才進 RS，同樣受嚴格權限審查保護
            Network network = RSBridge.getNetwork(world, getUpgradeStack(), playerCtx, BuiltinPermission.INSERT);
            if (network != null && RSBridge.canInsert(network, playerCtx)) {
                stack = insertIntoRsNetwork(network, stack, simulate, playerCtx);
            }
            return stack;
        }
    }

    /**
     * 封裝 RS 插入邏輯
     */
    private ItemStack insertIntoRsNetwork(Network network, ItemStack stack, boolean simulate, @Nullable Player player) {
        if (network == null || stack.isEmpty()) {
            return stack;
        }

        // 🛡️ 雙重防護：再次確認該玩家/機器是否有權限寫入
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
}