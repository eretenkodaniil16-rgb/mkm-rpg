package com.eretenkodaniil.mkm.client.animation;

/**
 * Visual-only lower-body blend for attacks. When the player walks, vanilla
 * locomotion must win over authored stationary stance offsets. Dodge tracks
 * intentionally bypass this gate because their full-body choreography was
 * authored separately.
 */
public final class CombatStanceMath {
    private static final double STILL_SPEED = 0.025D;
    private static final double MOVING_SPEED = 0.25D;
    private static final double MOVING_WEIGHT = 0.20D;

    private CombatStanceMath() {}

    /**
     * @param grounded local client's grounded state
     * @param horizontalSpeedSquared length squared of current horizontal velocity
     * @return finite grounded weight in [0.2,1], used ONLY on authored attack leg offsets
     */
    public static float attackLegWeight(boolean grounded, double horizontalSpeedSquared) {
        if (!grounded || !Double.isFinite(horizontalSpeedSquared) || horizontalSpeedSquared < 0D) {
            return 0F;
        }
        double speed = Math.sqrt(horizontalSpeedSquared);
        double t = Math.max(0D, Math.min(1D,
                (speed - STILL_SPEED) / (MOVING_SPEED - STILL_SPEED)));
        double smooth = t * t * (3D - 2D * t);
        return (float) (1D - (1D - MOVING_WEIGHT) * smooth);
    }
}
