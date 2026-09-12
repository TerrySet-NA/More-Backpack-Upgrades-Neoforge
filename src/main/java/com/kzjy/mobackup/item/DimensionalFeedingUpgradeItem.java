/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.item;

// import com.kzjy.mobackup.MoBackup;
import com.kzjy.mobackup.wrapper.DimensionalFeedingUpgradeWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
// import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeItem;
// import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
// import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper;

import java.util.List;
// import java.util.Set;

@SuppressWarnings("null")
public class DimensionalFeedingUpgradeItem extends FeedingUpgradeItem implements IRSLinkedItem {
    public static final UpgradeType<FeedingUpgradeWrapper> TYPE = new UpgradeType<>(
            DimensionalFeedingUpgradeWrapper::new);

    public DimensionalFeedingUpgradeItem() {
        super(Config.SERVER.advancedFeedingUpgrade.filterSlots::get, Config.SERVER.maxUpgradesPerStorage);
    }

    @Override
    public UpgradeType<FeedingUpgradeWrapper> getType() {
        return TYPE;
    }

    @Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of(new UpgradeConflictDefinition(item -> item instanceof FeedingUpgradeItem, 0,
				Component.translatable("gui.sophisticatedbackpacks.status.feeding_only_one_allowed")));
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