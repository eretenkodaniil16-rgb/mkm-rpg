package com.eretenkodaniil.mkm.client.animation;

import java.util.Locale;

/**
 * Diagnostic evidence collected AFTER PlayerModel.setupAnim's animation Mixin
 * actually changed part angles. It proves a pose was applied at setupAnim,
 * NOT that pixels were drawn or the pose is visually good.
 *
 * Owner-local and read by the client HUD; no server protocol or persistent data.
 */
public final class AnimationRenderProbe {
    private static long lastAppliedTick = Long.MIN_VALUE;
    private static String lastClip = "";
    private static double lastMaxAngleDelta = 0D;
    private static String lastRenderer = "UNKNOWN";
    private static String lastSelection = "";
    private static String lastReason = "";

    private AnimationRenderProbe() {}

    public static void mark(long gameTick, String clipId, double maxAngleDeltaRadians) {
        if (gameTick < 0L || clipId == null || clipId.isBlank()
                || !Double.isFinite(maxAngleDeltaRadians) || maxAngleDeltaRadians < 0D) return;
        lastAppliedTick = gameTick;
        lastClip = clipId;
        lastMaxAngleDelta = maxAngleDeltaRadians;
    }

    /**
     * Detailed outcome is written by the actual model setup, not guessed by
     * the HUD. ModelPart setup is NOT proof of framebuffer/item-layer output.
     */
    public static void markDecision(long tick, String actualClip, double changed,
                                    String renderer, WeaponPresentationResolver.Selection selected,
                                    String fallbackReason) {
        if (tick < 0L || selected == null) return;
        String clip=(actualClip == null || actualClip.isBlank()) ? "NONE" : actualClip;
        mark(tick,clip,Double.isFinite(changed) && changed>=0D ? changed : 0D);
        lastRenderer=renderer;
        lastReason=fallbackReason;
        lastSelection="main="+selected.mainFamily()
                +" off="+selected.offFamily()
                +" active="+selected.activeHand()+"/"+selected.activeArm()
                +" step="+selected.comboStep();
    }

    public static String diagnostic(long tick) {
        if (tick < 0 || lastAppliedTick < 0 || tick < lastAppliedTick || tick-lastAppliedTick>1)
            return "renderer=NOT_OBSERVED (use F5)";
        return "renderer="+lastRenderer+" clip="+lastClip+" reason="+lastReason;
    }

    public static String context(long tick) {
        if (tick < 0 || lastAppliedTick < 0 || tick < lastAppliedTick || tick-lastAppliedTick>1)
            return "main/off/active/step not observed";
        return lastSelection;
    }

    public static double maxAngleDelta(float[] before, float[] after) {
        if (before == null || after == null || before.length == 0
                || before.length != after.length) {
            throw new IllegalArgumentException("Invalid pose comparison arrays");
        }
        double max = 0D;
        for (int i = 0; i < before.length; i++) {
            if (!Float.isFinite(before[i]) || !Float.isFinite(after[i])) {
                throw new IllegalArgumentException("Nonfinite model part angle");
            }
            max = Math.max(max, Math.abs((double) after[i] - before[i]));
        }
        return max;
    }

    /**
     * Returns evidence for the EXPECTED clip only; a recent clip from a
     * previous combo stage cannot incorrectly mark this stage as observed.
     */
    public static String status(long gameTick, String expectedClip) {
        if (gameTick < 0L || lastAppliedTick < 0L
                || gameTick < lastAppliedTick || gameTick - lastAppliedTick > 1L) {
            return "POSE NOT OBSERVED (use F5)";
        }
        if (expectedClip == null || !lastClip.equals(expectedClip)) {
            return "DIFFERENT CLIP: " + lastClip;
        }
        if (lastMaxAngleDelta < 0.0001D) {
            return "NEUTRAL KEYFRAME " + lastClip;
        }
        return String.format(Locale.ROOT, "POSE APPLIED %s  %.1f deg",
                lastClip, Math.toDegrees(lastMaxAngleDelta));
    }
}
