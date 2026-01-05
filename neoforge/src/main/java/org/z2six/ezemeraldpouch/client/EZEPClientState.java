// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/client/EZEPClientState.java
package org.z2six.ezemeraldpouch.client;

import org.z2six.ezemeraldpouch.ModConstants;

public final class EZEPClientState {

    private static volatile long emeralds = 0L;

    private EZEPClientState() {
    }

    public static long getEmeralds() {
        return emeralds;
    }

    public static void setEmeralds(long value) {
        if (value < 0L) value = 0L;
        emeralds = value;
        ModConstants.LOG.debug("[EZEP] ClientState emeralds={}", emeralds);
    }
}
