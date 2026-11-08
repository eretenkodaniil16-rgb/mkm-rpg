package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.DodgeDirection;

/**
 * Three visually distinct, bounded client-only dodge families. These are
 * conservative posture prototypes, NOT a finished IK shoulder-contact roll.
 * Server impulse, stamina, hitbox and damage are unchanged.
 */
public final class DodgeVisualChoreography {
    public enum Family { FORWARD_TUCK, SIDE_SLIDE, BACK_HOP }
    public record Pose(Family family,float pitch,float yaw,float bank,float lift) {}
    private DodgeVisualChoreography(){}
    public static Pose sample(byte direction,float progress) {
        if(direction<DodgeDirection.FORWARD||direction>DodgeDirection.LAST)
            return new Pose(Family.FORWARD_TUCK,0,0,0,0);
        double p=Double.isFinite(progress)?Math.clamp(progress,0F,1F):0D;
        double pulse=Math.pow(Math.sin(Math.PI*p),2);
        boolean back=direction==DodgeDirection.BACKWARD
                ||direction==DodgeDirection.BACKWARD_LEFT
                ||direction==DodgeDirection.BACKWARD_RIGHT;
        boolean straightSide=direction==DodgeDirection.LEFT||direction==DodgeDirection.RIGHT;
        int horizontal=switch(direction){
            case DodgeDirection.LEFT,DodgeDirection.FORWARD_LEFT,
                    DodgeDirection.BACKWARD_LEFT->1;
            case DodgeDirection.RIGHT,DodgeDirection.FORWARD_RIGHT,
                    DodgeDirection.BACKWARD_RIGHT->-1;
            default->0;
        };
        double diagonal=(straightSide||horizontal==0)?1D:.72D;
        if(back){
            return new Pose(Family.BACK_HOP,(float)(.34*pulse),
                    (float)(horizontal*.12*pulse),(float)(-horizontal*.13*pulse),
                    (float)(.12*pulse));
        }
        if(straightSide){
            return new Pose(Family.SIDE_SLIDE,(float)(-.08*pulse),
                    (float)(horizontal*.15*pulse),(float)(-horizontal*.46*pulse),
                    (float)(-.045*pulse));
        }
        return new Pose(Family.FORWARD_TUCK,(float)(-1.04*pulse*diagonal),
                (float)(horizontal*.17*pulse),(float)(-horizontal*.22*pulse),
                (float)(-.085*pulse));
    }
}
