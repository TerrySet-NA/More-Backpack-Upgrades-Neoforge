package com.kzjy.mobackup.wrapper;

import java.util.function.Consumer;
import javax.annotation.Nullable;

import com.kzjy.mobackup.core.PickupContext;
import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import com.kzjy.mobackup.util.RSRoutingHelper;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;

public class DimensionalPickupUpgradeWrapper extends PickupUpgradeWrapper implements IPriorityRoutingUpgrade {

    public DimensionalPickupUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
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
    public ItemStack pickup(Level world, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !getFilterLogic().matchesFilter(stack)) {
            return stack;
        }

        Player playerCtx = PickupContext.current();
        var backpackInv = storageWrapper.getInventoryForUpgradeProcessing();

        if (isNetworkFirst()) {
            Network network = RSBridge.getNetwork(world, getUpgradeStack(), playerCtx, BuiltinPermission.INSERT);
            if (network != null) {
                stack = RSRoutingHelper.insertIntoRs(network, stack, simulate, playerCtx);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
            return backpackInv.insertItem(stack, simulate);
        } else {
            stack = backpackInv.insertItem(stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            Network network = RSBridge.getNetwork(world, getUpgradeStack(), playerCtx, BuiltinPermission.INSERT);
            if (network != null) {
                stack = RSRoutingHelper.insertIntoRs(network, stack, simulate, playerCtx);
            }
            return stack;
        }
    }
}