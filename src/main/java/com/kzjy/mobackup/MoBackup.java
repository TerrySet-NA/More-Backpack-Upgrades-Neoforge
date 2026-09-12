/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup;

import com.kzjy.mobackup.client.gui.DimensionalAlchemyUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalDepositUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalFeedingUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalMagnetUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalPickupUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalPumpUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalRefillUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalRestockUpgradeTab;
import com.kzjy.mobackup.registry.ModCreativeModeTabs;
import com.kzjy.mobackup.registry.ModItems;
// import com.mojang.logging.LogUtils;
// import com.refinedmods.refinedstorage.api.network.Network;
// import com.refinedmods.refinedstorage.api.network.node.NetworkNode;
// import com.refinedmods.refinedstorage.api.network.node.container.NetworkNodeContainer;
// import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.api.network.Network;

import net.minecraft.core.BlockPos;
// import net.minecraft.network.chat.Component;
// import net.minecraft.core.BlockPos;
// import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
// import net.minecraft.server.level.ServerLevel;
// import net.minecraft.world.InteractionResult;
// import net.minecraft.world.item.ItemStack;
// import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
// import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.common.NeoForge;
// import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
// import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

import java.util.Objects;
// import javax.annotation.Nullable;
// import org.slf4j.Logger;

import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeContainer;
// import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeTab;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeContainer;
// import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeTab;
// import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeTab;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
// import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeContainer;
// import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer;
// import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeTab;
// import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
// import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeContainer;
// import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeContainer;

@Mod(MoBackup.MOD_ID)
@SuppressWarnings("null")
public class MoBackup {
    public static final String MOD_ID = "mobackup";
    // public static final Logger LOGGER = LogUtils.getLogger();

