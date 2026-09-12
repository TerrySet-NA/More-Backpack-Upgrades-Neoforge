/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.mixin;

import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StorageContainerMenuBase.class, remap = false)
public abstract class StorageContainerMenuBaseMixin {

    @Inject(method = "handlePacket", at = @At("HEAD"))
    private void mobackup$onHandlePacket(CompoundTag data, CallbackInfo ci) {
        if (data.contains("containerId") && data.contains("mobackup:priority")) {
            int containerId = data.getInt("containerId");
            StorageContainerMenuBase<?> menu = (StorageContainerMenuBase<?>) (Object) this;
            UpgradeContainerBase<?, ?> container = menu.getUpgradeContainers().get(containerId);
            if (container != null && container.getUpgradeWrapper() instanceof IPriorityRoutingUpgrade priorityUpgrade) {
                priorityUpgrade.setNetworkFirst(data.getBoolean("mobackup:priority"));
            }
        }
    }
}