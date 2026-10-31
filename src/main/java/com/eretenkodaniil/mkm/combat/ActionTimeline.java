package com.eretenkodaniil.mkm.combat;

/**
 * Volatile server-authoritative action state, owner-synchronized for HUD and
 * client pose extraction. Timestamps use server game ticks; world persistence
 * and client-selected displacements are intentionally excluded.
 */
public record ActionTimeline(
        Kind kind, Phase phase, int comboStep, long phaseUntilTick,
        long comboUntilTick, byte dodgeDirection) {
    public enum Kind { NONE, LIGHT_COMBO, DODGE }
    public enum Phase { IDLE, WINDUP, ACTIVE, RECOVERY, DODGING }

    public ActionTimeline {
        if (kind == null || phase == null) {
            throw new IllegalArgumentException("Action enums cannot be null");
        }
        if (comboStep < 0 || comboStep > 3) {
            throw new IllegalArgumentException("Combo step out of range");
        }
        if (dodgeDirection < 0 || dodgeDirection > DodgeDirection.LAST) {
            throw new IllegalArgumentException("Dodge direction out of range");
        }
    }

    // Keep C2.0's existing timeline construction call sites compatible.
    public ActionTimeline(Kind kind, Phase phase, int step, long until, long comboUntil) {
        this(kind, phase, step, until, comboUntil, DodgeDirection.FORWARD);
    }

    public ActionTimeline() {
        this(Kind.NONE, Phase.IDLE, 0, 0L, 0L, DodgeDirection.FORWARD);
    }

    public boolean idle() {
        return phase == Phase.IDLE;
    }
}
