package org.z2six.ezemeraldpouch.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.z2six.ezemeraldpouch.util.EmeraldPouchUtil;

public final class EZEmeraldPouchApi {

    private EZEmeraldPouchApi() {}

    public static boolean hasPouch(Player player) {
        return EmeraldPouchUtil.hasPouchSomewhere(player);
    }

    public static long getStoredEmeralds(Player player) {
        return EmeraldPouchUtil.getCount(player);
    }

    public static long depositToPouch(Player player, long emeralds) {
        return EmeraldPouchUtil.depositEmeralds(player, emeralds);
    }

    public static long withdrawFromPouch(Player player, long emeralds) {
        return EmeraldPouchUtil.withdrawEmeralds(player, emeralds);
    }

    public static boolean trySpendEmeralds(Player player, long emeralds) {
        return EmeraldPouchUtil.tryWithdrawExact(player, emeralds);
    }

    public static long giveEmeralds(ServerPlayer player, long emeralds) {
        return EmeraldPouchUtil.giveEmeralds(player, emeralds);
    }

    public static void sync(ServerPlayer player) {
        EmeraldPouchUtil.syncCountToClient(player);
    }
}
