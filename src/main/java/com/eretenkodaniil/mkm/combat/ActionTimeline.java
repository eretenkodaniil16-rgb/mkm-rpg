package com.eretenkodaniil.mkm.combat;

/**
 * Volatile server-authoritative action state, owner-synchronized for HUD.
 * Timestamps use server game ticks; no world persistence or client authority.
 */
public record ActionTimeline(
        Kind kind, Phase phase, int comboStep, long phaseUntilTick, long comboUntilTick) {
    public enum Kind { NONE, LIGHT_COMBO, DODGE }
    public enum Phase { IDLE, WINDUP, ACTIVE, RECOVERY, DODGING }

    public ActionTimeline {
        if (kind == null || phase == null) throw new IllegalArgumentException("Action enums cannot be null");
        if (comboStep < 0 || comboStep > 3) throw new IllegalArgumentException("Combo step out of range");
    }

    public ActionTimeline() {
        this(Kind.NONE, Phase.IDLE, 0, 0L, 0L);
    }

    public boolean idle() {
        return phase == Phase.IDLE;
    }
}
