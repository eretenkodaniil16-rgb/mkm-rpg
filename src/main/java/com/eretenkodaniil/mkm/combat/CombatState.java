package com.eretenkodaniil.mkm.combat;

/**
 * Temporary owner-only runtime state for action combat. Not saved to the character schema.
 * Time coordinates are logical server game ticks.
 */
public record CombatState(
        float stamina,
        long lastUpdateTick,
        long regenReadyTick,
        long heavyReadyTick,
        long dodgeReadyTick) {

    public CombatState {
        if (!Float.isFinite(stamina)) {
            throw new IllegalArgumentException("Stamina must be finite");
        }
        stamina = Math.max(0.0F, stamina);
    }

    public CombatState() {
        this(100.0F, 0L, 0L, 0L, 0L);
    }
}
