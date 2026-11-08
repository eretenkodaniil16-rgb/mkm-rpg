package com.eretenkodaniil.mkm.client.animation;

/** Client-only F8 comparison: C3.7 vs native six-part articulated projection. */
public final class NativeAnimationToggle {
    private static boolean enabled;
    private NativeAnimationToggle(){}
    public static boolean enabled(){return enabled;}
    public static boolean flip(){return enabled=!enabled;}
    public static void reset(){enabled=false;}
}
