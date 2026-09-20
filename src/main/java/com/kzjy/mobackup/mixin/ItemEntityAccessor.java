/*
 * Copyright (C) 2026 TerrySet
 */

package com.kzjy.mobackup.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {

    @Accessor("pickupDelay")
    int mobackup$getPickupDelay();
}