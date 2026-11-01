package com.eretenkodaniil.mkm.combat;

/**
 * Visual-only directional dodge roll pose. A conservative rectangular
 * silhouette remains on a reference ground plane without moving the server
 * hitbox or the player's actual feet. Forward retains its C3.1 authored
 * shoulder-roll choreography in ForwardRollChoreography.
 */
public final class DirectionalRollChoreography {
    public static final float PIVOT_HEIGHT = 0.90F;
    private static final double HALF_HEIGHT = 0.90D;
    private static final double HALF_WIDTH = 0.38D;
    private static final double HALF_DEPTH = 0.38D;
    private static final double FULL_TURN = 2D * Math.PI;

    private DirectionalRollChoreography() {}

    public record Pose(float radians, float visualYOffset, float axisX, float axisZ) {}

    public static Pose sample(byte direction, float progress) {
        if (direction < DodgeDirection.FORWARD || direction > DodgeDirection.LAST) {
            return new Pose(0F, 0F, -1F, 0F);
        }
        float x = -CombatAnimationMath.rollAxisX(direction);
        float z = CombatAnimationMath.rollAxisZ(direction);
        float norm = (float) Math.hypot(x, z);
        if (norm < .0001F) return new Pose(0F, 0F, -1F, 0F);
        x /= norm;
        z /= norm;
        // 6% foot plant, 78% rotation, 16% recovery into the standing pose.
        double t = CombatAnimationMath.clamp01((CombatAnimationMath.clamp01(progress) - .06D) / .78D);
        double ease = t * t * (3D - 2D * t);
        double angle = FULL_TURN * ease;
        // For a cuboid rotating around n=(x,0,z), the vertical projection
        // radius is h*|cos| + |sin|*(w*|z| + d*|x|).
        double radius = projectedRadius(angle, x, z);
        return new Pose((float) angle, (float) (radius - PIVOT_HEIGHT), x, z);
    }

    private static double projectedRadius(double angle, double axisX, double axisZ) {
        return HALF_HEIGHT * Math.abs(Math.cos(angle))
                + Math.abs(Math.sin(angle))
                * (HALF_WIDTH * Math.abs(axisZ) + HALF_DEPTH * Math.abs(axisX));
    }

    public static double silhouetteMinHeight(Pose pose) {
        return PIVOT_HEIGHT + pose.visualYOffset()
                - projectedRadius(pose.radians(), pose.axisX(), pose.axisZ());
    }
}
