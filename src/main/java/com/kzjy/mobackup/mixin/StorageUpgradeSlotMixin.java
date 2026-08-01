/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.mixin;

import com.kzjy.mobackup.MoBackup;
import com.kzjy.mobackup.item.IMoBackupUpgrade;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 限制可放入背包升級槽的物品類型與數量
 * 僅允許模組自訂升級卡，且限制每個背包最多放入 1 張相同卡片
 */
@Mixin(StorageContainerMenuBase.StorageUpgradeSlot.class)
public class StorageUpgradeSlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true, remap = false)
    private void mobackup$mayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IMoBackupUpgrade)) {
            return; // 非本模組卡片，交回原版 Sophisticated Core 邏輯處理
        }

        // 轉型獲取當前升級槽容器
        SlotItemHandler slot = (SlotItemHandler) (Object) this;
        IItemHandler itemHandler = slot.getItemHandler();
        int currentSlotIndex = slot.getSlotIndex(); // 當前準備放進去的槽位編號

        int existingCount = 0;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            // 跳過當前操作的槽位本身（防止玩家拿起卡片再放回同一格時被誤判為已存在）
            if (i == currentSlotIndex) continue;

            ItemStack inSlot = itemHandler.getStackInSlot(i);
            if (!inSlot.isEmpty() && inSlot.getItem() == stack.getItem()) {
                existingCount++;
            }
        }

        if (existingCount >= 1) {
            MoBackup.LOGGER.warn("[MoBackup-Debug] mayPlace 拒絕：升級槽內已存在相同的卡片 ({})", stack.getHoverName().getString());
            cir.setReturnValue(false); // 槽內已有相同卡片，擋掉
        } else {
            MoBackup.LOGGER.info("[MoBackup-Debug] mayPlace 直接放行：許可寫入升級槽 ({})", stack.getHoverName().getString());
            cir.setReturnValue(true);  // 槽內沒有，無條件放行
        }
    }
}