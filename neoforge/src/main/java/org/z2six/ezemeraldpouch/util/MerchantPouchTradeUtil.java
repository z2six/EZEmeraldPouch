package org.z2six.ezemeraldpouch.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

public final class MerchantPouchTradeUtil {

    private MerchantPouchTradeUtil() {}

    public static void topUpTradeFromPouch(Merchant trader, MerchantContainer tradeContainer, int offerIndex) {
        try {
            if (trader == null || tradeContainer == null) return;
            Player player = trader.getTradingPlayer();
            if (!(player instanceof ServerPlayer sp)) return;
            if (!EmeraldPouchUtil.hasPouchSomewhere(sp)) return;

            var offers = trader.getOffers();
            if (offers == null || offerIndex < 0 || offerIndex >= offers.size()) return;
            MerchantOffer offer = offers.get(offerIndex);
            if (offer == null) return;

            topUpOfferFromPouch(sp, tradeContainer, offer);
        } catch (Throwable ignored) {}
    }

    public static void topUpActiveTradeFromPouch(Merchant trader, MerchantContainer tradeContainer) {
        try {
            if (trader == null || tradeContainer == null) return;
            Player player = trader.getTradingPlayer();
            if (!(player instanceof ServerPlayer sp)) return;
            if (!EmeraldPouchUtil.hasPouchSomewhere(sp)) return;

            MerchantOffer offer = tradeContainer.getActiveOffer();
            if (offer == null || offer.isOutOfStock()) return;
            topUpOfferFromPouch(sp, tradeContainer, offer);
        } catch (Throwable ignored) {}
    }

    public static void redepositPaymentSlotsToPouch(Merchant trader, MerchantContainer tradeContainer) {
        try {
            if (trader == null || tradeContainer == null) return;
            Player player = trader.getTradingPlayer();
            if (!(player instanceof ServerPlayer sp)) return;
            if (!EmeraldPouchUtil.hasPouchSomewhere(sp)) return;

            long moved = 0L;
            moved += redepositSlot(tradeContainer, 0, sp);
            moved += redepositSlot(tradeContainer, 1, sp);
            if (moved > 0L) {
                EmeraldPouchUtil.syncCountToClient(sp);
            }
        } catch (Throwable ignored) {}
    }

    private static boolean topUpSlotFromPouch(ServerPlayer player, MerchantContainer tradeContainer, int slotIndex, ItemStack costStack) {
        try {
            if (player == null || tradeContainer == null || costStack == null || costStack.isEmpty()) return false;
            if (!costStack.is(Items.EMERALD)) return false;

            ItemStack current = tradeContainer.getItem(slotIndex);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, costStack)) return false;

            int have = current.isEmpty() ? 0 : current.getCount();
            int need = Math.max(0, costStack.getCount() - have);
            if (need <= 0) return false;
            if (!EmeraldPouchUtil.tryWithdrawExact(player, need)) return false;

            ItemStack next = costStack.copyWithCount(have + need);
            tradeContainer.setItem(slotIndex, next);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void topUpOfferFromPouch(ServerPlayer player, MerchantContainer tradeContainer, MerchantOffer offer) {
        try {
            if (player == null || tradeContainer == null || offer == null) return;
            boolean changed = false;
            changed |= topUpSlotFromPouch(player, tradeContainer, 0, offer.getCostA());
            ItemStack costB = offer.getCostB();
            if (!costB.isEmpty()) {
                changed |= topUpSlotFromPouch(player, tradeContainer, 1, costB);
            }
            if (changed) {
                EmeraldPouchUtil.syncCountToClient(player);
            }
        } catch (Throwable ignored) {}
    }

    private static long redepositSlot(MerchantContainer tradeContainer, int slotIndex, ServerPlayer player) {
        try {
            ItemStack stack = tradeContainer.getItem(slotIndex);
            if (stack == null || stack.isEmpty() || !stack.is(Items.EMERALD)) return 0L;
            long count = Math.max(0, stack.getCount());
            if (count <= 0L) return 0L;
            EmeraldPouchUtil.depositEmeralds(player, count);
            tradeContainer.setItem(slotIndex, ItemStack.EMPTY);
            return count;
        } catch (Throwable ignored) {
            return 0L;
        }
    }
}
