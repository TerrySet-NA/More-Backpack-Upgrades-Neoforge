/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup;

import java.util.List;
import java.util.Objects;

import com.kzjy.mobackup.client.gui.*;
import com.kzjy.mobackup.core.PickupContext;
import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.item.IRSLinkedItem;
import com.kzjy.mobackup.registry.ModCreativeModeTabs;
import com.kzjy.mobackup.registry.ModDataComponents;
import com.kzjy.mobackup.registry.ModItems;
import com.kzjy.mobackup.wrapper.DimensionalDepositUpgradeWrapper;
import com.kzjy.mobackup.wrapper.DimensionalRestockUpgradeWrapper;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeWrapper;

@Mod(MoBackup.MOD_ID)
public class MoBackup {
    public static final String MOD_ID = "mobackup";

    public MoBackup(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 8 個次元版註冊
            registerUpgrade(ModItems.DIMENSIONAL_MAGNET_UPGRADE.getId(), MAGNET_TYPE);
            registerUpgrade(ModItems.DIMENSIONAL_PICKUP_UPGRADE.getId(), PICKUP_TYPE);
            registerUpgrade(ModItems.DIMENSIONAL_DEPOSIT_UPGRADE.getId(), DEPOSIT_TYPE);
            registerUpgrade(ModItems.DIMENSIONAL_FEEDING_UPGRADE.getId(), FEEDING_TYPE);
            registerUpgrade(ModItems.DIMENSIONAL_REFILL_UPGRADE.getId(), REFILL_TYPE);
            registerUpgrade(ModItems.DIMENSIONAL_RESTOCK_UPGRADE.getId(), RESTOCK_TYPE);
            registerUpgrade(ModItems.DIMENSIONAL_PUMP_UPGRADE.getId(), PUMP_TYPE);
            registerUpgrade(ModItems.DIMENSIONAL_ALCHEMY_UPGRADE.getId(), ALCHEMY_TYPE);

            // 8 個網路版共用完全相同的 ContainerType
            registerUpgrade(ModItems.NETWORK_MAGNET_UPGRADE.getId(), MAGNET_TYPE);
            registerUpgrade(ModItems.NETWORK_PICKUP_UPGRADE.getId(), PICKUP_TYPE);
            registerUpgrade(ModItems.NETWORK_DEPOSIT_UPGRADE.getId(), DEPOSIT_TYPE);
            registerUpgrade(ModItems.NETWORK_FEEDING_UPGRADE.getId(), FEEDING_TYPE);
            registerUpgrade(ModItems.NETWORK_REFILL_UPGRADE.getId(), REFILL_TYPE);
            registerUpgrade(ModItems.NETWORK_RESTOCK_UPGRADE.getId(), RESTOCK_TYPE);
            registerUpgrade(ModItems.NETWORK_PUMP_UPGRADE.getId(), PUMP_TYPE);
            registerUpgrade(ModItems.NETWORK_ALCHEMY_UPGRADE.getId(), ALCHEMY_TYPE);
        });
    }

    private static void registerUpgrade(ResourceLocation id, UpgradeContainerType<?, ?> type) {
        UpgradeContainerRegistry.register(Objects.requireNonNull(id), type);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    // 共通 ContainerType (共 8 個)
    public static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> PICKUP_TYPE =
            new UpgradeContainerType<>(ContentsFilteredUpgradeContainer::new);
    public static final UpgradeContainerType<MagnetUpgradeWrapper, MagnetUpgradeContainer> MAGNET_TYPE =
            new UpgradeContainerType<>(MagnetUpgradeContainer::new);
    public static final UpgradeContainerType<DepositUpgradeWrapper, DepositUpgradeContainer> DEPOSIT_TYPE =
            new UpgradeContainerType<>(DepositUpgradeContainer::new);
    public static final UpgradeContainerType<FeedingUpgradeWrapper, FeedingUpgradeContainer> FEEDING_TYPE =
            new UpgradeContainerType<>(FeedingUpgradeContainer::new);
    public static final UpgradeContainerType<RefillUpgradeWrapper, RefillUpgradeContainer> REFILL_TYPE =
            new UpgradeContainerType<>(RefillUpgradeContainer::new);
    public static final UpgradeContainerType<RestockUpgradeWrapper, ContentsFilteredUpgradeContainer<RestockUpgradeWrapper>> RESTOCK_TYPE =
            new UpgradeContainerType<>(ContentsFilteredUpgradeContainer::new);
    public static final UpgradeContainerType<PumpUpgradeWrapper, PumpUpgradeContainer> PUMP_TYPE =
            new UpgradeContainerType<>(PumpUpgradeContainer::new);
    public static final UpgradeContainerType<AlchemyUpgradeWrapper, AlchemyUpgradeContainer> ALCHEMY_TYPE =
            new UpgradeContainerType<>(AlchemyUpgradeContainer::new);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPickupPre(ItemEntityPickupEvent.Pre event) {
        PickupContext.push(event.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPickupPost(ItemEntityPickupEvent.Post event) {
        PickupContext.pop();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        if (player == null || !player.isShiftKeyDown()) return;

        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof BackpackItem)) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (level.isClientSide()) {
            if (level.getBlockEntity(pos) != null) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            Network network = RSBridge.getRsNetworkAt(serverLevel, pos);
            if (network == null || !RSBridge.hasPermission(network, player, BuiltinPermission.INSERT)) {
                return;
            }

            IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(held);
            if (backpackWrapper == null) return;

            boolean handled = false;

            // 卸貨升級執行
            for (DimensionalDepositUpgradeWrapper depositWrapper : backpackWrapper.getUpgradeHandler().getWrappersThatImplement(DimensionalDepositUpgradeWrapper.class)) {
                depositWrapper.performDepositAndNotify(network, player);
                handled = true;
            }

            // 取貨升級執行
            for (DimensionalRestockUpgradeWrapper restockWrapper : backpackWrapper.getUpgradeHandler().getWrappersThatImplement(DimensionalRestockUpgradeWrapper.class)) {
                restockWrapper.performRestockAndNotify(network, player);
                handled = true;
            }

            if (handled) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void clientSetup(final FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                UpgradeGuiManager.registerTab(PICKUP_TYPE, DimensionalPickupUpgradeTab::new);
                UpgradeGuiManager.registerTab(MAGNET_TYPE, DimensionalMagnetUpgradeTab::new);
                UpgradeGuiManager.registerTab(DEPOSIT_TYPE, DimensionalDepositUpgradeTab::new);
                UpgradeGuiManager.registerTab(FEEDING_TYPE, DimensionalFeedingUpgradeTab::new);
                UpgradeGuiManager.registerTab(REFILL_TYPE, DimensionalRefillUpgradeTab::new);
                UpgradeGuiManager.registerTab(RESTOCK_TYPE, DimensionalRestockUpgradeTab::new);
                UpgradeGuiManager.registerTab(PUMP_TYPE, DimensionalPumpUpgradeTab::new);
                UpgradeGuiManager.registerTab(ALCHEMY_TYPE, DimensionalAlchemyUpgradeTab::new);

                // 2. 註冊模型 Predicate: mobackup:linked
                registerLinkedItemProperties();
            });
        }

        private static void registerLinkedItemProperties() {
            for (var entry : ModItems.ITEMS.getEntries()) {
                Item item = entry.get();
                if (item instanceof IRSLinkedItem) {
                    ItemProperties.register(
                            item,
                            MoBackup.rl("linked"),
                            (stack, level, entity, seed) -> RSBridge.getBoundTarget(stack) != null ? 1.0F : 0.0F
                    );
                }
            }
        }
    }
}