package com.kzjy.mobackup.mixin;

import com.kzjy.mobackup.upgrade.IPriorityRoutingUpgrade;
import com.kzjy.mobackup.wrapper.DimensionalDepositUpgradeWrapper;
import com.kzjy.mobackup.wrapper.DimensionalRestockUpgradeWrapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StorageContainerMenuBase.class, remap = false)
public abstract class StorageContainerMenuBaseMixin {

    @Inject(method = "handlePacket", at = @At("HEAD"))
    private void mobackup$onHandlePacket(CompoundTag data, CallbackInfo ci) {
        if (!data.contains("containerId")) return;

        int containerId = data.getInt("containerId");
        StorageContainerMenuBase<?> menu = (StorageContainerMenuBase<?>) (Object) this;
        UpgradeContainerBase<?, ?> container = menu.getUpgradeContainers().get(containerId);
        if (container == null) return;

        // 1. 優先級切換
        if (data.contains("mobackup:priority") && container.getUpgradeWrapper() instanceof IPriorityRoutingUpgrade priorityUpgrade) {
            priorityUpgrade.setNetworkFirst(data.getBoolean("mobackup:priority"));
        }

        // 2. 一鍵動作按鈕
        if (data.contains("mobackup:action")) {
            String action = data.getString("mobackup:action");

            Player menuPlayer = container.getPlayer();
            Level safeLevel = menuPlayer.level();

            // 規則 3: 地上方塊模式時，actionPlayer 為 null
            boolean isBlock = menu instanceof BackpackContainer backpackContainer
                    && backpackContainer.getBlockPosition().isPresent();
            Player actionPlayer = isBlock ? null : menuPlayer;

            if ("quick_deposit".equals(action) && container.getUpgradeWrapper() instanceof DimensionalDepositUpgradeWrapper depositWrapper) {
                depositWrapper.performQuickDepositToLinkedRs(actionPlayer, menuPlayer, safeLevel);
            } else if ("quick_restock".equals(action) && container.getUpgradeWrapper() instanceof DimensionalRestockUpgradeWrapper restockWrapper) {
                restockWrapper.performQuickRestockFromLinkedRs(actionPlayer, menuPlayer, safeLevel);
            }
        }
    }
}