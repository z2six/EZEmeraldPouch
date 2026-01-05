// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/util/CuriosReflection.java
package org.z2six.ezemeraldpouch.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.z2six.ezemeraldpouch.ModConstants;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public final class CuriosReflection {

    private static final String CURIOS_API = "top.theillusivec4.curios.api.CuriosApi";

    private CuriosReflection() {
    }

    public static boolean hasEquippedPouch(LivingEntity entity, String itemIdString) {
        try {
            if (entity == null) return false;

            Class<?> curiosApi = Class.forName(CURIOS_API);
            Method getInv = curiosApi.getMethod("getCuriosInventory", LivingEntity.class);

            Object opt = getInv.invoke(null, entity);
            if (!(opt instanceof Optional<?> optional) || optional.isEmpty()) {
                return false;
            }

            Object handler = optional.get();
            Method getCurios = handler.getClass().getMethod("getCurios");
            Object curiosMapObj = getCurios.invoke(handler);
            if (!(curiosMapObj instanceof Map<?, ?> curiosMap)) {
                return false;
            }

            for (Object entryObj : curiosMap.entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObj;
                Object stacksHandler = entry.getValue();
                if (stacksHandler == null) continue;

                Method getStacks = stacksHandler.getClass().getMethod("getStacks");
                Object stacksObj = getStacks.invoke(stacksHandler);
                if (stacksObj == null) continue;

                Method getSlots = stacksObj.getClass().getMethod("getSlots");
                int slots = (int) getSlots.invoke(stacksObj);

                Method getStackInSlot = stacksObj.getClass().getMethod("getStackInSlot", int.class);
                for (int i = 0; i < slots; i++) {
                    Object stObj = getStackInSlot.invoke(stacksObj, i);
                    if (stObj instanceof ItemStack stack && !stack.isEmpty()) {
                        String id = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
                        if (itemIdString.equals(id)) {
                            return true;
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Curios reflection check failed (safe to ignore). {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Curios reflection failure details", t);
            return false;
        }
        return false;
    }
}
