package com.eretenkodaniil.mkm.combat;

/**
 * Pure interpolation rules shared by client pose and third-person animation.
 * Damage and movement are never inferred from an animation frame.
 */
public final class CombatAnimationMath {
    private CombatAnimationMath() {}

    public static float phaseProgress(ActionTimeline state, long gameTick, float partialTick) {
        if (state == null || state.idle()) return 0.0F;
        long duration = switch (state.phase()) {
            case WINDUP -> ActionTimelineRules.windupTicks(state.comboStep());
            case ACTIVE -> 1L;
            case RECOVERY -> ActionTimelineRules.recoveryTicks(state.comboStep());
            case DODGING -> ActionTimelineRules.DODGE_ACTIVE_TICKS;
            case IDLE -> 1L;
        };
        if (duration <= 0) return 0F;
        double remaining = state.phaseUntilTick() - (gameTick + clamp01(partialTick));
        return (float) clamp01(1.0D - remaining / duration);
    }

    public static float poseWeight(ActionTimeline.Phase phase, float progress) {
        float t = smooth(progress);
        return switch (phase) {
            case WINDUP -> t * 0.8F;
            case ACTIVE -> 1.0F;
            case RECOVERY -> 1.0F - t;
            case DODGING -> (float) Math.sin(Math.PI * clamp01(progress));
            case IDLE -> 0.0F;
        };
    }

    public static float rollRadians(float progress) {
        // Smooth whole-body rotation from 0 to 2 PI, with zero angular speed at endpoints.
        return (float) (2.0D * Math.PI * smooth(progress));
    }

    /** Local-space roll axis projected into the avatar's horizontal plane. */
    public static float rollAxisX(byte direction) {
        return switch (direction) {
            case DodgeDirection.FORWARD, DodgeDirection.FORWARD_LEFT, DodgeDirection.FORWARD_RIGHT -> 1F;
            case DodgeDirection.BACKWARD, DodgeDirection.BACKWARD_LEFT, DodgeDirection.BACKWARD_RIGHT -> -1F;
            default -> 0F;
        };
    }

    public static float rollAxisZ(byte direction) {
        return switch (direction) {
            case DodgeDirection.LEFT, DodgeDirection.FORWARD_LEFT, DodgeDirection.BACKWARD_LEFT -> 1F;
            case DodgeDirection.RIGHT, DodgeDirection.FORWARD_RIGHT, DodgeDirection.BACKWARD_RIGHT -> -1F;
            default -> 0F;
        };
    }

    public static float smooth(float value) {
        float t = (float) clamp01(value);
        return t * t * (3F - 2F * t);
    }

    public static double clamp01(double value) {
        if (!Double.isFinite(value)) return 0D;
        return Math.max(0D, Math.min(1D, value));
    }
}
