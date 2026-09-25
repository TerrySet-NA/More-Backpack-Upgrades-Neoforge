/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import com.kzjy.mobackup.item.IRSLinkedItem;
import com.kzjy.mobackup.registry.ModDataComponents;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.energy.EnergyNetworkComponent;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.api.network.node.NetworkNode;
import com.refinedmods.refinedstorage.api.network.node.container.NetworkNodeContainer;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.api.network.security.SecurityActor;
import com.refinedmods.refinedstorage.api.network.security.SecurityNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemPlayerValidator;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemTargetBlockEntity;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class RSBridge {

    private static final int NETWORK_CACHE_TICKS = 20;

    private record NetworkCacheKey(ResourceKey<Level> dim, BlockPos pos, @Nullable UUID playerUuid, @Nullable Permission perm) {}
    private record CachedNetworkEntry(@Nullable Network network, long timestamp) {}
    private static final Map<NetworkCacheKey, CachedNetworkEntry> CACHE_POOL = new ConcurrentHashMap<>();

    private static final SecurityActor EMPTY_ACTOR = new SecurityActor() {};

    // =========================================================================
    // 綁定座標讀寫 (使用原生 DataComponent<GlobalPos> destination)
    // =========================================================================

    public static void saveCoordinate(ItemStack stack, Level level, BlockPos pos) {
        stack.set(ModDataComponents.DESTINATION.get(), GlobalPos.of(level.dimension(), pos));
    }

    @Nullable
    public static GlobalPos getBoundTarget(ItemStack stack) {
        return stack.get(ModDataComponents.DESTINATION.get());
    }

    @Nullable
    public static BlockPos getCoordinate(ItemStack stack) {
        GlobalPos target = getBoundTarget(stack);
        return target != null ? target.pos() : null;
    }

    @Nullable
    public static ResourceKey<Level> getDimension(ItemStack stack) {
        GlobalPos target = getBoundTarget(stack);
        return target != null ? target.dimension() : null;
    }

    public static boolean isDimensional(ItemStack stack) {
        if (stack.has(ModDataComponents.IS_DIMENSIONAL.get())) {
            return Boolean.TRUE.equals(stack.get(ModDataComponents.IS_DIMENSIONAL.get()));
        }
        if (stack.getItem() instanceof IRSLinkedItem linked) {
            return linked.isDimensional();
        }
        return true;
    }

    // =========================================================================
    // 網路解析與驗證 (含精確距離/範圍檢測)
    // =========================================================================

    @Nullable
    public static Network getNetwork(Level level, ItemStack stack) {
        return getNetwork(level, stack, null, (BlockPos) null, null);
    }

    @Nullable
    public static Network getNetwork(Level level, ItemStack stack, BlockPos currentBlockPos) {
        return getNetwork(level, stack, null, currentBlockPos, null);
    }

    @Nullable
    public static Network getNetwork(Level level, ItemStack stack, @Nullable Player player) {
        return getNetwork(level, stack, player, (BlockPos) null, null);
    }

    @Nullable
    public static Network getNetwork(Level level, ItemStack stack, @Nullable Player player, @Nullable Permission requiredPerm) {
        return getNetwork(level, stack, player, (BlockPos) null, requiredPerm);
    }

    @Nullable
    public static Network getNetwork(Level level, ItemStack stack, BlockPos currentBlockPos, @Nullable Permission requiredPerm) {
        return getNetwork(level, stack, null, currentBlockPos, requiredPerm);
    }

    @Nullable
    public static Network getNetwork(Level level, ItemStack stack, @Nullable Player player, @Nullable BlockPos currentBlockPos, @Nullable Permission requiredPerm) {
        if (level.isClientSide() || stack.isEmpty()) return null;

        GlobalPos target = getBoundTarget(stack);
        if (target == null) return null;

        BlockPos targetPos = target.pos();
        ResourceKey<Level> targetDim = target.dimension();

        // 1. 維度檢測：網路版 (Network) 嚴格禁止跨維度連線
        boolean dimensional = isDimensional(stack);

        // 2. 解析 RS 網路實體（網路本身狀態可走 20 ticks 快取）
        long gameTime = level.getGameTime();
        UUID playerUuid = player != null ? player.getUUID() : null;
        NetworkCacheKey cacheKey = new NetworkCacheKey(targetDim, targetPos, playerUuid, requiredPerm);

        CachedNetworkEntry entry = CACHE_POOL.get(cacheKey);
        Network resolvedNetwork;
        if (entry != null && gameTime >= entry.timestamp() && (gameTime - entry.timestamp() < NETWORK_CACHE_TICKS)) {
            resolvedNetwork = entry.network();
        } else {
            resolvedNetwork = resolveAndValidateNetwork(level, targetDim, targetPos, player, requiredPerm);
            if (resolvedNetwork != null) {
                CACHE_POOL.put(cacheKey, new CachedNetworkEntry(resolvedNetwork, gameTime));
            }
            if (CACHE_POOL.size() > 256) {
                CACHE_POOL.entrySet().removeIf(e -> (gameTime - e.getValue().timestamp()) > 100);
            }
        }

        if (resolvedNetwork == null) {
            return null;
        }

        // 3. 🎯 核心距離檢測：若是「網路版 (Network)」，必須即時檢測是否在無線發射器範圍內！
        if (!dimensional) {
            Vec3 posToCheck = player != null ? player.position() : (currentBlockPos != null ? Vec3.atCenterOf(currentBlockPos) : null);;
            if (posToCheck == null && player != null) {
                posToCheck = player.position();
            }
            if (posToCheck == null && PickupContext.current() != null) {
                posToCheck = PickupContext.current().position();
            }

            // 網路版若拿不到當前操作位置，或已走出 RS 無線發射器覆蓋半徑，直接斷線拒絕！
            if (posToCheck == null || !isInWirelessRange(resolvedNetwork, level.dimension(), posToCheck)) {
                return null;
            }
        }

        return resolvedNetwork;
    }

    @Nullable
    private static Network resolveAndValidateNetwork(Level level, ResourceKey<Level> dim, BlockPos pos, @Nullable Player player, @Nullable Permission requiredPerm) {
        if (level.getServer() == null) return null;
        ServerLevel targetLevel = level.getServer().getLevel(dim);
        if (targetLevel == null || !targetLevel.isLoaded(pos)) return null;

        Network network = getRsNetworkAt(targetLevel, pos);
        if (network == null || !isNetworkPowered(network)) return null;

        if (!hasPermission(network, player, requiredPerm)) return null;
        return network;
    }

    public static boolean validateClickedNetwork(@Nullable Network network, @Nullable Player player, @Nullable Permission requiredPerm) {
        if (network == null || !isNetworkPowered(network)) return false;
        return hasPermission(network, player, requiredPerm);
    }

    public static boolean isNetworkPowered(@Nullable Network network) {
        if (network == null) return false;
        if (!RefinedStorageApi.INSTANCE.isEnergyRequired()) return true;
        EnergyNetworkComponent energy = network.getComponent(EnergyNetworkComponent.class);
        return energy != null && energy.getStored() > 0;
    }

    /**
     * 檢測目標座標是否在 RS 無線發射器（Wireless Transmitter）覆蓋範圍內
     */
    public static boolean isInWirelessRange(@Nullable Network network, ResourceKey<Level> currentDim, Vec3 currentPos) {
        if (network == null) return false;
        NetworkItemPlayerValidator.PlayerCoordinates coordinates = new NetworkItemPlayerValidator.PlayerCoordinates(
                currentDim, currentPos
        );
        GraphNetworkComponent graph = network.getComponent(GraphNetworkComponent.class);
        if (graph == null) return false;
        return graph.getContainers(NetworkItemPlayerValidator.class).stream().anyMatch(validator -> validator.isValid(coordinates));
    }

    public static boolean hasPermission(@Nullable Network network, @Nullable Player player, @Nullable Permission permission) {
        if (network == null) return false;
        if (permission == null) return true;

        if (player instanceof ServerPlayer serverPlayer) {
            return SecurityHelper.isAllowed(serverPlayer, permission, network);
        }

        SecurityNetworkComponent security = network.getComponent(SecurityNetworkComponent.class);
        if (security != null) {
            return security.isAllowed(permission, EMPTY_ACTOR);
        }

        return true;
    }

    public static boolean canInsert(@Nullable Network network, @Nullable Player player) {
        return hasPermission(network, player, BuiltinPermission.INSERT);
    }

    public static boolean canExtract(@Nullable Network network, @Nullable Player player) {
        return hasPermission(network, player, BuiltinPermission.EXTRACT);
    }

    @Nullable
    public static Network getRsNetworkAt(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        if (be instanceof NetworkItemTargetBlockEntity targetBe) {
            Network network = targetBe.getNetworkForItem();
            if (network != null) return network;
        }
        if (be instanceof NetworkNodeContainerProvider provider) {
            for (NetworkNodeContainer container : provider.getContainers()) {
                NetworkNode node = container.getNode();
                if (node != null && node.getNetwork() != null) return node.getNetwork();
            }
        }
        return null;
    }

    public static Actor getActor(@Nullable Player player) {
        return player != null ? new PlayerActor(player) : Actor.EMPTY;
    }
}