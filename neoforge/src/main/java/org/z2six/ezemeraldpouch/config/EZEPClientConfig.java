// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/config/EZEPClientConfig.java
package org.z2six.ezemeraldpouch.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EZEPClientConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    public static final ModConfigSpec.IntValue HUD_X;
    public static final ModConfigSpec.IntValue HUD_Y;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("hud");
        HUD_ENABLED = b.comment("Enable the always-on emerald pouch HUD")
                .define("enabled", true);

        HUD_X = b.comment("HUD X position (pixels from left)")
                .defineInRange("x", 6, 0, 100000);

        HUD_Y = b.comment("HUD Y position (pixels from top)")
                .defineInRange("y", 6, 0, 100000);

        HUD_SCALE = b.comment("HUD scale multiplier")
                .defineInRange("scale", 1.0, 0.25, 4.0);
        b.pop();

        SPEC = b.build();
    }

    private EZEPClientConfig() {
    }
}
