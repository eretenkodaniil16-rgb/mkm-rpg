package com.eretenkodaniil.mkm.client.animation;

/**
 * In-memory diagnostics ONLY. Marked by actual rendering callbacks;
 * neither a source of hits nor framebuffer capture. Avoid "RENDERED" claims:
 * submission of a GPU mesh is not proof of on-screen visible pixels.
 */
public final class MnaEVisibleProbe {
    private static long lastTick=Long.MIN_VALUE;
    private static String state="NO_RENDER_CALLBACK";
    private static String clip="";
    private MnaEVisibleProbe(){}
    public static void mark(long tick,String renderer,String clipId){
        if(tick<0||renderer==null||clipId==null)return;
        lastTick=tick;state=renderer;clip=clipId;
    }
    public static String status(long tick){
        return tick>=lastTick && lastTick>=0 && tick-lastTick<=2
                ? state+" SUBMITTED "+clip : "R07 NOT SUBMITTED (F5)";
    }
    public static void reset(){lastTick=Long.MIN_VALUE;state="NO_RENDER_CALLBACK";clip="";}
}
