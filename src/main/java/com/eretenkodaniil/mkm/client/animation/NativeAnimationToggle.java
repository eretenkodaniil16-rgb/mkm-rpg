package com.eretenkodaniil.mkm.client.animation;
/** A/B switch, client-local only; OFF by default to preserve existing C3.8 behavior. */
public final class NativeAnimationToggle {
    private static boolean enabled;
    private NativeAnimationToggle(){}
    public static boolean enabled(){return enabled;}
    public static boolean flip(){enabled=!enabled;return enabled;}
}
