// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/config/EZEPClientConfig.java
package org.z2six.ezemeraldpouch.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EZEPClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    public static final ModConfigSpec.IntValue HUD_X;
    public static final ModConfigSpec.IntValue HUD_Y;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;

    /**
     * Withdraw button placement INSIDE InventoryScreen:
     * stored as offsets relative to the inventory GUI top-left (InventoryScreen.getGuiLeft/getGuiTop).
     *
     * Default matches the original behavior:
     *   x = invXSize - 16 - 4  (top-right corner inside GUI)
     *   y = 4
     */
    public static final ModConfigSpec.IntValue WITHDRAW_BTN_OFFSET_X;
    public static final ModConfigSpec.IntValue WITHDRAW_BTN_OFFSET_Y;

    /**
     * Keybind settings: category + translation keys are in code; Minecraft stores the binding in options.
     * We keep config only for placements, not for the key itself.
     */

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

        b.push("inventory_withdraw_button");
        WITHDRAW_BTN_OFFSET_X = b.comment(
                        "Withdraw button X offset relative to the inventory GUI left.\n" +
                                "Example: if the inventory GUI is at (guiLeft, guiTop), the button will be at (guiLeft + offsetX, guiTop + offsetY)."
                )
                // allow large range so it works across different GUI scale / mods
                .defineInRange("offsetX", 156, -10000, 10000);

        WITHDRAW_BTN_OFFSET_Y = b.comment(
                        "Withdraw button Y offset relative to the inventory GUI top.\n" +
                                "Example: if the inventory GUI is at (guiLeft, guiTop), the button will be at (guiLeft + offsetX, guiTop + offsetY)."
                )
                .defineInRange("offsetY", 4, -10000, 10000);
        b.pop();

        SPEC = b.build();
    }

    private EZEPClientConfig() {
    }
}
