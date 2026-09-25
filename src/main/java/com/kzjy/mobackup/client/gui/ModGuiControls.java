/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.client.gui;

import com.kzjy.mobackup.MoBackup;
import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

@SuppressWarnings("null")
public class ModGuiControls {

    public static final ResourceLocation RS_WIRELESS_GRID_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(MoBackup.MOD_ID, "textures/button/icon.png");

    private static TextureBlitData PUV(UV uv) {
        return new TextureBlitData(
                RS_WIRELESS_GRID_TEXTURE,
                new Position(1, 1),
                Dimension.SQUARE_256,
                uv,
                Dimension.SQUARE_16
        );
    }

    // =========================================================================
    // 1. 通用雙向優先級切換開關 (全面改為 Data Component)
    // =========================================================================
    public static final ButtonDefinition.Toggle<Boolean> PRIORITY_TOGGLE = ButtonDefinitions.createToggleButtonDefinition(
            ButtonDefinitions.getBooleanStateData(
                    // true: RS 網路優先
                    new ToggleButton.StateData(
                            PUV(new UV(16 * 2, 0)),
                            Component.translatable("gui.mobackup.button.priority_toggle_rs_1").withStyle(ChatFormatting.AQUA),
                            Component.translatable("gui.mobackup.button.priority_toggle_rs_2").withStyle(ChatFormatting.GRAY),
                            Component.translatable("gui.mobackup.button.priority_toggle_rs_3").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC)
                    ),
                    // false: 背包優先
                    GuiHelper.getButtonStateData(new UV(16 * 4, 16 * 3), Dimension.SQUARE_16, new Position(1, 1),
                            Component.translatable("gui.mobackup.button.priority_toggle_backpack_1").withStyle(ChatFormatting.GOLD),
                            Component.translatable("gui.mobackup.button.priority_toggle_backpack_2").withStyle(ChatFormatting.GRAY),
                            Component.translatable("gui.mobackup.button.priority_toggle_backpack_3").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC))
            )
    );

    public static ToggleButton<Boolean> createPriorityButton(Position position, UpgradeContainerBase<?, ?> container) {
        return new ToggleButton<>(
                position,
                PRIORITY_TOGGLE,
                button -> {
                    ItemStack stack = container.getUpgradeStack();
                    boolean newMode = !IPriorityRoutingUpgrade.isNetworkFirst(stack);

                    // 1.21.1 客戶端本地樂觀更新：直接寫入 Data Component，無 NBT
                    IPriorityRoutingUpgrade.setNetworkFirst(stack, newMode);

                    // 同步發送至伺服端
                    container.sendBooleanToServer("mobackup:priority", newMode);
                },
                () -> IPriorityRoutingUpgrade.isNetworkFirst(container.getUpgradeStack())
        );
    }

    // =========================================================================
    // 2. 卸貨升級：一鍵傳送背包目標物至 RS 網路按鈕
    // =========================================================================
    public static final ButtonDefinition.Toggle<Boolean> QUICK_DEPOSIT_TOGGLE = ButtonDefinitions.createToggleButtonDefinition(
            ButtonDefinitions.getBooleanStateData(
                    new ToggleButton.StateData(
                            PUV(new UV(0, 0)),
                            Component.translatable("gui.mobackup.button.quick_deposit_1").withStyle(ChatFormatting.GREEN),
                            Component.translatable("gui.mobackup.button.quick_deposit_2").withStyle(ChatFormatting.GRAY)
                    ),
                    new ToggleButton.StateData(
                            PUV(new UV(0, 0)),
                            Component.translatable("gui.mobackup.button.quick_deposit_1").withStyle(ChatFormatting.GREEN),
                            Component.translatable("gui.mobackup.button.quick_deposit_2").withStyle(ChatFormatting.GRAY)
                    )
            )
    );

    public static ToggleButton<Boolean> createQuickDepositButton(Position position, UpgradeContainerBase<?, ?> container) {
        return new ToggleButton<>(
                position,
                QUICK_DEPOSIT_TOGGLE,
                button -> container.sendDataToServer(() -> {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("mobackup:action", "quick_deposit");
                    return tag;
                }),
                () -> true
        );
    }

    // =========================================================================
    // 3. 取貨升級：一鍵從 RS 網路補貨目標物至背包按鈕
    // =========================================================================
    public static final ButtonDefinition.Toggle<Boolean> QUICK_RESTOCK_TOGGLE = ButtonDefinitions.createToggleButtonDefinition(
            ButtonDefinitions.getBooleanStateData(
                    new ToggleButton.StateData(
                            PUV(new UV(16, 0)),
                            Component.translatable("gui.mobackup.button.quick_restock_1").withStyle(ChatFormatting.AQUA),
                            Component.translatable("gui.mobackup.button.quick_restock_2").withStyle(ChatFormatting.GRAY)
                    ),
                    new ToggleButton.StateData(
                            PUV(new UV(16, 0)),
                            Component.translatable("gui.mobackup.button.quick_restock_1").withStyle(ChatFormatting.AQUA),
                            Component.translatable("gui.mobackup.button.quick_restock_2").withStyle(ChatFormatting.GRAY)
                    )
            )
    );

    public static ToggleButton<Boolean> createQuickRestockButton(Position position, UpgradeContainerBase<?, ?> container) {
        return new ToggleButton<>(
                position,
                QUICK_RESTOCK_TOGGLE,
                button -> container.sendDataToServer(() -> {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("mobackup:action", "quick_restock");
                    return tag;
                }),
                () -> true
        );
    }
}