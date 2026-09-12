/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.mixin;

import com.kzjy.mobackup.item.IMoBackupUpgrade;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StorageContainerMenuBase.StorageUpgradeSlot.class)
public class StorageUpgradeSlotMixin {

    /**
     * 攔截 mayPlace 裡對 isItemValid 的呼叫：
     * 繞過原版 UpgradeHandler 對第三方模組物品的白名單阻擋
     */
    @Redirect(
        method = "mayPlace",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/items/IItemHandler;isItemValid(ILnet/minecraft/world/item/ItemStack;)Z"
        ),
        remap = false
    )
    private boolean mobackup$bypassIsItemValid(IItemHandler handler, int slot, ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof IMoBackupUpgrade) {
            // 是本模組升級卡，直接強制判定有效，放行讓 mayPlace 進入 canAddUpgradeTo 檢查！
            return true;
        }
        // 非本模組物品，交回原版 UpgradeHandler 判定
        return handler.isItemValid(slot, stack);
    }
}