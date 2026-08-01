/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

// package com.kzjy.mobackup.item;

// import com.kzjy.mobackup.wrapper.DimensionalDepositUpgradeWrapper;
// import com.mojang.logging.LogUtils;

// import net.minecraft.network.chat.Component;
// import net.minecraft.world.InteractionResult;
// import net.minecraft.world.item.Item;
// import net.minecraft.world.item.ItemStack;
// import net.minecraft.world.item.TooltipFlag;
// import net.minecraft.world.item.context.UseOnContext;
// import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeItem;
// import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
// import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
// import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
// import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

// import java.util.List;
// import java.util.Set;
// import java.util.function.IntSupplier;

// import org.slf4j.Logger;

// public class DimensionalDepositUpgradeItem extends DepositUpgradeItem implements IRSLinkedItem {

//     private static final Logger MoBackup.LOGGER = LogUtils.getLogger();
//     private static final UpgradeType<DepositUpgradeWrapper> TYPE = new UpgradeType<>(
//             DimensionalDepositUpgradeWrapper::new);

//     public DimensionalDepositUpgradeItem(IntSupplier filterSlotCount) {
//         super(filterSlotCount);
//     }

//     @Override
//     public UpgradeType<DepositUpgradeWrapper> getType() {
//         return TYPE;
//     }

//     // 關鍵接管：通過門禁後，只檢查是否重複放入同款卡片
//     @Override
//     public UpgradeSlotChangeResult canAddUpgradeTo(IStorageWrapper storageWrapper, ItemStack upgradeStack, boolean firstLevelStorage, boolean isClientSide) {
//         // 絕對不要呼叫 super.canAddUpgradeTo！直接由我們自己檢查背包內是否已存在同款卡片
//         int existingCount = 0;
//         for (int i = 0; i < storageWrapper.getUpgradeHandler().getSlots(); i++) {
//             ItemStack inSlot = storageWrapper.getUpgradeHandler().getStackInSlot(i);
//             if (!inSlot.isEmpty() && inSlot.getItem() instanceof DimensionalMagnetUpgradeItem) {
//                 existingCount++;
//             }
//         }

//         if (existingCount >= 1) {
//             MoBackup.LOGGER.warn("[MoBackup-Debug] canAddUpgradeTo 拒絕：背包內已存在次元磁吸升級卡！");
//             return UpgradeSlotChangeResult.fail(
//                     Component.literal("背包內已存在相同的次元磁吸升級卡！"),
//                     Set.of(), Set.of(), Set.of()
//             );
//         }

//         MoBackup.LOGGER.info("[MoBackup-Debug] canAddUpgradeTo 放行成功！允許放入升級槽");
//         return UpgradeSlotChangeResult.success();
//     }

//     @Override
//     public InteractionResult useOn(UseOnContext ctx) {
//         InteractionResult result = handleUseOn(ctx);
//         if (result != InteractionResult.PASS) {
//             return result;
//         }
//         return super.useOn(ctx);
//     }

//     @Override
//     public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
//         super.appendHoverText(stack, context, tooltip, flagIn);
//         IRSLinkedItem.super.appendHoverText(stack, tooltip);
//     }

//     @Override
//     public int getUpgradesPerStorage(String storageType) {
//         return 1;
//     }

//     @Override
//     public int getUpgradesInGroupPerStorage(String storageType) {
//         return Integer.MAX_VALUE;
//     }
// }