    public MoBackup(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, com.kzjy.mobackup.Config.COMMON_SPEC, "MoreBackpackUpgrades-common.toml");

        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_MAGNET_UPGRADE.getId()),
                    DIMENSIONAL_MAGNET_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_PICKUP_UPGRADE.getId()),
                    DIMENSIONAL_PICKUP_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_DEPOSIT_UPGRADE.getId()),
                    DIMENSIONAL_DEPOSIT_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_FEEDING_UPGRADE.getId()),
                    DIMENSIONAL_FEEDING_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_REFILL_UPGRADE.getId()),
                    DIMENSIONAL_REFILL_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_RESTOCK_UPGRADE.getId()),
                    DIMENSIONAL_RESTOCK_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_PUMP_UPGRADE.getId()),
                    DIMENSIONAL_PUMP_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_ALCHEMY_UPGRADE.getId()),
                    DIMENSIONAL_ALCHEMY_TYPE);
        });
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> DIMENSIONAL_PICKUP_TYPE = new UpgradeContainerType<>(
            ContentsFilteredUpgradeContainer::new);
    public static final UpgradeContainerType<net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper, net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer> DIMENSIONAL_MAGNET_TYPE =
            new UpgradeContainerType<>(MagnetUpgradeContainer::new);
    public static final UpgradeContainerType<DepositUpgradeWrapper, DepositUpgradeContainer> DIMENSIONAL_DEPOSIT_TYPE = new UpgradeContainerType<>(
            DepositUpgradeContainer::new);
    public static final UpgradeContainerType<net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper, net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeContainer> DIMENSIONAL_FEEDING_TYPE = new UpgradeContainerType<>(
            net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeContainer::new);
    public static final UpgradeContainerType<net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper, net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeContainer> DIMENSIONAL_REFILL_TYPE = new UpgradeContainerType<>(
            net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeContainer::new);
    public static final UpgradeContainerType<net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper, net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer<net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper>> DIMENSIONAL_RESTOCK_TYPE =
            new UpgradeContainerType<>(net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer::new);
    public static final UpgradeContainerType<net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeWrapper, net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeContainer> DIMENSIONAL_PUMP_TYPE =
            new UpgradeContainerType<>(net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeContainer::new);
    public static final UpgradeContainerType<net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper, net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeContainer> DIMENSIONAL_ALCHEMY_TYPE =
            new UpgradeContainerType<>(net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeContainer::new);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPickupPre(ItemEntityPickupEvent.Pre event) {
        com.kzjy.mobackup.core.PickupContext.push(event.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPickupPost(ItemEntityPickupEvent.Post event) {
        com.kzjy.mobackup.core.PickupContext.pop();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player == null || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        // 檢查是否點擊 RS 2.x 方塊
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
        boolean isRsBlock = be instanceof com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider
                || be instanceof com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemTargetBlockEntity
                || level.getBlockState(pos).getBlock().getClass().getName().contains("refinedstorage");

        if (!isRsBlock) {
            return;
        }

        // 客戶端攔截預測，防破圖與幽靈方塊
        if (level.isClientSide()) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        // 伺服器端依序執行：先卸貨，後取貨
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper backpackWrapper =
                    net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper.fromStack(held);

            Network network = com.kzjy.mobackup.core.RSBridge.getRsNetworkAt(serverLevel, pos);
            boolean handled = false;

            if (network != null) {
                // 1. 先執行次元卸貨（清空戰利品）
                java.util.List<com.kzjy.mobackup.wrapper.DimensionalDepositUpgradeWrapper> depositWrappers =
                        backpackWrapper.getUpgradeHandler().getWrappersThatImplement(com.kzjy.mobackup.wrapper.DimensionalDepositUpgradeWrapper.class);
                for (com.kzjy.mobackup.wrapper.DimensionalDepositUpgradeWrapper depositWrapper : depositWrappers) {
                    depositWrapper.performDepositAndNotify(network, player);
                    handled = true;
                }

                // 2. 後執行次元取貨（補齊物資）
                java.util.List<com.kzjy.mobackup.wrapper.DimensionalRestockUpgradeWrapper> restockWrappers =
                        backpackWrapper.getUpgradeHandler().getWrappersThatImplement(com.kzjy.mobackup.wrapper.DimensionalRestockUpgradeWrapper.class);
                for (com.kzjy.mobackup.wrapper.DimensionalRestockUpgradeWrapper restockWrapper : restockWrappers) {
                    restockWrapper.performRestockAndNotify(network, player);
                    handled = true;
                }
            }

            if (handled) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    @SuppressWarnings("removal")
    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void clientSetup(final FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_PICKUP_TYPE,
                        (ContentsFilteredUpgradeContainer<PickupUpgradeWrapper> uc,
                                Position p,
                                StorageScreenBase<?> s) ->
                                new DimensionalPickupUpgradeTab(
                                        uc, p, s,
                                        Config.SERVER.advancedPickupUpgrade.slotsInRow.get(),
                                        SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_MAGNET_TYPE,
                        (MagnetUpgradeContainer uc,
                        Position p,
                        StorageScreenBase<?> s) ->
                                new DimensionalMagnetUpgradeTab(
                                        uc, p, s,
                                        Config.SERVER.advancedMagnetUpgrade.slotsInRow.get(),
                                        SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_DEPOSIT_TYPE,
                        (DepositUpgradeContainer uc,
                                Position p,
                                StorageScreenBase<?> s) ->
                                new DimensionalDepositUpgradeTab(uc, p, s));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_FEEDING_TYPE,
                        (FeedingUpgradeContainer uc,
                        Position p,
                        StorageScreenBase<?> s) ->
                                new DimensionalFeedingUpgradeTab(
                                        uc, p, s,
                                        Config.SERVER.advancedFeedingUpgrade.slotsInRow.get()));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_REFILL_TYPE,
                        (RefillUpgradeContainer uc,
                        Position p,
                        StorageScreenBase<?> s) ->
                                new DimensionalRefillUpgradeTab(
                                        uc, p, s,
                                        Config.SERVER.advancedRefillUpgrade.slotsInRow.get()));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_RESTOCK_TYPE,
                        (ContentsFilteredUpgradeContainer<RestockUpgradeWrapper> uc,
                        Position p,
                        StorageScreenBase<?> s) ->
                                new DimensionalRestockUpgradeTab(
                                        uc, p, s,
                                        SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE));
                
                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_PUMP_TYPE,
                        (PumpUpgradeContainer uc,
                        Position p,
                        StorageScreenBase<?> s) ->
                                new DimensionalPumpUpgradeTab(uc, p, s));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_ALCHEMY_TYPE,
                        (AlchemyUpgradeContainer uc,
                        Position p,
                        StorageScreenBase<?> s) ->
                                new DimensionalAlchemyUpgradeTab(uc, p, s));
            });
        }
    }
}