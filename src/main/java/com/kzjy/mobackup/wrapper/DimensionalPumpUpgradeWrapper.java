/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.wrapper;

import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
// import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.CoreFakePlayer;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("null")
public class DimensionalPumpUpgradeWrapper extends PumpUpgradeWrapper implements IPriorityRoutingUpgrade {

    private static final int DID_NOTHING_COOLDOWN_TIME = 40;
    private static final int HAND_INTERACTION_COOLDOWN_TIME = 3;
    private static final int WORLD_INTERACTION_COOLDOWN_TIME = 20;
    private static final int FLUID_HANDLER_INTERACTION_COOLDOWN_TIME = 20;
    private static final int PLAYER_SEARCH_RANGE = 3;
    private static final int PUMP_IN_WORLD_RANGE = 4;
    private static final int PUMP_IN_WORLD_RANGE_SQR = PUMP_IN_WORLD_RANGE * PUMP_IN_WORLD_RANGE;

    private long lastHandActionTime = -1;

    public DimensionalPumpUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    private Network cachedNetwork;
    private long lastNetworkCheckTime = -1;
    private static final int NETWORK_CHECK_INTERVAL = 20;

    private Network getCachedNetwork(Level level) {
        long gameTime = level.getGameTime();
        if (cachedNetwork == null || lastNetworkCheckTime < 0
                || gameTime - lastNetworkCheckTime >= NETWORK_CHECK_INTERVAL) {
            lastNetworkCheckTime = gameTime;
            cachedNetwork = RSBridge.getNetwork(level, getUpgradeStack());
        }
        return cachedNetwork;
    }

    private Boolean networkFirstCache = null;

