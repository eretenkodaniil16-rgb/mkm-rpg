package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.DodgeDirection;

/**
 * Cosmetic-only three-category evasions. No 360-degree rigid avatar flips.
 * Server dodge velocity, duration, immunity and collision remain untouched.
 */
public final class EvasionChoreography {
    public enum Kind { FORWARD_TUCK, LATERAL_STEP, BACKWARD_HOP }
    public record Pose(Kind kind,float pitch,float roll,float yaw,float lift,
                       float tuck,float stride) {}
    private EvasionChoreography(){}
    public static Pose sample(byte direction,double progress){
        if(direction<0||direction>DodgeDirection.LAST)
            throw new IllegalArgumentException("Unknown dodge direction");
        if(!Double.isFinite(progress))throw new IllegalArgumentException("Non-finite progress");
        double t=Math.clamp(progress,0D,1D);
        double smooth=t*t*(3D-2D*t);
        // Smooth onset and departure; no visible pop when entering C3 fallback.
        float s=(float)Math.sin(Math.PI*smooth);
        boolean forward=direction==DodgeDirection.FORWARD
                || direction==DodgeDirection.FORWARD_LEFT||direction==DodgeDirection.FORWARD_RIGHT;
        boolean backward=direction==DodgeDirection.BACKWARD
                || direction==DodgeDirection.BACKWARD_LEFT||direction==DodgeDirection.BACKWARD_RIGHT;
        int lateral= switch(direction) {
            case DodgeDirection.LEFT,DodgeDirection.FORWARD_LEFT,DodgeDirection.BACKWARD_LEFT -> -1;
            case DodgeDirection.RIGHT,DodgeDirection.FORWARD_RIGHT,DodgeDirection.BACKWARD_RIGHT -> 1;
            default -> 0;
        };
        if(forward)return new Pose(Kind.FORWARD_TUCK,
                -.68F*s,.11F*lateral*s,.10F*lateral*s,-.10F*s,.95F*s,.30F*s);
        if(backward)return new Pose(Kind.BACKWARD_HOP,
                .28F*s,.06F*lateral*s,.08F*lateral*s,.075F*s,.40F*s,-.38F*s);
        return new Pose(Kind.LATERAL_STEP,
                .08F*s,.27F*lateral*s,.17F*lateral*s,.015F*s,.22F*s,.55F*lateral*s);
    }
}
