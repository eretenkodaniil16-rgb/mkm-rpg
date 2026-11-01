package com.eretenkodaniil.mkm.client.animation;

/**
 * Client-side proof that the local third-person player-model mixin actually
 * applied a combat clip. Server timelines alone are not rendering proof.
 * Written on the render thread, read by the HUD on the same client.
 */
public final class AnimationRenderProbe {
    private static long lastRenderedTick = Long.MIN_VALUE;
    private static String lastClip = "";

    private AnimationRenderProbe() {}

    public static void mark(long gameTick, String clipId) {
        if (gameTick < 0L || clipId == null || clipId.isBlank()) return;
        lastRenderedTick = gameTick;
        lastClip = clipId;
    }

    public static String status(long gameTick) {
        if (gameTick < 0L || lastRenderedTick < 0L
                || gameTick < lastRenderedTick || gameTick - lastRenderedTick > 2L) {
            return "NOT RENDERED (use F5)";
        }
        return "RENDERED " + lastClip;
    }
}
