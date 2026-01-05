// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/data/EmeraldPouchData.java
package org.z2six.ezemeraldpouch.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.z2six.ezemeraldpouch.ModConstants;

public final class EmeraldPouchData {

    public static final Codec<EmeraldPouchData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.LONG.optionalFieldOf("emeralds", 0L).forGetter(EmeraldPouchData::getEmeralds)
    ).apply(inst, EmeraldPouchData::new));

    private long emeralds;

    public EmeraldPouchData() {
        this(0L);
    }

    public EmeraldPouchData(long emeralds) {
        this.emeralds = Math.max(0L, emeralds);
    }

    public long getEmeralds() {
        return emeralds;
    }

    public void setEmeralds(long emeralds) {
        long fixed = Math.max(0L, emeralds);
        if (fixed != emeralds) {
            ModConstants.LOG.debug("[EZEP] setEmeralds clamped {} -> {}", emeralds, fixed);
        }
        this.emeralds = fixed;
    }

    public void deposit(long amount) {
        if (amount <= 0L) {
            ModConstants.LOG.debug("[EZEP] deposit ignored amount={}", amount);
            return;
        }
        long before = this.emeralds;
        long after;
        try {
            after = Math.addExact(before, amount);
        } catch (ArithmeticException ex) {
            after = Long.MAX_VALUE;
            ModConstants.LOG.warn("[EZEP] deposit overflow: before={} add={} -> clamped to Long.MAX_VALUE", before, amount);
        }
        this.emeralds = after;
        ModConstants.LOG.debug("[EZEP] deposit: +{} ({} -> {})", amount, before, after);
    }

    public long withdrawUpTo(long amount) {
        if (amount <= 0L) {
            ModConstants.LOG.debug("[EZEP] withdrawUpTo ignored amount={}", amount);
            return 0L;
        }
        long before = this.emeralds;
        long taken = Math.min(before, amount);
        this.emeralds = before - taken;
        ModConstants.LOG.debug("[EZEP] withdrawUpTo: -{} ({} -> {})", taken, before, this.emeralds);
        return taken;
    }
}
