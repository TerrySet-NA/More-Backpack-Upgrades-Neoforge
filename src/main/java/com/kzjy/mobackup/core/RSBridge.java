/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.core;

import javax.annotation.Nullable;

// import org.slf4j.Logger;

import com.kzjy.mobackup.MoBackup;
// import com.mojang.logging.LogUtils;
import com.refinedmods.refinedstorage.api.network.Network;
// import com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage.api.network.node.NetworkNode;
import com.refinedmods.refinedstorage.api.network.node.container.NetworkNodeContainer;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemTargetBlockEntity;
// import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("null")
public class RSBridge {

    private static final String NBT_RECEIVER_X = "ReceiverX";
    private static final String NBT_RECEIVER_Y = "ReceiverY";
    private static final String NBT_RECEIVER_Z = "ReceiverZ";
    private static final String NBT_DIMENSION = "Dimension";

    public static void saveCoordinate(ItemStack stack, Level level, BlockPos pos) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            saveCoordinate(tag, pos, level.dimension());
        });
    }

    public static void saveCoordinate(CompoundTag tag, BlockPos pos, ResourceKey<Level> dimension) {
        tag.putInt(NBT_RECEIVER_X, pos.getX());
        tag.putInt(NBT_RECEIVER_Y, pos.getY());
        tag.putInt(NBT_RECEIVER_Z, pos.getZ());
        tag.putString(NBT_DIMENSION, dimension.location().toString());
    }

    @Nullable
    public static BlockPos getCoordinate(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(NBT_RECEIVER_X) && tag.contains(NBT_RECEIVER_Y) && tag.contains(NBT_RECEIVER_Z)) {
                return new BlockPos(tag.getInt(NBT_RECEIVER_X), tag.getInt(NBT_RECEIVER_Y), tag.getInt(NBT_RECEIVER_Z));
            }
        }
        return null;
    }

    @Nullable
    public static ResourceKey<Level> getDimension(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(NBT_DIMENSION)) {
                ResourceLocation name = ResourceLocation.tryParse(tag.getString(NBT_DIMENSION));
                if (name != null) {
                    return ResourceKey.create(Registries.DIMENSION, name);
                }
            }
        }
        return null;
    }

    @Nullable
    public static Network getNetwork(Level level, ItemStack stack) {
        if (level.isClientSide()) {
            return null;
        }

        BlockPos pos = getCoordinate(stack);
        ResourceKey<Level> dim = getDimension(stack);

        if (pos == null || dim == null) {
            CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
            MoBackup.LOGGER.warn("[MoBackup-Debug] RSBridge: 物品未綁定座標或 NBT 丟失！CustomData 內容: {}", (cd != null ? cd.copyTag() : "null"));
            return null;
        }

        if (level.getServer() == null) {
            MoBackup.LOGGER.warn("[MoBackup-Debug] RSBridge: Server 實例為 null");
            return null;
        }

        ServerLevel serverLevel = level.getServer().getLevel(dim);
        if (serverLevel == null) {
            MoBackup.LOGGER.warn("[MoBackup-Debug] RSBridge: 找不到目標維度 -> {}", dim.location());
            return null;
        }

        if (!serverLevel.isLoaded(pos)) {
            MoBackup.LOGGER.warn("[MoBackup-Debug] RSBridge: 目標區塊未載入！pos: {}, dim: {}", pos, dim.location());
            return null;
        }

        Network network = getRsNetworkAt(serverLevel, pos);
        if (network == null) {
            BlockEntity be = serverLevel.getBlockEntity(pos);
            MoBackup.LOGGER.warn("[MoBackup-Debug] RSBridge: 目標座標無效！BlockEntity: {}", (be != null ? be.getClass().getName() : "null (空氣或非方塊實體)"));
        }
        return network;
    }

    @Nullable
    public static Network getRsNetworkAt(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }

        // 1. 最精準：使用 RS 2.x 官方專門為卡片/物品遠端讀取設計的介面
        if (be instanceof NetworkItemTargetBlockEntity targetBe) {
            Network network = targetBe.getNetworkForItem();
            if (network != null) {
                MoBackup.LOGGER.info("[MoBackup-Debug] RSBridge: 成功透過 NetworkItemTargetBlockEntity.getNetworkForItem() 取得 Network！");
                return network;
            }
        }

        // 2. 後備：標準 NetworkNodeContainerProvider 容器遍歷
        if (be instanceof NetworkNodeContainerProvider provider) {
            for (NetworkNodeContainer container : provider.getContainers()) {
                NetworkNode node = container.getNode();
                if (node != null && node.getNetwork() != null) {
                    return node.getNetwork();
                }
            }
        }

        MoBackup.LOGGER.warn("[MoBackup-Debug] RSBridge: 座標 {} 上的方塊 ({}) 回傳 Network 為 null (請確認方塊是否通電且連至控制器)！", pos, be.getClass().getSimpleName());
        return null;
    }
}