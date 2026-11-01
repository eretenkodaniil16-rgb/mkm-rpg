package com.eretenkodaniil.mkm.combat;

/**
 * Third-person choreography for a forward shoulder roll. This is strictly a
 * VISUAL transform; server velocity, collision, damage and stamina are not touched.
 *
 * <p>Model local forward is -Z. A forward somersault therefore rotates around
 * NEGATIVE local X (the previous positive X animated a backflip while traveling
 * forward). The bounding silhouette is kept above its reference ground plane
 * by an analytical vertical correction.</p>
 */
public final class ForwardRollChoreography {
    public static final float PIVOT_HEIGHT = 0.90F;
    public static final float HALF_HEIGHT = 0.90F;
    public static final float HALF_DEPTH = 0.38F;
    private static final double TURN = Math.PI * 2.0D;

    public record Pose(float radians, float visualYOffset, float axisX, float axisZ) {}

    private ForwardRollChoreography() {}

    /**
     * 6% initial plant, 70% active rotation, 24% settling. The main rotation
     * overlaps the server's strong early impulse, unlike an even full-duration
     * spin that continues after the character has nearly stopped moving.
     */
    public static Pose sample(float progress) {
        double p = CombatAnimationMath.clamp01(progress);
        double normalized = CombatAnimationMath.clamp01((p - 0.06D) / 0.70D);
        // Smoothstep gives zero angular velocity at the planted and settled poses.
        double spin = normalized * normalized * (3D - 2D * normalized);
        double angle = TURN * spin;

        // Model approximated by a vertically symmetric 1.8x0.76 block
        // centered 0.9 above ground. At angle, its lowest projected point is:
        // pivot - (halfHeight*|cos| + halfDepth*|sin|) + verticalCorrection.
        // Solve for the correction that keeps the lowest point at y=0.
        double radius = HALF_HEIGHT * Math.abs(Math.cos(angle))
                + HALF_DEPTH * Math.abs(Math.sin(angle));
        double correction = radius - PIVOT_HEIGHT;
        return new Pose((float) angle, (float) correction, -1F, 0F);
    }

    public static double silhouetteMinHeight(Pose pose) {
        double projected = HALF_HEIGHT * Math.abs(Math.cos(pose.radians()))
                + HALF_DEPTH * Math.abs(Math.sin(pose.radians()));
        return PIVOT_HEIGHT + pose.visualYOffset() - projected;
    }
}
