package com.kzjy.mobackup.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.energy.EnergyNetworkComponent;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.api.network.node.NetworkNode;
import com.refinedmods.refinedstorage.api.network.node.container.NetworkNodeContainer;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.api.network.security.SecurityActor;
import com.refinedmods.refinedstorage.api.network.security.SecurityNetworkComponent;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemPlayerValidator;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemTargetBlockEntity;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class RSBridge {

    private static final String NBT_RECEIVER_X = "ReceiverX";
    private static final String NBT_RECEIVER_Y = "ReceiverY";
    private static final String NBT_RECEIVER_Z = "ReceiverZ";
    private static final String NBT_DIMENSION = "Dimension";
    private static final int NETWORK_CACHE_TICKS = 20;

    private record NetworkCacheKey(ResourceKey<Level> dim, BlockPos pos, @Nullable UUID playerUuid, @Nullable Permission perm) {}
    private record CachedNetworkEntry(@Nullable Network network, long timestamp) {}
    private static final Map<NetworkCacheKey, CachedNetworkEntry> CACHE_POOL = new ConcurrentHashMap<>();

    private static final SecurityActor EMPTY_ACTOR = new SecurityActor() {};

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
                if (name != null) return ResourceKey.create(Registries.DIMENSION, name);
            }
        }
        return null;
    }

    @Nullable
    public static Network getNetwork(Level level, ItemStack stack) {
        return getNetwork(level, stack, null, null);
    }

    @Nullable
    public static Network getNetwork(Level level, ItemStack stack, @Nullable Player player) {
        return getNetwork(level, stack, player, null);
    }

    @Nullable
    public static Network getNetwork(Level level, ItemStack stack, @Nullable Player player, @Nullable Permission requiredPerm) {
        if (level.isClientSide() || stack.isEmpty()) return null;

        BlockPos pos = getCoordinate(stack);
        ResourceKey<Level> dim = getDimension(stack);
        if (pos == null || dim == null) return null;

        long gameTime = level.getGameTime();
        UUID playerUuid = player != null ? player.getUUID() : null;
        NetworkCacheKey cacheKey = new NetworkCacheKey(dim, pos, playerUuid, requiredPerm);

        CachedNetworkEntry entry = CACHE_POOL.get(cacheKey);
        if (entry != null && gameTime >= entry.timestamp() && (gameTime - entry.timestamp() < NETWORK_CACHE_TICKS)) {
            return entry.network();
        }

        Network resolvedNetwork = resolveAndValidateNetwork(level, dim, pos, player, requiredPerm);
        if (resolvedNetwork != null) {
            CACHE_POOL.put(cacheKey, new CachedNetworkEntry(resolvedNetwork, gameTime));
        }

        if (CACHE_POOL.size() > 256) {
            CACHE_POOL.entrySet().removeIf(e -> (gameTime - e.getValue().timestamp()) > 100);
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

        // if (!isInWirelessRange(network, dim, pos)) {
        //     return null;
        // }

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

    public static boolean isInWirelessRange(@Nullable Network network, ResourceKey<Level> dim, BlockPos pos) {
        if (network == null) return false;
        NetworkItemPlayerValidator.PlayerCoordinates coordinates = new NetworkItemPlayerValidator.PlayerCoordinates(
                dim, new Vec3(pos.getX(), pos.getY(), pos.getZ())
        );
        GraphNetworkComponent graph = network.getComponent(GraphNetworkComponent.class);
        if (graph == null) return false;
        return graph.getContainers(NetworkItemPlayerValidator.class).stream().anyMatch(validator -> validator.isValid(coordinates));
    }

    // =========================================================================
    // 不提前截斷，直接進入 isAllowed 判定
    // =========================================================================
    public static boolean hasPermission(@Nullable Network network, @Nullable Player player, @Nullable Permission permission) {
        if (network == null) return false;
        if (permission == null) return true;

        if (player instanceof ServerPlayer serverPlayer) {
            return SecurityHelper.isAllowed(serverPlayer, permission, network);
        }

        // 當 player == null 時，直接進入 RS2 的 isAllowed 進行判定
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
}