package org.z2six.ezemeraldpouch.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.z2six.ezemeraldpouch.util.MerchantPouchTradeUtil;

@Mixin(MerchantMenu.class)
abstract class MerchantMenuPouchMixin {

    @Shadow @Final private Merchant trader;
    @Shadow @Final private MerchantContainer tradeContainer;

    @Unique
    private boolean ezep$topUpInProgress;

    @Inject(method = "tryMoveItems", at = @At("TAIL"))
    private void ezep$topUpAfterRecipeSelect(int selectedMerchantRecipe, CallbackInfo ci) {
        if (this.ezep$topUpInProgress) return;
        this.ezep$topUpInProgress = true;
        try {
            MerchantPouchTradeUtil.topUpTradeFromPouch(this.trader, this.tradeContainer, selectedMerchantRecipe);
        } finally {
            this.ezep$topUpInProgress = false;
        }
    }

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void ezep$topUpAfterTradeSlotChanges(Container inventory, CallbackInfo ci) {
        if (this.ezep$topUpInProgress || inventory != this.tradeContainer) return;
        this.ezep$topUpInProgress = true;
        try {
            MerchantPouchTradeUtil.topUpActiveTradeFromPouch(this.trader, this.tradeContainer);
        } finally {
            this.ezep$topUpInProgress = false;
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void ezep$redepositToPouch(Player player, CallbackInfo ci) {
        MerchantPouchTradeUtil.redepositPaymentSlotsToPouch(this.trader, this.tradeContainer);
    }
}
