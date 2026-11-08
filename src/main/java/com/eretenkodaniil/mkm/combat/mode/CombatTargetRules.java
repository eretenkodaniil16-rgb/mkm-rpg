package com.eretenkodaniil.mkm.combat.mode;

/**
 * Bounded server-target cone approximation for the first opt-in combat slice.
 * Not an animation-tip hitbox or a substitute for native vanilla collision.
 */
public final class CombatTargetRules {
    public static final double RANGE = 3.35D;
    public static final double MIN_DOT = 0.84D;
    private CombatTargetRules() {}
    public static boolean valid(double distanceSquared, double facingDot, boolean lineOfSight) {
        return lineOfSight && Double.isFinite(distanceSquared)
                && Double.isFinite(facingDot) && distanceSquared > 1e-5D
                && distanceSquared <= RANGE * RANGE
                && facingDot >= MIN_DOT;
    }
}
