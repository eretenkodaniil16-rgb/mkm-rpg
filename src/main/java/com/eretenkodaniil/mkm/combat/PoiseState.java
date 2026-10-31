package com.eretenkodaniil.mkm.combat;

/** Transient poise accumulated on living combat targets. */
public record PoiseState(float buildup, long lastHitTick, long staggeredUntilTick) {
    public PoiseState() {
        this(0.0F, 0L, 0L);
    }
}
