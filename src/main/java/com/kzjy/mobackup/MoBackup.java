/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup;

import com.kzjy.mobackup.registry.ModCreativeModeTabs;
import com.kzjy.mobackup.registry.ModItems;
import com.mojang.logging.LogUtils;
// import com.refinedmods.refinedstorage.api.network.Network;
// import com.refinedmods.refinedstorage.api.network.node.NetworkNode;
// import com.refinedmods.refinedstorage.api.network.node.container.NetworkNodeContainer;
// import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;

// import net.minecraft.core.BlockPos;
// import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
import org.slf4j.Logger;

// import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;

@Mod(MoBackup.MOD_ID)
@SuppressWarnings("null")
public class MoBackup {
    public static final String MOD_ID = "mobackup";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MoBackup(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC, "MoreBackpackUpgrades-common.toml");

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
            // UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_DEPOSIT_UPGRADE.getId()),
            //         DIMENSIONAL_DEPOSIT_TYPE);
        });
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> DIMENSIONAL_PICKUP_TYPE = new UpgradeContainerType<>(
            ContentsFilteredUpgradeContainer::new);
    public static final UpgradeContainerType<net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper, MagnetUpgradeContainer> DIMENSIONAL_MAGNET_TYPE = new UpgradeContainerType<>(
            MagnetUpgradeContainer::new);
    public static final UpgradeContainerType<DepositUpgradeWrapper, DepositUpgradeContainer> DIMENSIONAL_DEPOSIT_TYPE = new UpgradeContainerType<>(
            DepositUpgradeContainer::new);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPickupPre(ItemEntityPickupEvent.Pre event) {
        com.kzjy.mobackup.core.PickupContext.push(event.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPickupPost(ItemEntityPickupEvent.Post event) {
        com.kzjy.mobackup.core.PickupContext.pop();
    }

    @SuppressWarnings("removal")
    @EventBusSubscriber(modid = com.kzjy.mobackup.MoBackup.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void clientSetup(final FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager.registerTab(
                        com.kzjy.mobackup.MoBackup.DIMENSIONAL_PICKUP_TYPE,
                        (net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer<net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper> uc,
                                net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position p,
                                net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase<?> s) ->
                                new net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeTab.Advanced(
                                        uc, p, s,
                                        net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedPickupUpgrade.slotsInRow.get(),
                                        net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE));

                net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager.registerTab(
                        com.kzjy.mobackup.MoBackup.DIMENSIONAL_MAGNET_TYPE,
                        (net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer uc,
                                net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position p,
                                net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase<?> s) ->
                                new net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeTab.Advanced(
                                        uc, p, s,
                                        net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedMagnetUpgrade.slotsInRow.get(),
                                        net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE));

                net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager.registerTab(
                        com.kzjy.mobackup.MoBackup.DIMENSIONAL_DEPOSIT_TYPE,
                        (net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeContainer uc,
                                net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position p,
                                net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase<?> s) ->
                                new net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeTab.Advanced(uc, p, s));
            });
        }
    }
}