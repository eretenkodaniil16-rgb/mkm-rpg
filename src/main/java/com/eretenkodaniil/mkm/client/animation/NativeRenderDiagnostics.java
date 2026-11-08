package com.eretenkodaniil.mkm.client.animation;

/**
 * Renderer-level telemetry. A render-layer submit proves the client submitted
 * geometry; it is not evidence of framebuffer visibility or visual acceptance.
 */
public final class NativeRenderDiagnostics {
    private static volatile long lastTick=Long.MIN_VALUE;
    private static volatile String renderer="NONE";
    private static volatile String clip="NONE";
    private static volatile String cause="NO_RENDER";
    private NativeRenderDiagnostics(){}
    public static void mark(long tick,String selectedRenderer,String appliedClip,String reason){
        if(tick<0)return;
        lastTick=tick;renderer=selectedRenderer;clip=appliedClip;cause=reason;
    }
    public static String status(long now){
        if(now<0||lastTick<0||now<lastTick||now-lastTick>2)return "R07 no pose submitted (F5)";
        return renderer+" "+clip+" "+cause;
    }
}