    @Override
    public boolean isNetworkFirst() {
        if (networkFirstCache == null) {
            CustomData customData = upgrade.get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.contains(TAG_NETWORK_FIRST)) {
                networkFirstCache = customData.copyTag().getBoolean(TAG_NETWORK_FIRST);
            } else {
                networkFirstCache = true; // 預設：RS 網路優先
            }
        }
        return networkFirstCache;
    }

    @Override
    public void setNetworkFirst(boolean networkFirst) {
        this.networkFirstCache = networkFirst;
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, tag -> {
            tag.putBoolean(TAG_NETWORK_FIRST, networkFirst);
        });
        save();
    }

    // ==========================================
    // RS 2.x FluidResource <-> NeoForge FluidStack 轉換
    // ==========================================
    private static FluidResource toResource(FluidStack stack) {
        return new FluidResource(stack.getFluid(), stack.getComponentsPatch());
    }

    private static FluidStack toFluidStack(FluidResource resource, long amount) {
        int safeAmount = (int) Math.min(Integer.MAX_VALUE, amount);
        if (safeAmount <= 0) {
            return FluidStack.EMPTY;
        }
        if (resource.components().isEmpty()) {
            return new FluidStack(resource.fluid(), safeAmount);
        }
        return new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(resource.fluid()), safeAmount, resource.components());
    }

    @Override
    public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (isInCooldown(level)) {
            return;
        }

        IFluidHandler backpackTank = storageWrapper.getFluidHandler().orElse(null);
        Network network = getCachedNetwork(level);

        if (backpackTank == null && network == null) {
            setCooldown(level, DID_NOTHING_COOLDOWN_TIME);
            return;
        }

        Player player = entity instanceof Player p ? p : null;
        DimensionalFluidHandler fluidHandler = new DimensionalFluidHandler(backpackTank, network, player, isNetworkFirst());

        setCooldown(level, tickInternal(fluidHandler, entity, level, pos));
    }

    private int tickInternal(IFluidHandler fluidHandler, @Nullable Entity entity, Level level, BlockPos pos) {
        if (shouldInteractWithHand()) {
            if (entity instanceof Player player) {
                if (handleFluidContainerInHands(player, fluidHandler)) {
                    lastHandActionTime = level.getGameTime();
                    return HAND_INTERACTION_COOLDOWN_TIME;
                }
            } else if (handleFluidContainersInHandsOfNearbyPlayers(level, pos, fluidHandler)) {
                lastHandActionTime = level.getGameTime();
                return HAND_INTERACTION_COOLDOWN_TIME;
            }
        }

        return handleInWorldInteractions(fluidHandler, entity, level, pos)
                .orElseGet(() -> lastHandActionTime + 10 * HAND_INTERACTION_COOLDOWN_TIME > level.getGameTime()
                        ? HAND_INTERACTION_COOLDOWN_TIME
                        : DID_NOTHING_COOLDOWN_TIME);
    }

    private Optional<Integer> handleInWorldInteractions(IFluidHandler fluidHandler, @Nullable Entity entity, Level level, BlockPos pos) {
        if (shouldInteractWithWorld()) {
            Optional<Integer> newCooldown = interactWithWorld(level, pos, fluidHandler, entity);
            if (newCooldown.isPresent()) {
                return newCooldown;
            }
        }
        if (shouldInteractWithFluidHandlers()) {
            return interactWithAttachedFluidHandlers(level, pos, fluidHandler);
        }
        return Optional.empty();
    }

    private Optional<Integer> interactWithAttachedFluidHandlers(Level level, BlockPos pos, IFluidHandler fluidHandler) {
        for (Direction dir : Direction.values()) {
            boolean successful = WorldHelper.getBlockEntity(level, pos.offset(dir.getNormal()))
                    .map(be -> CapabilityHelper.<Boolean>getFromFluidHandler(be, dir.getOpposite(), targetHandler -> {
                        if (isInput()) {
                            return fillFromFluidHandler(targetHandler, fluidHandler, getMaxInOut());
                        } else {
                            return fillFluidHandler(targetHandler, fluidHandler, getMaxInOut());
                        }
                    }, false)).orElse(false);

            if (successful) {
                return Optional.of(FLUID_HANDLER_INTERACTION_COOLDOWN_TIME);
            }
        }
        return Optional.empty();
    }

    private int getMaxInOut() {
        return Math.max(FluidType.BUCKET_VOLUME,
                upgradeItem.getPumpUpgradeConfig().maxInputOutput.get() * storageWrapper.getNumberOfSlotRows() * getAdjustedStackMultiplier(storageWrapper));
    }

    private Optional<Integer> interactWithWorld(Level level, BlockPos pos, IFluidHandler fluidHandler, @Nullable Entity entity) {
        Optional<Player> interactingPlayer = getInteractingPlayer(level, pos, entity);
        if (interactingPlayer.isEmpty()) {
            return Optional.empty();
        }

        Player player = interactingPlayer.get();
        if (isInput()) {
            return fillFromBlockInRange(level, pos, fluidHandler, player);
        } else {
            for (Direction dir : Direction.values()) {
                BlockPos offsetPos = pos.offset(dir.getNormal());
                if (placeFluidInWorld(level, fluidHandler, dir, offsetPos, player)) {
                    return Optional.of(WORLD_INTERACTION_COOLDOWN_TIME);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Player> getInteractingPlayer(Level level, BlockPos pos, @Nullable Entity entity) {
        if (entity instanceof Player player) {
            return Optional.of(player);
        }
        if (level instanceof ServerLevel serverLevel) {
            CoreFakePlayer fakePlayer = CoreFakePlayer.get(serverLevel);
            fakePlayer.setPosition(Vec3.atCenterOf(pos));
            return Optional.of(fakePlayer);
        }
        return Optional.empty();
    }

    private boolean placeFluidInWorld(Level level, IFluidHandler fluidHandler, Direction dir, BlockPos offsetPos, Player player) {
        if (dir != Direction.UP) {
            for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
                FluidStack tankFluid = fluidHandler.getFluidInTank(tank);
                if (!tankFluid.isEmpty() && getFluidFilterLogic().fluidMatches(tankFluid)
                        && WorldHelper.playerMayInteract(player, offsetPos)
                        && isValidForFluidPlacement(level, offsetPos)
                        && FluidUtil.tryPlaceFluid(null, level, InteractionHand.MAIN_HAND, offsetPos, fluidHandler, tankFluid)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isValidForFluidPlacement(Level level, BlockPos offsetPos) {
        BlockState blockState = level.getBlockState(offsetPos);
        return blockState.isAir() || (!blockState.getFluidState().isEmpty() && !blockState.getFluidState().isSource());
    }

    private Optional<Integer> fillFromBlockInRange(Level level, BlockPos basePos, IFluidHandler fluidHandler, Player player) {
        LinkedList<BlockPos> nextPositions = new LinkedList<>();
        Set<BlockPos> searchedPositions = new HashSet<>();
        nextPositions.add(basePos);

        while (!nextPositions.isEmpty()) {
            BlockPos pos = nextPositions.poll();
            if (fillFromBlock(level, pos, fluidHandler, player)) {
                return Optional.of((int) (Math.max(1, Math.sqrt(basePos.distSqr(pos))) * WORLD_INTERACTION_COOLDOWN_TIME));
            }

            for (Direction dir : Direction.values()) {
                BlockPos offsetPos = pos.offset(dir.getNormal());
                if (!searchedPositions.contains(offsetPos)) {
                    searchedPositions.add(offsetPos);
                    if (basePos.distSqr(offsetPos) < PUMP_IN_WORLD_RANGE_SQR) {
                        nextPositions.add(offsetPos);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean fillFromBlock(Level level, BlockPos pos, IFluidHandler fluidHandler, Player player) {
        if (!WorldHelper.playerMayInteract(player, pos)) {
            return false;
        }

        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isEmpty() && fluidState.isSource()) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            IFluidHandler targetFluidHandler;
            if (block instanceof BucketPickup bucketPickup) {
                targetFluidHandler = new BucketPickupHandlerWrapper(player, bucketPickup, level, pos);
            } else {
                Optional<IFluidHandler> handlerOpt = FluidUtil.getFluidHandler(level, pos, null);
                if (handlerOpt.isEmpty()) {
                    return false;
                }
                targetFluidHandler = handlerOpt.get();
            }
            return fillFromFluidHandler(targetFluidHandler, fluidHandler, FluidType.BUCKET_VOLUME);
        }
        return false;
    }

    private boolean handleFluidContainersInHandsOfNearbyPlayers(Level level, BlockPos pos, IFluidHandler fluidHandler) {
        AABB searchBox = new AABB(pos).inflate(PLAYER_SEARCH_RANGE);
        for (Player player : level.players()) {
            if (searchBox.contains(player.getX(), player.getY(), player.getZ()) && handleFluidContainerInHands(player, fluidHandler)) {
                return true;
            }
        }
        return false;
    }

    private boolean handleFluidContainerInHands(Player player, IFluidHandler fluidHandler) {
        return handleFluidContainerInHand(fluidHandler, player, InteractionHand.MAIN_HAND)
                || handleFluidContainerInHand(fluidHandler, player, InteractionHand.OFF_HAND);
    }

    private boolean handleFluidContainerInHand(IFluidHandler fluidHandler, Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        if (itemInHand.getCount() != 1 || itemInHand == storageWrapper.getWrappedStorageStack()) {
            return false;
        }
        return CapabilityHelper.getFromFluidHandler(itemInHand, itemFluidHandler -> {
            if (isInput()) {
                if (fillFromFluidHandler(itemFluidHandler, fluidHandler, FluidType.BUCKET_VOLUME)) {
                    player.setItemInHand(hand, itemFluidHandler.getContainer());
                    return true;
                }
                return false;
            } else {
                if (fillFluidHandler(itemFluidHandler, fluidHandler, FluidType.BUCKET_VOLUME)) {
                    player.setItemInHand(hand, itemFluidHandler.getContainer());
                    return true;
                }
                return false;
            }
        }, false);
    }

    private boolean fillFluidHandler(IFluidHandler targetHandler, IFluidHandler fluidHandler, int maxFill) {
        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            FluidStack tankFluid = fluidHandler.getFluidInTank(tank);
            if (!tankFluid.isEmpty() && getFluidFilterLogic().fluidMatches(tankFluid)
                    && !FluidUtil.tryFluidTransfer(targetHandler, fluidHandler, new FluidStack(tankFluid.getFluid(), maxFill), true).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean fillFromFluidHandler(IFluidHandler sourceHandler, IFluidHandler fluidHandler, int maxDrain) {
        FluidStack containedFluid = sourceHandler.drain(maxDrain, IFluidHandler.FluidAction.SIMULATE);
        if (!containedFluid.isEmpty() && getFluidFilterLogic().fluidMatches(containedFluid)) {
            return !FluidUtil.tryFluidTransfer(fluidHandler, sourceHandler, containedFluid, true).isEmpty();
        }
        return false;
    }

    private class DimensionalFluidHandler implements IFluidHandler {
        private final @Nullable IFluidHandler backpackTank;
        private final @Nullable Network network;
        private final @Nullable Player player;
        private final boolean networkFirst;

        public DimensionalFluidHandler(@Nullable IFluidHandler backpackTank, @Nullable Network network, @Nullable Player player, boolean networkFirst) {
            this.backpackTank = backpackTank;
            this.network = network;
            this.player = player;
            this.networkFirst = networkFirst;
        }

        private List<FluidStack> getAvailableFluids() {
            List<FluidStack> fluids = new ArrayList<>();
            Set<Fluid> seen = new HashSet<>();

            if (networkFirst) {
                addRsFluids(fluids, seen);
                addBackpackFluids(fluids, seen);
            } else {
                addBackpackFluids(fluids, seen);
                addRsFluids(fluids, seen);
            }
            return fluids;
        }

        private void addRsFluids(List<FluidStack> fluids, Set<Fluid> seen) {
            if (network == null) {
                return;
            }
            StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
            if (storage == null) {
                return;
            }

            for (ResourceAmount ra : storage.getAll()) {
                if (ra.amount() > 0 && ra.resource() instanceof FluidResource fr) {
                    FluidStack fs = toFluidStack(fr, ra.amount());
                    if (!fs.isEmpty() && getFluidFilterLogic().fluidMatches(fs) && seen.add(fs.getFluid())) {
                        fluids.add(fs);
                    }
                }
            }
        }

        private void addBackpackFluids(List<FluidStack> fluids, Set<Fluid> seen) {
            if (backpackTank == null) {
                return;
            }
            for (int i = 0; i < backpackTank.getTanks(); i++) {
                FluidStack fs = backpackTank.getFluidInTank(i);
                if (!fs.isEmpty() && getFluidFilterLogic().fluidMatches(fs) && seen.add(fs.getFluid())) {
                    fluids.add(fs.copy());
                }
            }
        }

        @Override
        public int getTanks() {
            return Math.max(1, getAvailableFluids().size());
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            List<FluidStack> fluids = getAvailableFluids();
            return tank >= 0 && tank < fluids.size() ? fluids.get(tank) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return getFluidFilterLogic().fluidMatches(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }

            if (networkFirst) {
                int filledRs = fillRs(resource, action);
                if (filledRs >= resource.getAmount()) {
                    return filledRs;
                }
                int remaining = resource.getAmount() - filledRs;
                int filledBackpack = fillBackpack(resource.copyWithAmount(remaining), action);
                return filledRs + filledBackpack;
            } else {
                int filledBackpack = fillBackpack(resource, action);
                if (filledBackpack >= resource.getAmount()) {
                    return filledBackpack;
                }
                int remaining = resource.getAmount() - filledBackpack;
                int filledRs = fillRs(resource.copyWithAmount(remaining), action);
                return filledBackpack + filledRs;
            }
        }

        private int fillRs(FluidStack stack, FluidAction action) {
            if (network == null || stack.isEmpty()) {
                return 0;
            }
            StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
            if (storage == null) {
                return 0;
            }

            FluidResource resource = toResource(stack);
            Action rsAction = action.execute() ? Action.EXECUTE : Action.SIMULATE;
            Actor actor = player != null ? new PlayerActor(player) : Actor.EMPTY;
            return (int) storage.insert(resource, stack.getAmount(), rsAction, actor);
        }

        private int fillBackpack(FluidStack stack, FluidAction action) {
            return backpackTank != null && !stack.isEmpty() ? backpackTank.fill(stack, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }

            if (networkFirst) {
                FluidStack drainedRs = drainRs(resource, action);
                int needed = resource.getAmount() - drainedRs.getAmount();
                if (needed <= 0) {
                    return drainedRs;
                }
                FluidStack drainedBackpack = drainBackpack(resource.copyWithAmount(needed), action);
                int total = drainedRs.getAmount() + drainedBackpack.getAmount();
                return total > 0 ? resource.copyWithAmount(total) : FluidStack.EMPTY;
            } else {
                FluidStack drainedBackpack = drainBackpack(resource, action);
                int needed = resource.getAmount() - drainedBackpack.getAmount();
                if (needed <= 0) {
                    return drainedBackpack;
                }
                FluidStack drainedRs = drainRs(resource.copyWithAmount(needed), action);
                int total = drainedBackpack.getAmount() + drainedRs.getAmount();
                return total > 0 ? resource.copyWithAmount(total) : FluidStack.EMPTY;
            }
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            for (FluidStack fluid : getAvailableFluids()) {
                if (!fluid.isEmpty()) {
                    FluidStack drained = drain(fluid.copyWithAmount(maxDrain), action);
                    if (!drained.isEmpty()) {
                        return drained;
                    }
                }
            }
            return FluidStack.EMPTY;
        }

        private FluidStack drainRs(FluidStack resource, FluidAction action) {
            if (network == null || resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
            if (storage == null) {
                return FluidStack.EMPTY;
            }

            FluidResource res = toResource(resource);
            Action rsAction = action.execute() ? Action.EXECUTE : Action.SIMULATE;
            Actor actor = player != null ? new PlayerActor(player) : Actor.EMPTY;
            long extracted = storage.extract(res, resource.getAmount(), rsAction, actor);
            return extracted > 0 ? resource.copyWithAmount((int) extracted) : FluidStack.EMPTY;
        }

        private FluidStack drainBackpack(FluidStack resource, FluidAction action) {
            return backpackTank != null && !resource.isEmpty() ? backpackTank.drain(resource, action) : FluidStack.EMPTY;
        }
    }
}