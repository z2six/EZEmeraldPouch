// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/util/EZEPClientConfigSaver.java
package org.z2six.ezemeraldpouch.util;

import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import org.z2six.ezemeraldpouch.ModConstants;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Attempts to persist client config changes to disk.
 *
 * NeoForge config internals vary by version. This helper:
 * - updates the ModConfigSpec values via ConfigValue#set (done by callers),
 * - then tries multiple reflection strategies to force a save.
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

            ModContainer container = opt.get();

            // 1) Try a direct "getConfig(ModConfig.Type)" style method if present
            ModConfig clientCfg = tryGetClientConfig(container);
            if (clientCfg != null) {
                if (tryInvokeSaveOnConfig(clientCfg)) {
                    ModConstants.LOG.info("[EZEP] Client config saved (direct save method).");
                    return;
                }
            }

            // 2) Try ConfigTracker.INSTANCE.* reflection
            if (clientCfg != null && trySaveViaConfigTracker(clientCfg)) {
                ModConstants.LOG.info("[EZEP] Client config saved (ConfigTracker path).");
                return;
            }

            // 3) Try to locate all ModConfig objects from the container and save them
            if (trySaveAllContainerConfigs(container)) {
                ModConstants.LOG.info("[EZEP] Client config saved (container configs iteration path).");
                return;
            }

            ModConstants.LOG.warn("[EZEP] Client config save: no working save path found (non-fatal).");

        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Client config save failed (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Client config save failure details", t);
        }
    }

    private static ModConfig tryGetClientConfig(ModContainer container) {
        try {
            if (container == null) return null;

            // Look for getConfig(ModConfig.Type)
            for (Method m : container.getClass().getMethods()) {
                if (!m.getName().equals("getConfig")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length != 1) continue;
                if (!p[0].getName().equals("net.neoforged.fml.config.ModConfig$Type")) continue;

                Object cfg = m.invoke(container, ModConfig.Type.CLIENT);
                if (cfg instanceof ModConfig mc) return mc;
            }

            // Look for getConfigs() returning something iterable / map-like
            for (Method m : container.getClass().getMethods()) {
                if (!m.getName().equals("getConfigs")) continue;
                if (m.getParameterCount() != 0) continue;

                Object o = m.invoke(container);
                // Some versions: Map<ModConfig.Type, ModConfig>
                if (o instanceof Map<?, ?> map) {
                    Object cfg = map.get(ModConfig.Type.CLIENT);
                    if (cfg instanceof ModConfig mc) return mc;
                }
                // Some versions: Collection<ModConfig>
                if (o instanceof Collection<?> col) {
                    for (Object cfg : col) {
                        if (cfg instanceof ModConfig mc && mc.getType() == ModConfig.Type.CLIENT) {
                            return mc;
                        }
                    }
                }
            }

        } catch (Throwable t) {
            ModConstants.LOG.debug("[EZEP] tryGetClientConfig failed (non-fatal).", t);
        }
        return null;
    }

    private static boolean tryInvokeSaveOnConfig(ModConfig cfg) {
        try {
            if (cfg == null) return false;

            // Common method names (varies by impl)
            String[] names = new String[]{"save", "saveConfig", "saveToFile", "write", "writeConfig"};
            for (String n : names) {
                try {
                    Method m = cfg.getClass().getMethod(n);
                    m.invoke(cfg);
                    ModConstants.LOG.debug("[EZEP] Invoked ModConfig.{}()", n);
                    return true;
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (Throwable t) {
            ModConstants.LOG.debug("[EZEP] tryInvokeSaveOnConfig failed (non-fatal).", t);
        }
        return false;
    }

    private static boolean trySaveViaConfigTracker(ModConfig cfg) {
        try {
            if (cfg == null) return false;

            Class<?> trackerCls = Class.forName("net.neoforged.fml.config.ConfigTracker");
            Field instField = trackerCls.getField("INSTANCE");
            Object tracker = instField.get(null);
            if (tracker == null) return false;

            // Probe methods that accept ModConfig or (ModConfig.Type, String) etc.
            for (Method m : trackerCls.getMethods()) {
                String name = m.getName();
                Class<?>[] p = m.getParameterTypes();

                // Candidate methods: writeConfig(ModConfig), save(ModConfig), saveConfig(ModConfig)
                if ((name.equals("writeConfig") || name.equals("save") || name.equals("saveConfig") || name.equals("saveConfigFile"))
                        && p.length == 1
                        && p[0].getName().equals("net.neoforged.fml.config.ModConfig")) {
                    m.invoke(tracker, cfg);
                    ModConstants.LOG.debug("[EZEP] ConfigTracker.{}(ModConfig) invoked", name);
                    return true;
                }

                // Candidate: writeConfig(ModConfig, ...) etc: try only the 1-arg form above for safety.
            }

        } catch (Throwable t) {
            ModConstants.LOG.debug("[EZEP] trySaveViaConfigTracker failed (non-fatal).", t);
        }
        return false;
    }

    private static boolean trySaveAllContainerConfigs(ModContainer container) {
        try {
            if (container == null) return false;

            // Attempt to read a field holding configs
            for (Field f : container.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(container);
                    if (v instanceof Collection<?> col) {
                        boolean did = false;
                        for (Object o : col) {
                            if (o instanceof ModConfig mc) {
                                // Save only client config (or everything if you prefer; but we stay scoped)
                                if (mc.getType() == ModConfig.Type.CLIENT) {
                                    did |= tryInvokeSaveOnConfig(mc) || trySaveViaConfigTracker(mc);
                                }
                            }
                        }
                        if (did) return true;
                    }
                    if (v instanceof Map<?, ?> map) {
                        Object cfg = map.get(ModConfig.Type.CLIENT);
                        if (cfg instanceof ModConfig mc) {
                            return tryInvokeSaveOnConfig(mc) || trySaveViaConfigTracker(mc);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

        } catch (Throwable t) {
            ModConstants.LOG.debug("[EZEP] trySaveAllContainerConfigs failed (non-fatal).", t);
        }
        return false;
    }
}
