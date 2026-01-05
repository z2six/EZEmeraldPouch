// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/config/EZEPClientConfig.java
package org.z2six.ezemeraldpouch.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EZEPClientConfig {
    public static final ModConfigSpec SPEC;

    // HUD
    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    public static final ModConfigSpec.IntValue HUD_X;
    public static final ModConfigSpec.IntValue HUD_Y;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;

    // Withdraw button (inventory screen)
    public static final ModConfigSpec.BooleanValue WITHDRAW_BTN_ENABLED;
    public static final ModConfigSpec.IntValue WITHDRAW_BTN_OFFSET_X;
    public static final ModConfigSpec.IntValue WITHDRAW_BTN_OFFSET_Y;

    // Defaults (used by reset in placement editor)
    public static final int DEFAULT_HUD_X = 6;
    public static final int DEFAULT_HUD_Y = 6;
    public static final double DEFAULT_HUD_SCALE = 1.0;

    // Default button offsets relative to inventory GUI top-left.
    // Keep close to prior placement: top-right corner (approx).
    public static final int DEFAULT_WITHDRAW_BTN_OFFSET_X = 176 - 16 - 4; // invWidth - icon - pad
    public static final int DEFAULT_WITHDRAW_BTN_OFFSET_Y = 4;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("hud");
        HUD_ENABLED = b.comment("Enable the always-on emerald pouch HUD")
                .define("enabled", true);

        HUD_X = b.comment("HUD X position (pixels from left)")
                .defineInRange("x", DEFAULT_HUD_X, -100000, 100000);

        HUD_Y = b.comment("HUD Y position (pixels from top)")
                .defineInRange("y", DEFAULT_HUD_Y, -100000, 100000);

        HUD_SCALE = b.comment("HUD scale multiplier")
                .defineInRange("scale", DEFAULT_HUD_SCALE, 0.25, 4.0);
        b.pop();

        b.push("withdraw_button");
        WITHDRAW_BTN_ENABLED = b.comment("Show the withdraw button on the player inventory screen")
                .define("enabled", true);

        WITHDRAW_BTN_OFFSET_X = b.comment("Withdraw button X offset relative to the inventory GUI left")
                .defineInRange("offsetX", DEFAULT_WITHDRAW_BTN_OFFSET_X, -100000, 100000);

        WITHDRAW_BTN_OFFSET_Y = b.comment("Withdraw button Y offset relative to the inventory GUI top")
                .defineInRange("offsetY", DEFAULT_WITHDRAW_BTN_OFFSET_Y, -100000, 100000);
        b.pop();

        SPEC = b.build();
    }

    private EZEPClientConfig() {
    }
}
