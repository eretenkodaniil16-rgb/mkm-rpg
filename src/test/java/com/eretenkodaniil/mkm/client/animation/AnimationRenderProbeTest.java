package com.eretenkodaniil.mkm.client.animation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnimationRenderProbeTest {
    @Test
    void modelDeltaIsMeasuredNotGuessed() {
        assertEquals(.75D, AnimationRenderProbe.maxAngleDelta(
                new float[]{0F, .5F, -1F}, new float[]{0F, 1.25F, -1.5F}), 1e-8);
        assertEquals(0D, AnimationRenderProbe.maxAngleDelta(
                new float[]{.4F, .5F}, new float[]{.4F, .5F}), 1e-8);
        assertThrows(IllegalArgumentException.class, () ->
                AnimationRenderProbe.maxAngleDelta(new float[]{0F}, new float[2]));
        assertThrows(IllegalArgumentException.class, () ->
                AnimationRenderProbe.maxAngleDelta(new float[]{Float.NaN}, new float[]{0F}));
    }

    @Test
    void onlySameClipRecentAndNonNeutralSamplesCountAsApplied() {
        long tick = 400_000L;
        AnimationRenderProbe.mark(tick, "mkm:sword/light_1", .5D);
        assertTrue(AnimationRenderProbe.status(tick, "mkm:sword/light_1")
                .startsWith("POSE APPLIED mkm:sword/light_1"));
        assertEquals("DIFFERENT CLIP: mkm:sword/light_1",
                AnimationRenderProbe.status(tick, "mkm:sword/light_2"));
        assertEquals("POSE NOT OBSERVED (use F5)",
                AnimationRenderProbe.status(tick + 2L, "mkm:sword/light_1"));
        AnimationRenderProbe.mark(tick + 3L, "mkm:unarmed/jab", 0D);
        assertEquals("NEUTRAL KEYFRAME mkm:unarmed/jab",
                AnimationRenderProbe.status(tick + 3L, "mkm:unarmed/jab"));
        AnimationRenderProbe.mark(tick + 4L, "mkm:unarmed/jab", Double.NaN);
        assertEquals("NEUTRAL KEYFRAME mkm:unarmed/jab",
                AnimationRenderProbe.status(tick + 3L, "mkm:unarmed/jab"));
    }
    @Test
    void diagnosticReportsActualRendererAndBothHandsRatherThanSelectedToggle() {
        var selected=new WeaponPresentationResolver.Selection(
                WeaponPresentationResolver.Family.SWORD,
                WeaponPresentationResolver.Family.SHIELD,
                net.minecraft.world.InteractionHand.MAIN_HAND,
                net.minecraft.world.entity.HumanoidArm.LEFT,
                WeaponPresentationResolver.Renderer.MNAE,
                "mkm:one_handed_sword","mkm:mnae/sword_cut","NATIVE_DEMO",1);
        AnimationRenderProbe.markDecision(950000,"mkm:mnae/sword_cut",.3D,
                "MNAE",selected,"NATIVE_DEMO");
        assertTrue(AnimationRenderProbe.diagnostic(950000).contains("renderer=MNAE"));
        assertTrue(AnimationRenderProbe.diagnostic(950000).contains("mkm:mnae/sword_cut"));
        assertTrue(AnimationRenderProbe.diagnostic(950000).contains("reason=NATIVE_DEMO"));
        assertTrue(AnimationRenderProbe.context(950000).contains("off=SHIELD"));
        assertTrue(AnimationRenderProbe.context(950000).contains("MAIN_HAND/LEFT"));
        assertTrue(AnimationRenderProbe.context(950000).contains("step=1"));
        assertTrue(AnimationRenderProbe.diagnostic(950002).contains("NOT_OBSERVED"));
    }

}
