/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup;

import org.apache.commons.lang3.tuple.Pair;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置类
 */
public class Config {
    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        Pair<Common, ModConfigSpec> commonSpecPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = commonSpecPair.getLeft();
        COMMON_SPEC = commonSpecPair.getRight();
    }

    /**
     * 通用配置部分
     */
    public static class Common {
        // 次元磁吸升级的吸附范围半径
        public final ModConfigSpec.IntValue dimensionalMagnetRange;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("Upgrades");

            dimensionalMagnetRange = builder.comment("次元磁吸升级的吸附范围")
                    .defineInRange("dimensionalMagnetRange", 5, 1, 64);

            builder.pop();
        }
    }
}