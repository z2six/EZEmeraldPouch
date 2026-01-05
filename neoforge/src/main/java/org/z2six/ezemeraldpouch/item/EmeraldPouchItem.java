// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/item/EmeraldPouchItem.java
package org.z2six.ezemeraldpouch.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.z2six.ezemeraldpouch.ModConstants;

import java.util.List;

public final class EmeraldPouchItem extends Item {

    public EmeraldPouchItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Stores your emeralds infinitely."));
        tooltip.add(Component.literal("Auto-pickup + HUD counter + withdraw button."));
        tooltip.add(Component.literal("Shift+RMB emerald stacks to deposit."));
        if (flag.isAdvanced()) {
            tooltip.add(Component.literal("mod=" + ModConstants.MODID));
        }
    }
}
