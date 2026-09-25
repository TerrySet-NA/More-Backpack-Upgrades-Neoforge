/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.registry;

import com.kzjy.mobackup.MoBackup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MoBackup.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MO_BACKUP_TAB = CREATIVE_MODE_TABS.register("mobackup_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> {
                        ItemStack iconStack = new ItemStack(ModItems.DIMENSIONAL_MAGNET_UPGRADE.get());
                        iconStack.set(ModDataComponents.DESTINATION.get(), GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO));
                        return iconStack;
                    })
                    .title(Component.translatable("creativetab.mobackup_tab"))
                    .displayItems((parameters, output) -> {
                        // 1. 磁吸 (Magnet)
                        output.accept(ModItems.NETWORK_MAGNET_UPGRADE.get());
                        output.accept(ModItems.DIMENSIONAL_MAGNET_UPGRADE.get());

                        // 2. 拾取 (Pickup)
                        output.accept(ModItems.NETWORK_PICKUP_UPGRADE.get());
                        output.accept(ModItems.DIMENSIONAL_PICKUP_UPGRADE.get());

                        // 3. 卸貨 (Deposit)
                        output.accept(ModItems.NETWORK_DEPOSIT_UPGRADE.get());
                        output.accept(ModItems.DIMENSIONAL_DEPOSIT_UPGRADE.get());

                        // 4. 餵食 (Feeding)
                        output.accept(ModItems.NETWORK_FEEDING_UPGRADE.get());
                        output.accept(ModItems.DIMENSIONAL_FEEDING_UPGRADE.get());

                        // 5. 玩家補給 (Refill)
                        output.accept(ModItems.NETWORK_REFILL_UPGRADE.get());
                        output.accept(ModItems.DIMENSIONAL_REFILL_UPGRADE.get());

                        // 6. 取貨 (Restock)
                        output.accept(ModItems.NETWORK_RESTOCK_UPGRADE.get());
                        output.accept(ModItems.DIMENSIONAL_RESTOCK_UPGRADE.get());

                        // 7. 流體泵 (Pump)
                        output.accept(ModItems.NETWORK_PUMP_UPGRADE.get());
                        output.accept(ModItems.DIMENSIONAL_PUMP_UPGRADE.get());

                        // 8. 鍊金施藥 (Alchemy)
                        output.accept(ModItems.NETWORK_ALCHEMY_UPGRADE.get());
                        output.accept(ModItems.DIMENSIONAL_ALCHEMY_UPGRADE.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}