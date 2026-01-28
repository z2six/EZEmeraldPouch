// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/util/EZEPClientConfigSaver.java
package org.z2six.ezemeraldpouch.util;

import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import org.z2six.ezemeraldpouch.ModConstants;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

/**
 * Attempts to persist client config changes to disk.
 *
 * NeoForge/FML config internals vary by version. This helper:
 * - updates the ModConfigSpec values via ConfigValue#set (done by callers),
 * - then calls ILoadedConfig#save() on the tracked ModConfig instance.
 *
 * If saving fails, values still apply for the current session (in-memory),
 * and NeoForge often writes configs at shutdown anyway, but we still try hard.
 */
public final class EZEPClientConfigSaver {

    private EZEPClientConfigSaver() {
    }

    public static void saveClientConfigBestEffort() {
        try {
            Optional<? extends ModContainer> opt = ModList.get().getModContainerById(ModConstants.MODID);
            if (opt.isEmpty()) {
                ModConstants.LOG.warn("[EZEP] Config save: mod container not found for modid={}", ModConstants.MODID);
                return;
            }

            ModConfig clientCfg = tryFindClientModConfig(ModConstants.MODID);
            if (clientCfg == null) {
                ModConstants.LOG.warn("[EZEP] Client config save: could not locate ModConfig instance for modid={} (non-fatal).",
                        ModConstants.MODID);
                return;
            }

            var loaded = clientCfg.getLoadedConfig();
            if (loaded == null) {
                ModConstants.LOG.warn("[EZEP] Client config save: ModConfig found but not loaded yet (non-fatal). file={}",
                        clientCfg.getFileName());
                return;
            }

            loaded.save();
            ModConstants.LOG.info("[EZEP] Client config saved to disk. file={}", clientCfg.getFileName());

        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Client config save failed (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Client config save failure details", t);
        }
    }

    private static ModConfig tryFindClientModConfig(String modId) {
        try {
            if (modId == null || modId.isBlank()) return null;

            ConfigTracker tracker = ConfigTracker.INSTANCE;

            // ConfigTracker has what we need, but it doesn't expose public getters for it.
            // We reflect the tracked configs-by-mod map and locate our CLIENT ModConfig instance.
            Field f = tracker.getClass().getDeclaredField("configsByMod");
            f.setAccessible(true);
            Object v = f.get(tracker);
            if (!(v instanceof Map<?, ?> map)) return null;

            Object list = map.get(modId);
            if (list instanceof Iterable<?> it) {
                for (Object o : it) {
                    if (o instanceof ModConfig mc && mc.getType() == ModConfig.Type.CLIENT) {
                        return mc;
                    }
                }
            }
        } catch (Throwable t) {
            ModConstants.LOG.debug("[EZEP] tryFindClientModConfig failed (non-fatal).", t);
        }
        return null;
    }
}
