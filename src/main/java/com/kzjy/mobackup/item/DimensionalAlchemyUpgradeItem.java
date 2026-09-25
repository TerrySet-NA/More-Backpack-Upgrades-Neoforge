/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.item;

import com.kzjy.mobackup.wrapper.DimensionalAlchemyUpgradeWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper;

import java.util.List;
import java.util.function.IntSupplier;

public class DimensionalAlchemyUpgradeItem extends AlchemyUpgradeItem implements IRSLinkedItem {
    public static final UpgradeType<AlchemyUpgradeWrapper> TYPE = new UpgradeType<>(
            DimensionalAlchemyUpgradeWrapper::new);

    private final boolean dimensional;

    public DimensionalAlchemyUpgradeItem(IntSupplier filterSlotCount, boolean dimensional) {
        super(filterSlotCount, Config.SERVER.maxUpgradesPerStorage);
        this.dimensional = dimensional;
    }

    public DimensionalAlchemyUpgradeItem(IntSupplier filterSlotCount) {
        this(filterSlotCount, true);
    }

    public DimensionalAlchemyUpgradeItem(boolean dimensional) {
        this(Config.SERVER.advancedAlchemyUpgrade.filterSlots::get, dimensional);
    }

    public DimensionalAlchemyUpgradeItem() {
        this(true);
    }

    @Override
    public boolean isDimensional() {
        return dimensional;
    }

    @Override
    public UpgradeType<AlchemyUpgradeWrapper> getType() {
        return TYPE;
    }

    @Override
    public List<UpgradeConflictDefinition> getUpgradeConflicts() {
        return List.of(new UpgradeConflictDefinition(item -> item instanceof AlchemyUpgradeItem, 0,
                Component.translatable("gui.mobackup.status.alchemy_only_one_allowed")));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        InteractionResult result = handleUseOn(ctx);
        if (result != InteractionResult.PASS) {
            return result;
        }
        return super.useOn(ctx);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        IRSLinkedItem.super.appendHoverText(stack, tooltip);
    }

    @Override
    public int getUpgradesPerStorage(String storageType) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getUpgradesInGroupPerStorage(String storageType) {
        return Integer.MAX_VALUE;
    }
}