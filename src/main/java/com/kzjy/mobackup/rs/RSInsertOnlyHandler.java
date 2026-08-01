/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

// package com.kzjy.mobackup.rs;

// import com.refinedmods.refinedstorage.api.core.Action;
// import com.refinedmods.refinedstorage.api.network.Network;
// import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
// import com.refinedmods.refinedstorage.api.storage.Actor;
// import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

// import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.item.ItemStack;
// import net.neoforged.neoforge.items.IItemHandler;

// import javax.annotation.Nonnull;
// import javax.annotation.Nullable;

// public class RSInsertOnlyHandler implements IItemHandler {

//     public static final RSInsertOnlyHandler CLIENT_DUMMY = new RSInsertOnlyHandler(null);

//     @Nullable
//     private final Network network;
//     @Nullable
//     private final Player player;

//     // 單參數建構子
//     public RSInsertOnlyHandler(@Nullable Network network) {
//         this(network, null);
//     }

//     // 雙參數建構子（相容傳入 player 或 null 的呼叫）
//     public RSInsertOnlyHandler(@Nullable Network network, @Nullable Player player) {
//         this.network = network;
//         this.player = player;
//     }

//     @Override
//     public int getSlots() {
//         return 1; // 固定回傳 1，告訴精妙背包「有槽位可供插入」
//     }

//     @Nonnull
//     @Override
//     public ItemStack getStackInSlot(int slot) {
//         return ItemStack.EMPTY;
//     }

//     @Nonnull
//     @Override
//     public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
//         if (stack.isEmpty()) {
//             return ItemStack.EMPTY;
//         }

//         if (network == null) {
//             return stack;
//         }

//         StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
//         if (storage == null) {
//             return stack;
//         }

//         ItemResource resource = ItemResource.ofItemStack(stack);
//         Action action = simulate ? Action.SIMULATE : Action.EXECUTE;

//         // 向 RS 2.x 網路執行存入
//         long inserted = storage.insert(resource, stack.getCount(), action, Actor.EMPTY);

//         if (inserted == 0) {
//             return stack;
//         }

//         if (inserted >= stack.getCount()) {
//             return ItemStack.EMPTY;
//         }

//         ItemStack remainder = stack.copy();
//         remainder.shrink((int) inserted);
//         return remainder;
//     }

//     @Nonnull
//     @Override
//     public ItemStack extractItem(int slot, int amount, boolean simulate) {
//         return ItemStack.EMPTY;
//     }

//     @Override
//     public int getSlotLimit(int slot) {
//         return 64; // 給予標準 64 上限
//     }

//     @Override
//     public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
//         return true;
//     }
// }