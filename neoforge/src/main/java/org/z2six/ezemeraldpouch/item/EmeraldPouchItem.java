// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/item/EmeraldPouchItem.java
package org.z2six.ezemeraldpouch.item;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.z2six.ezemeraldpouch.client.EZEPClientState;

import java.util.List;

public final class EmeraldPouchItem extends Item {

    public EmeraldPouchItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Stores an infinite amount of Emeralds"));

        // Full (non-abbreviated) count, using client-synced value
        long emeralds = EZEPClientState.getEmeralds();
        tooltip.add(Component.literal("Amount: " + Long.toString(Math.max(0L, emeralds))));

        // Optional extra: show a hint for emerald blocks too
        if (flag.isAdvanced()) {
            tooltip.add(Component.literal("(Shift+RMB emeralds / emerald blocks to deposit)"));
        }
    }
}
