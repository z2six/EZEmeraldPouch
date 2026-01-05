// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/event/EZEPCommonEvents.java
package org.z2six.ezemeraldpouch.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.z2six.ezemeraldpouch.ModConstants;
import org.z2six.ezemeraldpouch.network.SyncCountPayload;
import org.z2six.ezemeraldpouch.util.EmeraldPouchUtil;

import java.lang.reflect.Method;

public final class EZEPCommonEvents {

    private EZEPCommonEvents() {
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        // Failsafe: if emeralds exist but pouch item is missing (Curios toggled, datapack not applied, etc),
        // refund to player so we don't "soft-lock" value in the attachment.
        EmeraldPouchUtil.failsafeRefundIfNoPouch(sp, "login");

        PacketDistributor.sendToPlayer(sp, new SyncCountPayload(EmeraldPouchUtil.getCount(sp)));
        ModConstants.LOG.debug("[EZEP] Login sync sent to {}", sp.getGameProfile().getName());
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        EmeraldPouchUtil.failsafeRefundIfNoPouch(sp, "respawn");

        PacketDistributor.sendToPlayer(sp, new SyncCountPayload(EmeraldPouchUtil.getCount(sp)));
        ModConstants.LOG.debug("[EZEP] Respawn sync sent to {}", sp.getGameProfile().getName());
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onEmeraldPickup(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;

        ItemEntity itemEntity = event.getItemEntity();
        if (itemEntity == null) return;

        ItemStack stack = itemEntity.getItem();
        if (!EmeraldPouchUtil.isEmerald(stack)) return;

        // Only auto-deposit if the pouch exists somewhere.
        if (!EmeraldPouchUtil.hasPouchSomewhere(sp)) return;

        int count = stack.getCount();
        if (count <= 0) return;

        // Deposit into pouch
        EmeraldPouchUtil.depositEmeralds(sp, count);

        // Remove the entity so vanilla has nothing to insert.
        try {
            itemEntity.setItem(ItemStack.EMPTY);
        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Failed to set picked ItemEntity stack empty (safe to ignore): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] setItem failure details", t);
        }

        itemEntity.discard();

        // Best-effort: attempt to block pickup logic on this event without depending on a specific NeoForge signature.
        // If this fails, the entity is already discarded / emptied so vanilla should not add emeralds anyway.
        tryBlockPickup(event);

        PacketDistributor.sendToPlayer(sp, new SyncCountPayload(EmeraldPouchUtil.getCount(sp)));
        ModConstants.LOG.debug("[EZEP] Auto-picked {} emerald(s) into pouch for {}", count, sp.getGameProfile().getName());
    }

    private static void tryBlockPickup(ItemEntityPickupEvent.Pre event) {
        // NeoForge has moved some events to TriState-based gates. We avoid hard-linking by reflection.
        // Try common method names across versions:
        //  - setCanPickup(TriState)
        //  - setCanPickup(boolean)
        //  - deny()
        try {
            Class<?> triStateClass = null;
            Object triFalse = null;
            try {
                triStateClass = Class.forName("net.neoforged.neoforge.common.util.TriState");
                Method valueOf = triStateClass.getMethod("valueOf", String.class);
                triFalse = valueOf.invoke(null, "FALSE");
            } catch (Throwable ignored) {
                // TriState not present or changed; continue.
            }

            if (triStateClass != null && triFalse != null) {
                try {
                    Method m = event.getClass().getMethod("setCanPickup", triStateClass);
                    m.invoke(event, triFalse);
                    ModConstants.LOG.debug("[EZEP] ItemEntityPickupEvent.Pre blocked via setCanPickup(TriState.FALSE)");
                    return;
                } catch (NoSuchMethodException ignored) {
                }
            }

            try {
                Method m = event.getClass().getMethod("setCanPickup", boolean.class);
                m.invoke(event, false);
                ModConstants.LOG.debug("[EZEP] ItemEntityPickupEvent.Pre blocked via setCanPickup(false)");
                return;
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Method m = event.getClass().getMethod("deny");
                m.invoke(event);
                ModConstants.LOG.debug("[EZEP] ItemEntityPickupEvent.Pre blocked via deny()");
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Could not block pickup via reflection (safe to ignore): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Pickup block reflection failure details", t);
        }
    }
}
