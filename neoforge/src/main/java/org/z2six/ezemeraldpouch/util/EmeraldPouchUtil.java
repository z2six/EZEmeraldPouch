// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/util/EmeraldPouchUtil.java
package org.z2six.ezemeraldpouch.util;

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

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.is(ModItems.EMERALD_POUCH.get())) {
                return true;
            }
        }

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
}
