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
}
