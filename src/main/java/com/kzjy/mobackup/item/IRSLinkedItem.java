/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.item;

import java.util.List;

import com.kzjy.mobackup.core.RSBridge;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemTargetBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("null")
public interface IRSLinkedItem extends IMoBackupUpgrade {

    default boolean isDimensional() {
        return true;
    }

    default InteractionResult handleUseOn(UseOnContext ctx) {
        if (ctx.getPlayer() == null) {
            return InteractionResult.PASS;
        }

        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        // 避免字串反射，直接以介面與 Namespace 判斷 RS 方塊
        boolean isRsBlock = be instanceof NetworkNodeContainerProvider 
                || be instanceof NetworkItemTargetBlockEntity
                || "refinedstorage".equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getNamespace());

        if (isRsBlock) {
            if (!level.isClientSide()) {
                RSBridge.saveCoordinate(ctx.getItemInHand(), level, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
    }

    default void appendHoverText(ItemStack stack, List<Component> tooltip) {
        GlobalPos boundTarget = RSBridge.getBoundTarget(stack);

        if (boundTarget != null) {
            BlockPos pos = boundTarget.pos();
            tooltip.add(Component.translatable(
                    "item.refinedstorage.network_item.bound_to",
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            ).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.refinedstorage.network_item.unbound").withStyle(ChatFormatting.RED));
        }
    }
}