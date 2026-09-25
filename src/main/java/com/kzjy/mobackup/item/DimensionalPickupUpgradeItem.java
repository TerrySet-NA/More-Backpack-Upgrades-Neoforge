package com.kzjy.mobackup.item;

import com.kzjy.mobackup.wrapper.DimensionalPickupUpgradeWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;

import java.util.List;
import java.util.function.IntSupplier;

public class DimensionalPickupUpgradeItem extends PickupUpgradeItem implements IRSLinkedItem {
    public static final UpgradeType<PickupUpgradeWrapper> TYPE = new UpgradeType<>(
            DimensionalPickupUpgradeWrapper::new);

    private final boolean dimensional;

    public DimensionalPickupUpgradeItem(IntSupplier filterSlotCount, boolean dimensional) {
        super(filterSlotCount, Config.SERVER.maxUpgradesPerStorage);
        this.dimensional = dimensional;
    }

    public DimensionalPickupUpgradeItem(IntSupplier filterSlotCount) {
        this(filterSlotCount, true);
    }

    public DimensionalPickupUpgradeItem(boolean dimensional) {
        this(Config.SERVER.advancedPickupUpgrade.filterSlots::get, dimensional);
    }

    public DimensionalPickupUpgradeItem() {
        this(true);
    }

    @Override
    public boolean isDimensional() {
        return dimensional;
    }

    @Override
    public UpgradeType<PickupUpgradeWrapper> getType() {
        return TYPE;
    }

    @Override
    public List<UpgradeConflictDefinition> getUpgradeConflicts() {
        return List.of(new UpgradeConflictDefinition(item -> item instanceof PickupUpgradeItem, 0,
                Component.translatable("gui.mobackup.status.pickup_only_one_allowed")));
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