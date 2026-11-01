package com.eretenkodaniil.mkm.client.animation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnimationClipTest {
    private static AnimationClip.Rotation rot(double x) {
        return new AnimationClip.Rotation(x, 0D, 0D);
    }

    @Test
    void samplesSmoothlyWithoutMutatingAuthoredData() {
        AnimationClip clip = new AnimationClip("mkm:sword/test", 11,
                AnimationClip.Layer.UPPER_BODY,
                Map.of(AnimationClip.Bone.RIGHT_ARM, List.of(
                        new AnimationClip.Key(0D, rot(0D)),
                        new AnimationClip.Key(.25D, rot(-2D)),
                        new AnimationClip.Key(1D, rot(0D)))), List.of());
        assertEquals(0D, clip.sample(0D).get(AnimationClip.Bone.RIGHT_ARM).x(), 1e-8);
        assertEquals(-2D, clip.sample(.25D).get(AnimationClip.Bone.RIGHT_ARM).x(), 1e-8);
        assertEquals(-1D, clip.sample(.125D).get(AnimationClip.Bone.RIGHT_ARM).x(), 1e-8);
        assertEquals(0D, clip.sample(1D).get(AnimationClip.Bone.RIGHT_ARM).x(), 1e-8);
        assertEquals(0D, clip.sample(9D).get(AnimationClip.Bone.RIGHT_ARM).x(), 1e-8);
        assertThrows(UnsupportedOperationException.class,
                () -> clip.sample(.1D).clear());
    }

    @Test
    void rejectsMalformedOrNonFiniteTracks() {
        assertThrows(IllegalArgumentException.class,
                () -> new AnimationClip.Rotation(Double.NaN, 0D, 0D));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimationClip.Key(-.01D, rot(0)));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimationClip("mkm:invalid", 12, AnimationClip.Layer.UPPER_BODY,
                        Map.of(AnimationClip.Bone.RIGHT_ARM,
                                List.of(new AnimationClip.Key(0, rot(0)),
                                        new AnimationClip.Key(0, rot(1)))),
                        List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimationClip("mkm:invalid", 12, AnimationClip.Layer.UPPER_BODY,
                        Map.of(AnimationClip.Bone.RIGHT_LEG,
                                List.of(new AnimationClip.Key(0, rot(0)),
                                        new AnimationClip.Key(1, rot(1)))),
                        List.of()));
    }

    @Test
    void monotoneIntermediateKeyDoesNotArtificiallyStopMotion() {
        AnimationClip clip = new AnimationClip("mkm:sword/continuous", 14,
                AnimationClip.Layer.UPPER_BODY,
                Map.of(AnimationClip.Bone.RIGHT_ARM, List.of(
                        new AnimationClip.Key(0D, rot(0D)),
                        new AnimationClip.Key(0.5D, rot(0.5D)),
                        new AnimationClip.Key(1D, rot(1D)))), List.of());
        double epsilon = 0.0001D;
        double before = clip.sample(0.5D - epsilon).get(AnimationClip.Bone.RIGHT_ARM).x();
        double at = clip.sample(0.5D).get(AnimationClip.Bone.RIGHT_ARM).x();
        double after = clip.sample(0.5D + epsilon).get(AnimationClip.Bone.RIGHT_ARM).x();
        assertEquals(0.5D, at, 1e-10);
        assertTrue(before < at && after > at);
        assertTrue((after - before) / (2D * epsilon) > 0.9D,
                "Intermediate monotone key must retain velocity, not smoothstep to a stop");
        assertEquals((at - before) / epsilon, (after - at) / epsilon, 0.002D);
    }

    @Test
    void nonUniformReversalAndConstantAxesStayWithinAuthoredBounds() {
        AnimationClip clip = new AnimationClip("mkm:sword/reversal", 18,
                AnimationClip.Layer.UPPER_BODY,
                Map.of(AnimationClip.Bone.RIGHT_ARM, List.of(
                        new AnimationClip.Key(0D, new AnimationClip.Rotation(0D, .2D, 0D)),
                        new AnimationClip.Key(.15D, new AnimationClip.Rotation(1D, .2D, 0D)),
                        new AnimationClip.Key(.43D, new AnimationClip.Rotation(.2D, .2D, 0D)),
                        new AnimationClip.Key(1D, new AnimationClip.Rotation(.7D, .2D, 0D)))), List.of());
        for (int i = 0; i <= 1000; i++) {
            AnimationClip.Rotation pose = clip.sample(i / 1000D).get(AnimationClip.Bone.RIGHT_ARM);
            assertTrue(pose.x() >= 0D && pose.x() <= 1D);
            assertEquals(.2D, pose.y(), 1e-12);
            assertEquals(0D, pose.z(), 1e-12);
        }
        assertEquals(1D, clip.sample(.15D).get(AnimationClip.Bone.RIGHT_ARM).x(), 1e-12);
        assertEquals(.2D, clip.sample(.43D).get(AnimationClip.Bone.RIGHT_ARM).x(), 1e-12);
    }
}
