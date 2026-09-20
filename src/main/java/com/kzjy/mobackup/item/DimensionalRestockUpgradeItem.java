package com.kzjy.mobackup.item;

import com.kzjy.mobackup.wrapper.DimensionalRestockUpgradeWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeItem;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;

public class DimensionalRestockUpgradeItem extends RestockUpgradeItem implements IRSLinkedItem {
    public static final UpgradeType<RestockUpgradeWrapper> TYPE = new UpgradeType<>(
            DimensionalRestockUpgradeWrapper::new);

    public DimensionalRestockUpgradeItem() {
        super(Config.SERVER.advancedRestockUpgrade.filterSlots::get);
    }

    @Override
    public UpgradeType<RestockUpgradeWrapper> getType() {
        return TYPE;
    }

    @Override
    public List<UpgradeConflictDefinition> getUpgradeConflicts() {
        return List.of(new UpgradeConflictDefinition(
                item -> item instanceof RestockUpgradeItem,
                0,
                Component.translatable("gui.mobackup.status.restock_only_one_allowed")
        ));
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