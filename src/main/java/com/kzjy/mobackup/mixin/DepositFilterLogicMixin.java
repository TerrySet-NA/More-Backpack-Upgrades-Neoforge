/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.mixin;

import com.kzjy.mobackup.duck.IDepositFilterLogicExtension;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(value = DepositFilterLogic.class, remap = false)
public abstract class DepositFilterLogicMixin implements IDepositFilterLogicExtension {

    @Shadow
    private boolean shouldFilterByInventory() {
        throw new AssertionError();
    }

    @Unique
    @Nullable
    private StorageNetworkComponent mobackup$rsStorage = null;

    @Override
    public void mobackup$setRsStorage(@Nullable StorageNetworkComponent storage) {
        this.mobackup$rsStorage = storage;
    }

    @Override
    public @Nullable StorageNetworkComponent mobackup$getRsStorage() {
        return this.mobackup$rsStorage;
    }

    /**
     * 1. 攔截 setInventory：若有傳入 RS 網路，跳過 IItemHandler 掃描
     */
    @Inject(method = "setInventory", at = @At("HEAD"), cancellable = true)
    private void mobackup$onSetInventory(IItemHandler inventory, CallbackInfo ci) {
        if (this.mobackup$rsStorage != null) {
            ci.cancel();
        }
    }

    /**
     * 2. 攔截 matchesFilter：若有傳入 RS 網路且為 INVENTORY 模式，直接 O(1) 檢查 RS 存量！
     */
    @SuppressWarnings("null")
    @Inject(method = "matchesFilter", at = @At("HEAD"), cancellable = true)
    private void mobackup$onMatchesFilter(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.shouldFilterByInventory() && this.mobackup$rsStorage != null) {
            ItemResource resource = ItemResource.ofItemStack(stack);
            // 模擬抽取 1 顆，大於 0 代表 RS 網路已有該項庫存
            boolean existsInRs = this.mobackup$rsStorage.extract(resource, 1, Action.SIMULATE, Actor.EMPTY) > 0;
            cir.setReturnValue(existsInRs);
        }
    }
}