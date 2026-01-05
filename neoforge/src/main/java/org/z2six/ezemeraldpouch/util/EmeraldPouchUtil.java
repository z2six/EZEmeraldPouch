// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/util/EmeraldPouchUtil.java
package org.z2six.ezemeraldpouch.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.z2six.ezemeraldpouch.ModConstants;
import org.z2six.ezemeraldpouch.data.EmeraldPouchData;
import org.z2six.ezemeraldpouch.data.ModAttachments;
import org.z2six.ezemeraldpouch.registry.ModItems;

public final class EmeraldPouchUtil {

    private EmeraldPouchUtil() {
    }

    public static EmeraldPouchData getData(Player player) {
        EmeraldPouchData data = player.getData(ModAttachments.EMERALD_POUCH.get());
        if (data == null) {
            ModConstants.LOG.error("[EZEP] Player attachment returned null for {}", player.getGameProfile().getName());
            return new EmeraldPouchData();
        }
        return data;
    }

    public static boolean hasPouchSomewhere(Player player) {
        if (player == null) return false;

        // Inventory check
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.is(ModItems.EMERALD_POUCH.get())) {
                return true;
            }
        }

        // Curios check (reflection-based)
        if (CuriosReflection.hasEquippedPouch(player, ModConstants.MODID + ":emerald_pouch")) {
            return true;
        }

        return false;
    }

    public static boolean isEmerald(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.EMERALD);
    }

    public static long depositEmeralds(Player player, long amount) {
        if (player == null) return 0L;
        if (amount <= 0L) return 0L;
        EmeraldPouchData data = getData(player);
        data.deposit(amount);
        return amount;
    }

    public static long withdrawEmeralds(Player player, long amount) {
        if (player == null) return 0L;
        if (amount <= 0L) return 0L;
        EmeraldPouchData data = getData(player);
        return data.withdrawUpTo(amount);
    }

    public static long getCount(Player player) {
        if (player == null) return 0L;
        return getData(player).getEmeralds();
    }

    /**
     * Failsafe/corruption handling:
     * If the player has emeralds stored (attachment) but does not have an Emerald Pouch item
     * anywhere (inventory or Curios), refund those emeralds into the player's inventory (drop overflow),
     * and reduce the stored value accordingly (ideally to 0).
     *
     * This prevents "orphaned" emeralds when Curios is toggled off, a datapack misloads, or the pouch item is lost.
     */
    public static void failsafeRefundIfNoPouch(ServerPlayer player, String reason) {
        try {
            if (player == null) return;

            long stored = getCount(player);
            if (stored <= 0L) return;

            boolean hasPouch = hasPouchSomewhere(player);
            if (hasPouch) {
                ModConstants.LOG.debug("[EZEP] Failsafe check ok: pouch exists. reason={} player={} stored={}",
                        safe(reason), player.getName().getString(), stored);
                return;
            }

            ModConstants.LOG.warn("[EZEP] Failsafe triggered: player has stored emeralds but no pouch item. " +
                            "reason={} player={} stored={}. Refunding to inventory/drop.",
                    safe(reason), player.getName().getString(), stored);

            long refunded = refundEmeraldsToPlayer(player, stored);
            long remaining = Math.max(0L, stored - refunded);

            // Only reduce stored by what we successfully refunded.
            EmeraldPouchData data = getData(player);
            data.setEmeralds(remaining);

            if (remaining > 0L) {
                ModConstants.LOG.warn("[EZEP] Failsafe refund partial: refunded={} remainingStored={} player={}",
                        refunded, remaining, player.getName().getString());
            } else {
                ModConstants.LOG.info("[EZEP] Failsafe refund complete: refunded={} stored now 0. player={}",
                        refunded, player.getName().getString());
            }

        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Failsafe refund failed (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Failsafe refund failure details", t);
        }
    }

    /**
     * Tries to give emerald items to the player, dropping overflow.
     * Returns number of emeralds successfully materialized (added or dropped).
     *
     * This method is server-side and attempts to be non-lossy. If dropping fails (rare),
     * it stops early and returns partial success so stored value is not deleted.
     */
    public static long refundEmeraldsToPlayer(ServerPlayer player, long emeralds) {
        if (player == null) return 0L;
        if (emeralds <= 0L) return 0L;

        long givenTotal = 0L;
        long remaining = emeralds;

        while (remaining > 0L) {
            int give = (int) Math.min(64L, remaining);
            ItemStack stack = new ItemStack(Items.EMERALD, give);

            boolean added = false;
            try {
                added = player.getInventory().add(stack);
            } catch (Throwable t) {
                added = false;
                ModConstants.LOG.warn("[EZEP] refundEmeraldsToPlayer: inventory add failed (non-fatal) player={} err={}",
                        player.getName().getString(), t.toString());
                ModConstants.LOG.debug("[EZEP] inventory add failure details", t);
            }

            if (!added && !stack.isEmpty()) {
                // Drop at player if inventory full or add failed
                try {
                    ItemEntity ent = player.drop(stack, false);
                    if (ent != null) {
                        ent.setNoPickUpDelay();
                        ent.setThrower(player);
                    }
                    ModConstants.LOG.debug("[EZEP] refundEmeraldsToPlayer: dropped {} emerald(s) near player={}",
                            give, player.getName().getString());
                } catch (Throwable t) {
                    // If we can't drop reliably, stop early so we don't delete stored value.
                    ModConstants.LOG.warn("[EZEP] refundEmeraldsToPlayer: drop failed; aborting early to avoid loss. player={} err={}",
                            player.getName().getString(), t.toString());
                    ModConstants.LOG.debug("[EZEP] drop failure details", t);
                    break;
                }
            }

            givenTotal += give;
            remaining -= give;
        }

        return givenTotal;
    }

    private static String safe(String s) {
        return (s == null) ? "unknown" : s;
    }
}
