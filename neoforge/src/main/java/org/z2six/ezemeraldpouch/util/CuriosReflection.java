// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/util/CuriosReflection.java
package org.z2six.ezemeraldpouch.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.z2six.ezemeraldpouch.ModConstants;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Curios reflection helpers (no hard dependency).
 *
 * IMPORTANT:
 * This implementation intentionally uses the same proven reflection path as our
 * quick-equip payload:
 *   CuriosApi.getCuriosInventory(LivingEntity) -> Optional handler
 *   handler.getStacksHandler("pouch") -> Optional stacksHandler
 *   stacksHandler.getStacks() -> IDynamicStackHandler
 *   dynamic.getSlots(), dynamic.getStackInSlot(i)
 *
 * This avoids older/changed APIs like handler.getCurios() which may not exist in newer Curios builds.
 */
public final class CuriosReflection {

    private static final String CURIOS_API = "top.theillusivec4.curios.api.CuriosApi";

    private CuriosReflection() {
    }

    /**
     * Returns true if the entity has an item equipped in Curios slot type "pouch" matching itemIdString.
     * itemIdString example: "ezemeraldpouch:emerald_pouch"
     */
    public static boolean hasEquippedPouch(LivingEntity entity, String itemIdString) {
        try {
            if (entity == null) return false;
            if (itemIdString == null || itemIdString.isBlank()) return false;

            if (!ModList.get().isLoaded("curios")) {
                return false;
            }

            Class<?> curiosApi = Class.forName(CURIOS_API);

            // CuriosApi.getCuriosInventory(LivingEntity)
            Method getInv = curiosApi.getMethod("getCuriosInventory", LivingEntity.class);
            Object invOptObj = getInv.invoke(null, entity);

            if (!(invOptObj instanceof Optional<?> invOpt) || invOpt.isEmpty()) {
                ModConstants.LOG.debug("[EZEP] CuriosReflection: getCuriosInventory empty for {}", entity.getName().getString());
                return false;
            }

            Object handler = invOpt.get();
            if (handler == null) return false;

            // handler.getStacksHandler("pouch")
            Method getStacksHandler = handler.getClass().getMethod("getStacksHandler", String.class);
            Object stacksOptObj = getStacksHandler.invoke(handler, "pouch");

            if (!(stacksOptObj instanceof Optional<?> stacksOpt) || stacksOpt.isEmpty()) {
                ModConstants.LOG.debug("[EZEP] CuriosReflection: no stacks handler for slotType 'pouch' on {}",
                        entity.getName().getString());
                return false;
            }

            Object stacksHandler = stacksOpt.get();
            if (stacksHandler == null) return false;

            // stacksHandler.getStacks() -> dynamic
            Method getStacks = stacksHandler.getClass().getMethod("getStacks");
            Object dynamic = getStacks.invoke(stacksHandler);
            if (dynamic == null) {
                ModConstants.LOG.debug("[EZEP] CuriosReflection: stacksHandler.getStacks returned null for {}",
                        entity.getName().getString());
                return false;
            }

            Method getSlots = dynamic.getClass().getMethod("getSlots");
            int slots = (int) getSlots.invoke(dynamic);
            if (slots <= 0) return false;

            Method getStackInSlot = dynamic.getClass().getMethod("getStackInSlot", int.class);

            for (int i = 0; i < slots; i++) {
                Object stObj = getStackInSlot.invoke(dynamic, i);
                if (!(stObj instanceof ItemStack stack)) continue;
                if (stack.isEmpty()) continue;

                ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                String id = (key == null) ? "" : key.toString();

                if (itemIdString.equals(id)) {
                    ModConstants.LOG.debug("[EZEP] CuriosReflection: found {} in Curios pouch slot index {} for {}",
                            id, i, entity.getName().getString());
                    return true;
                }
            }

            return false;

        } catch (ClassNotFoundException e) {
            // Curios not installed (or class name changed)
            return false;
        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Curios reflection check failed (safe to ignore). {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Curios reflection failure details", t);
            return false;
        }
    }
}
