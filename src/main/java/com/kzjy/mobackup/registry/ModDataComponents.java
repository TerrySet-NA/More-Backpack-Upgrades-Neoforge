/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.registry;

import com.kzjy.mobackup.MoBackup;
import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MoBackup.MOD_ID);

    // 1. 優先級標記
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> NETWORK_FIRST =
            DATA_COMPONENTS.registerComponentType("network_first", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    // 2. RS 網路綁定座標 (名稱對齊物品 NBT/組件中的 mobackup:destination)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> DESTINATION =
            DATA_COMPONENTS.registerComponentType("destination", builder -> builder
                    .persistent(GlobalPos.CODEC)
                    .networkSynchronized(GlobalPos.STREAM_CODEC));

    // 3. 是否為次元版 (true = 次元版跨維度 / false = 網路版同維度)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_DIMENSIONAL =
            DATA_COMPONENTS.registerComponentType("is_dimensional", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));
}