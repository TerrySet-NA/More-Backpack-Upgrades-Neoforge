package com.kzjy.mobackup.item;

import com.kzjy.mobackup.wrapper.DimensionalDepositUpgradeWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeItem;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;

public class DimensionalDepositUpgradeItem extends DepositUpgradeItem implements IRSLinkedItem {
    public static final UpgradeType<DepositUpgradeWrapper> TYPE = new UpgradeType<>(
            DimensionalDepositUpgradeWrapper::new);

    private final boolean dimensional;

    public DimensionalDepositUpgradeItem(boolean dimensional) {
        super(Config.SERVER.advancedDepositUpgrade.filterSlots::get);
        this.dimensional = dimensional;
    }

    public DimensionalDepositUpgradeItem() {
        this(true);
    }

    @Override
    public boolean isDimensional() {
        return dimensional;
    }

    @Override
    public UpgradeType<DepositUpgradeWrapper> getType() {
        return TYPE;
    }

    @Override
    public List<UpgradeConflictDefinition> getUpgradeConflicts() {
        return List.of(new UpgradeConflictDefinition(item -> item instanceof DepositUpgradeItem, 0,
                Component.translatable("gui.mobackup.status.deposit_only_one_allowed")));
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