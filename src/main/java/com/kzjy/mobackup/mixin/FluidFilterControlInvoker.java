/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.mixin;

import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.FluidFilterContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.FluidFilterControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = FluidFilterControl.class, remap = false)
public interface FluidFilterControlInvoker {

    @Invoker("<init>")
    static FluidFilterControl callInit(Position position, FluidFilterContainer container) {
        throw new AssertionError("Mixin application failed");
    }
}