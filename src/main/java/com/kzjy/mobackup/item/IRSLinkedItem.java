/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.item;

import com.kzjy.mobackup.core.RSBridge;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

@SuppressWarnings("null")
public interface IRSLinkedItem extends IMoBackupUpgrade {
    default InteractionResult handleUseOn(UseOnContext ctx) {
        if (ctx.getPlayer() == null) {
            return InteractionResult.PASS;
        }

        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        boolean isRsBlock = be instanceof NetworkNodeContainerProvider 
                || level.getBlockState(pos).getBlock().getClass().getName().contains("refinedstorage");

        if (isRsBlock) {
            if (!level.isClientSide()) {
                ItemStack stack = ctx.getItemInHand();

                // 1.21.1 正確更新 CustomData
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    RSBridge.saveCoordinate(tag, pos, level.dimension());
                });
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    default void appendHoverText(ItemStack stack, List<Component> tooltip) {
        BlockPos pos = RSBridge.getCoordinate(stack);
        ResourceKey<Level> dim = RSBridge.getDimension(stack);

        if (pos != null && dim != null) {
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