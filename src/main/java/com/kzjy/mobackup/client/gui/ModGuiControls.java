/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.client.gui;

import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

import java.util.Map;

@SuppressWarnings("null")
public class ModGuiControls {

    // =========================================================================
    // 1. 通用雙向路由切換 (磁吸、拾取、液泵、鍊金等)
    // =========================================================================
    public static final ButtonDefinition.Toggle<Boolean> PRIORITY_TOGGLE = ButtonDefinitions.createToggleButtonDefinition(
            ButtonDefinitions.getBooleanStateData(
                    // true: RS 網路優先
                    GuiHelper.getButtonStateData(new UV(0, 48), Dimension.SQUARE_16, new Position(1, 1),
                            Component.translatable("gui.sophisticatedbackpacks.button.priority_toggle_rs_1").withStyle(ChatFormatting.AQUA),
                            Component.translatable("gui.sophisticatedbackpacks.button.priority_toggle_rs_2").withStyle(ChatFormatting.GRAY),
                            Component.translatable("gui.sophisticatedbackpacks.button.priority_toggle_rs_3").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC)),
                    // false: 背包優先
                    GuiHelper.getButtonStateData(new UV(64, 48), Dimension.SQUARE_16, new Position(1, 1),
                            Component.translatable("gui.sophisticatedbackpacks.button.priority_toggle_backpack_1").withStyle(ChatFormatting.GOLD),
                            Component.translatable("gui.sophisticatedbackpacks.button.priority_toggle_backpack_2").withStyle(ChatFormatting.GRAY),
                            Component.translatable("gui.sophisticatedbackpacks.button.priority_toggle_backpack_3").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC))
            )
    );

    public static ToggleButton<Boolean> createPriorityButton(Position position, UpgradeContainerBase<?, ?> container) {
        return new ToggleButton<>(
                position,
                PRIORITY_TOGGLE,
                button -> togglePriority(container),
                () -> IPriorityRoutingUpgrade.isNetworkFirst(container.getUpgradeStack())
        );
    }

    // =========================================================================
    // 2. 外部卸貨來源開關 (外部非記錄空間/不同網路時生效)
    // =========================================================================
    public static final ButtonDefinition.Toggle<Boolean> DEPOSIT_SOURCE_TOGGLE = ButtonDefinitions.createToggleButtonDefinition(
            ButtonDefinitions.getBooleanStateData(
                    // true: 綁定 RS 網路
                    GuiHelper.getButtonStateData(new UV(80, 16), Dimension.SQUARE_16, new Position(1, 1),
                            Component.translatable("gui.mobackup.button.deposit_source_rs_1").withStyle(ChatFormatting.AQUA),
                            Component.translatable("gui.mobackup.button.deposit_source_rs_2").withStyle(ChatFormatting.GRAY)),
                    // false: 精妙背包
                    GuiHelper.getButtonStateData(new UV(32, 48), Dimension.SQUARE_16, new Position(1, 1),
                            Component.translatable("gui.mobackup.button.deposit_source_backpack_1").withStyle(ChatFormatting.GOLD),
                            Component.translatable("gui.mobackup.button.deposit_source_backpack_2").withStyle(ChatFormatting.GRAY))
            )
    );

    public static ToggleButton<Boolean> createDepositSourceButton(Position position, UpgradeContainerBase<?, ?> container) {
        return new ToggleButton<>(
                position,
                DEPOSIT_SOURCE_TOGGLE,
                button -> togglePriority(container),
                () -> IPriorityRoutingUpgrade.isNetworkFirst(container.getUpgradeStack())
        );
    }

    // =========================================================================
    // 3. 外部取貨目的地開關 (外部非記錄空間/不同網路時生效)
    // =========================================================================
    public static final ButtonDefinition.Toggle<Boolean> RESTOCK_TARGET_TOGGLE = ButtonDefinitions.createToggleButtonDefinition(
            ButtonDefinitions.getBooleanStateData(
                    // true: 綁定 RS 網路
                    GuiHelper.getButtonStateData(new UV(80, 16), Dimension.SQUARE_16, new Position(1, 1),
                            Component.translatable("gui.mobackup.button.restock_target_rs_1").withStyle(ChatFormatting.AQUA),
                            Component.translatable("gui.mobackup.button.restock_target_rs_2").withStyle(ChatFormatting.GRAY)),
                    // false: 精妙背包
                    GuiHelper.getButtonStateData(new UV(32, 48), Dimension.SQUARE_16, new Position(1, 1),
                            Component.translatable("gui.mobackup.button.restock_target_backpack_1").withStyle(ChatFormatting.GOLD),
                            Component.translatable("gui.mobackup.button.restock_target_backpack_2").withStyle(ChatFormatting.GRAY))
            )
    );

    public static ToggleButton<Boolean> createRestockTargetButton(Position position, UpgradeContainerBase<?, ?> container) {
        return new ToggleButton<>(
                position,
                RESTOCK_TARGET_TOGGLE,
                button -> togglePriority(container),
                () -> IPriorityRoutingUpgrade.isNetworkFirst(container.getUpgradeStack())
        );
    }

    // =========================================================================
    // 4. GUI 一鍵動作按鈕 (Action Buttons)
    // =========================================================================

    // 一鍵將隨身背包中符合過濾目標的物品傳送至綁定的 RS 網路
    public static final ButtonDefinition.Toggle<Boolean> QUICK_DEPOSIT_DEF = ButtonDefinitions.createToggleButtonDefinition(
            Map.of(true, GuiHelper.getButtonStateData(new UV(96, 48), Dimension.SQUARE_16, new Position(1, 1),
                    Component.translatable("gui.mobackup.button.quick_deposit_1").withStyle(ChatFormatting.GREEN),
                    Component.translatable("gui.mobackup.button.quick_deposit_2").withStyle(ChatFormatting.GRAY)))
    );

    public static ToggleButton<Boolean> createQuickDepositButton(Position position, UpgradeContainerBase<?, ?> container) {
        return new ToggleButton<>(
                position,
                QUICK_DEPOSIT_DEF,
                button -> container.sendDataToServer(() -> {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("mobackup:action", "quick_deposit");
                    return tag;
                }),
                () -> true
        );
    }

    // 一鍵從綁定 RS 網路補貨目標物品至隨身背包 (無目標不添入)
    public static final ButtonDefinition.Toggle<Boolean> QUICK_RESTOCK_DEF = ButtonDefinitions.createToggleButtonDefinition(
            Map.of(true, GuiHelper.getButtonStateData(new UV(48, 80), Dimension.SQUARE_16, new Position(1, 1),
                    Component.translatable("gui.mobackup.button.quick_restock_1").withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.mobackup.button.quick_restock_2").withStyle(ChatFormatting.GRAY)))
    );

    public static ToggleButton<Boolean> createQuickRestockButton(Position position, UpgradeContainerBase<?, ?> container) {
        return new ToggleButton<>(
                position,
                QUICK_RESTOCK_DEF,
                button -> container.sendDataToServer(() -> {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("mobackup:action", "quick_restock");
                    return tag;
                }),
                () -> true
        );
    }

    // =========================================================================
    // 內部私有輔助：處理優先級切換的本地樂觀更新與封包發送
    // =========================================================================
    private static void togglePriority(UpgradeContainerBase<?, ?> container) {
        ItemStack stack = container.getUpgradeStack();
        boolean newMode = !IPriorityRoutingUpgrade.isNetworkFirst(stack);

        // 客戶端本地樂觀更新
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(IPriorityRoutingUpgrade.TAG_NETWORK_FIRST, newMode);
        });

        // 發送原版封包至伺服端
        container.sendBooleanToServer("mobackup:priority", newMode);
    }
}