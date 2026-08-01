/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

// package com.kzjy.mobackup.registry;

// import com.kzjy.mobackup.MoBackup;
// import com.kzjy.mobackup.core.RSBridge;
// import com.kzjy.mobackup.rs.RSInsertOnlyHandler;
// import com.refinedmods.refinedstorage.api.network.Network;
// import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;

// import net.minecraft.core.registries.BuiltInRegistries;
// import net.minecraft.server.level.ServerLevel;
// import net.minecraft.world.level.block.entity.BlockEntityType;
// import net.neoforged.bus.api.SubscribeEvent;
// import net.neoforged.fml.common.EventBusSubscriber;
// import net.neoforged.neoforge.capabilities.Capabilities;
// import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

// @EventBusSubscriber(modid = MoBackup.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
// public class ModCapabilityRegister {

//     @SuppressWarnings({"unchecked", "rawtypes"})
//     @SubscribeEvent
//     public static void registerCapabilities(RegisterCapabilitiesEvent event) {
//         BuiltInRegistries.BLOCK_ENTITY_TYPE.entrySet().stream()
//                 .filter(entry -> "refinedstorage".equals(entry.getKey().location().getNamespace()))
//                 .forEach(entry -> {
//                     BlockEntityType type = entry.getValue();

//                     event.registerBlockEntity(
//                             Capabilities.ItemHandler.BLOCK,
//                             type,
//                             (blockEntity, side) -> {
//                                 if (blockEntity instanceof NetworkNodeContainerProvider) {
//                                     // 伺服端：取得真正的 RS 網路
//                                     if (blockEntity.getLevel() instanceof ServerLevel serverLevel) {
//                                         Network network = RSBridge.getRsNetworkAt(serverLevel, blockEntity.getBlockPos());
//                                         if (network != null) {
//                                             return new RSInsertOnlyHandler(network, null);
//                                         }
//                                     } 
//                                     // 【關鍵修復】：客戶端也必須回傳 Dummy，告訴精妙背包「這是合法容器」！
//                                     else if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide()) {
//                                         return RSInsertOnlyHandler.CLIENT_DUMMY;
//                                     }
//                                 }
//                                 return null;
//                             }
//                     );
//                 });
//     }
// }