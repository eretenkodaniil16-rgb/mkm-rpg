package com.eretenkodaniil.mkm.combat;

/** Combat-owned defensive technique policy, separate from persistent character progression. */
public final class WarriorBraceRules {
    public static final float BASE_COST = 24.0F;
    public static final long COOLDOWN_TICKS = 180L;
    public static final int DURATION_TICKS = 60;
    public static final int TRAINED_DURATION_TICKS = 100;

    private WarriorBraceRules() {}

    public static boolean eligible(boolean warrior, boolean active, boolean grounded,
                                   boolean holdingShield, boolean timelineIdle,
                                   long now, long readyTick) {
        return warrior && active && grounded && holdingShield && timelineIdle
                && now >= readyTick && now >= 0;
    }
    public static int duration(boolean trained) {
        return trained ? TRAINED_DURATION_TICKS : DURATION_TICKS;
    }
}
