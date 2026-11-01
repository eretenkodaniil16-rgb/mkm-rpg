package com.eretenkodaniil.mkm.client.animation;

import static org.junit.jupiter.api.Assertions.*;
import com.eretenkodaniil.mkm.combat.ActionTimelineRules;
import org.junit.jupiter.api.Test;

class CinematicAttackClipTest {
    @Test
    void allSixAttackClipsContainBalancedFullBodyFootwork() {
        AnimationLibrary library = AnimationLibrary.loadClasspath();
        String[] clips = {"mkm:sword/light_1", "mkm:sword/light_2", "mkm:sword/light_3",
                "mkm:unarmed/jab", "mkm:unarmed/cross", "mkm:unarmed/uppercut"};
        for (int i = 0; i < clips.length; i++) {
            AnimationClip clip = library.clip(clips[i]);
            assertNotNull(clip, clips[i]);
            assertEquals(AnimationClip.Layer.FULL_BODY, clip.layer(), clips[i]);
            assertTrue(clip.tracks().containsKey(AnimationClip.Bone.LEFT_LEG), clips[i]);
            assertTrue(clip.tracks().containsKey(AnimationClip.Bone.RIGHT_LEG), clips[i]);
            assertEquals(0D, clip.sample(0D).get(AnimationClip.Bone.LEFT_LEG).x(), 1e-8);
            assertEquals(0D, clip.sample(1D).get(AnimationClip.Bone.RIGHT_LEG).x(), 1e-8);
            int step = i % 3 + 1;
            int full = (int) (ActionTimelineRules.windupTicks(step) + 1
                    + ActionTimelineRules.recoveryTicks(step));
            double hitTime = ActionTimelineRules.windupTicks(step) / (double) full;
            double left = clip.sample(hitTime).get(AnimationClip.Bone.LEFT_LEG).x();
            double right = clip.sample(hitTime).get(AnimationClip.Bone.RIGHT_LEG).x();
            assertTrue(Math.abs(left) + Math.abs(right) > .30D, clips[i]);
            assertTrue(clip.markers().stream().anyMatch(m -> m.name().equals("impact")
                    && Math.abs(m.time() - hitTime) < .001D), clips[i]);
            for (int k = 0; k <= 200; k++) {
                double t = k / 200D;
                for (AnimationClip.Bone leg : new AnimationClip.Bone[]{
                        AnimationClip.Bone.LEFT_LEG, AnimationClip.Bone.RIGHT_LEG}) {
                    AnimationClip.Rotation r = clip.sample(t).get(leg);
                    assertTrue(Math.abs(r.x()) <= .52D && Math.abs(r.y()) <= .09D
                            && Math.abs(r.z()) <= .16D,
                            clips[i] + " excessive leg rotation at " + t);
                }
            }
        }
    }
}
