package com.kzjy.mobackup;

import java.util.List;
import java.util.Objects;

import com.kzjy.mobackup.client.gui.DimensionalAlchemyUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalDepositUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalFeedingUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalMagnetUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalPickupUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalPumpUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalRefillUpgradeTab;
import com.kzjy.mobackup.client.gui.DimensionalRestockUpgradeTab;
import com.kzjy.mobackup.core.PickupContext;
import com.kzjy.mobackup.core.RSBridge;
import com.kzjy.mobackup.registry.ModCreativeModeTabs;
import com.kzjy.mobackup.registry.ModItems;
import com.kzjy.mobackup.wrapper.DimensionalDepositUpgradeWrapper;
import com.kzjy.mobackup.wrapper.DimensionalRestockUpgradeWrapper;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeWrapper;

@Mod(MoBackup.MOD_ID)
public class MoBackup {
    public static final String MOD_ID = "mobackup";

    public MoBackup(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, com.kzjy.mobackup.Config.COMMON_SPEC, "MoreBackpackUpgrades-common.toml");

        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_MAGNET_UPGRADE.getId()),
                    DIMENSIONAL_MAGNET_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_PICKUP_UPGRADE.getId()),
                    DIMENSIONAL_PICKUP_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_DEPOSIT_UPGRADE.getId()),
                    DIMENSIONAL_DEPOSIT_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_FEEDING_UPGRADE.getId()),
                    DIMENSIONAL_FEEDING_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_REFILL_UPGRADE.getId()),
                    DIMENSIONAL_REFILL_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_RESTOCK_UPGRADE.getId()),
                    DIMENSIONAL_RESTOCK_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_PUMP_UPGRADE.getId()),
                    DIMENSIONAL_PUMP_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_ALCHEMY_UPGRADE.getId()),
                    DIMENSIONAL_ALCHEMY_TYPE);
        });
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> DIMENSIONAL_PICKUP_TYPE =
            new UpgradeContainerType<>(ContentsFilteredUpgradeContainer::new);
    public static final UpgradeContainerType<MagnetUpgradeWrapper, MagnetUpgradeContainer> DIMENSIONAL_MAGNET_TYPE =
            new UpgradeContainerType<>(MagnetUpgradeContainer::new);
    public static final UpgradeContainerType<DepositUpgradeWrapper, DepositUpgradeContainer> DIMENSIONAL_DEPOSIT_TYPE =
            new UpgradeContainerType<>(DepositUpgradeContainer::new);
    public static final UpgradeContainerType<FeedingUpgradeWrapper, FeedingUpgradeContainer> DIMENSIONAL_FEEDING_TYPE =
            new UpgradeContainerType<>(FeedingUpgradeContainer::new);
    public static final UpgradeContainerType<RefillUpgradeWrapper, RefillUpgradeContainer> DIMENSIONAL_REFILL_TYPE =
            new UpgradeContainerType<>(RefillUpgradeContainer::new);
    public static final UpgradeContainerType<RestockUpgradeWrapper, ContentsFilteredUpgradeContainer<RestockUpgradeWrapper>> DIMENSIONAL_RESTOCK_TYPE =
            new UpgradeContainerType<>(ContentsFilteredUpgradeContainer::new);
    public static final UpgradeContainerType<PumpUpgradeWrapper, PumpUpgradeContainer> DIMENSIONAL_PUMP_TYPE =
            new UpgradeContainerType<>(PumpUpgradeContainer::new);
    public static final UpgradeContainerType<AlchemyUpgradeWrapper, AlchemyUpgradeContainer> DIMENSIONAL_ALCHEMY_TYPE =
            new UpgradeContainerType<>(AlchemyUpgradeContainer::new);

    // =========================================================================
    // 拾取上下文管理 (Pickup Context)
    // =========================================================================
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPickupPre(ItemEntityPickupEvent.Pre event) {
        PickupContext.push(event.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPickupPost(ItemEntityPickupEvent.Post event) {
        PickupContext.pop();
    }

    // =========================================================================
    // 世界交互：手持背包 Shift + 右鍵點擊實體 RS 方塊一鍵存取
    // =========================================================================
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        if (player == null || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof BackpackItem)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (level.isClientSide()) {
            if (level.getBlockEntity(pos) != null) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            Network network = RSBridge.getRsNetworkAt(serverLevel, pos);
            if (network == null) {
                return;
            }

            // 🛡️ 嚴格權限檢驗
            if (!RSBridge.hasPermission(network, player, BuiltinPermission.INSERT)) {
                return;
            }

            IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(held);
            if (backpackWrapper == null) return;

            boolean handled = false;

            // 1. 先執行次元卸貨
            List<DimensionalDepositUpgradeWrapper> depositWrappers =
                    backpackWrapper.getUpgradeHandler().getWrappersThatImplement(DimensionalDepositUpgradeWrapper.class);
            for (DimensionalDepositUpgradeWrapper depositWrapper : depositWrappers) {
                depositWrapper.performDepositAndNotify(network, player);
                handled = true;
            }

            // 2. 後執行次元取貨
            List<DimensionalRestockUpgradeWrapper> restockWrappers =
                    backpackWrapper.getUpgradeHandler().getWrappersThatImplement(DimensionalRestockUpgradeWrapper.class);
            for (DimensionalRestockUpgradeWrapper restockWrapper : restockWrappers) {
                restockWrapper.performRestockAndNotify(network, player);
                handled = true;
            }

            if (handled) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    // =========================================================================
    // 客戶端 GUI 標籤頁註冊
    // =========================================================================
    @SuppressWarnings("removal")
    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void clientSetup(final FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_PICKUP_TYPE,
                        (ContentsFilteredUpgradeContainer<PickupUpgradeWrapper> uc, Position p, StorageScreenBase<?> s) ->
                                new DimensionalPickupUpgradeTab(
                                        uc, p, s,
                                        Config.SERVER.advancedPickupUpgrade.slotsInRow.get(),
                                        SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_MAGNET_TYPE,
                        (MagnetUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
                                new DimensionalMagnetUpgradeTab(
                                        uc, p, s,
                                        Config.SERVER.advancedMagnetUpgrade.slotsInRow.get(),
                                        SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_DEPOSIT_TYPE,
                        (DepositUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
                                new DimensionalDepositUpgradeTab(uc, p, s));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_FEEDING_TYPE,
                        (FeedingUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
                                new DimensionalFeedingUpgradeTab(
                                        uc, p, s,
                                        Config.SERVER.advancedFeedingUpgrade.slotsInRow.get()));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_REFILL_TYPE,
                        (RefillUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
                                new DimensionalRefillUpgradeTab(
                                        uc, p, s,
                                        Config.SERVER.advancedRefillUpgrade.slotsInRow.get()));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_RESTOCK_TYPE,
                        (ContentsFilteredUpgradeContainer<RestockUpgradeWrapper> uc, Position p, StorageScreenBase<?> s) ->
                                new DimensionalRestockUpgradeTab(
                                        uc, p, s,
                                        SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_PUMP_TYPE,
                        (PumpUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
                                new DimensionalPumpUpgradeTab(uc, p, s));

                UpgradeGuiManager.registerTab(
                        DIMENSIONAL_ALCHEMY_TYPE,
                        (AlchemyUpgradeContainer uc, Position p, StorageScreenBase<?> s) ->
                                new DimensionalAlchemyUpgradeTab(uc, p, s));
            });
        }
    }
}