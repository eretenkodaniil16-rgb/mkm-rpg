package com.eretenkodaniil.mkm.client.animation.engine;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.client.animation.AnimationClip;
import com.eretenkodaniil.mkm.client.animation.AnimationLibrary;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Independent integration-gate tests. Failing tests indicate unsupported
 * mathematical contracts, not a mandate to change accepted gameplay timings.
 */
final class NativeAnimationCoreContractTest {
    private static final double EPS = 1e-8;

    private static void assertVec(Vec3 expected, Vec3 actual, double tol) {
        assertAll(
            () -> assertEquals(expected.x(), actual.x(), tol),
            () -> assertEquals(expected.y(), actual.y(), tol),
            () -> assertEquals(expected.z(), actual.z(), tol));
    }

    private static Vec3 apply(Transform t, Vec3 point) {
        return t.translation().add(t.rotation().rotate(t.scale().multiply(point)));
    }

    @Test
    void quaternionEulerAndSlerpPreserveKnownAxisRotations() {
        Quat yaw = Quat.euler(0, Math.PI / 2, 0);
        assertVec(new Vec3(0, 0, -1), yaw.rotate(new Vec3(1, 0, 0)), EPS);
        assertVec(new Vec3(1, 0, 0), yaw.rotate(new Vec3(0, 0, 1)), EPS);
        assertVec(new Vec3(1, 0, 0), Quat.IDENTITY.rotate(new Vec3(1, 0, 0)), EPS);
        assertVec(new Vec3(0, 0, -1),
            Quat.IDENTITY.slerp(Quat.euler(0, Math.PI, 0), .5)
                .rotate(new Vec3(1, 0, 0)), EPS);
        assertThrows(IllegalArgumentException.class,
            () -> new Quat(0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
            () -> Quat.euler(Double.NaN, 0, 0));
    }

    @Test
    void skeletonRejectsCyclesAndUsesParentPose() {
        assertThrows(IllegalArgumentException.class, () -> new Skeleton(List.of(
            new Skeleton.Bone("a", "b", Vec3.ZERO, Transform.IDENTITY),
            new Skeleton.Bone("b", "a", Vec3.ZERO, Transform.IDENTITY))));
        assertThrows(IllegalArgumentException.class, () -> new Skeleton(List.of(
            new Skeleton.Bone("a", "missing", Vec3.ZERO, Transform.IDENTITY))));
        Skeleton rig = new Skeleton(List.of(
            new Skeleton.Bone("root", null, Vec3.ZERO, Transform.IDENTITY),
            new Skeleton.Bone("hand", "root", new Vec3(2, 0, 0), Transform.IDENTITY)));
        Map<String, Transform> motion = Map.of("root",
            new Transform(Vec3.ZERO, Quat.euler(0, 0, Math.PI / 2), Vec3.ONE));
        assertVec(new Vec3(0, 2, 0),
            rig.world("hand", motion).translation(), EPS);
    }

    @Test
    void nonUniformScaleAndChildRotationCannotSilentlyLoseShear() {
        Transform parent = new Transform(Vec3.ZERO,
            Quat.euler(0, 0, Math.PI / 2), new Vec3(2, 1, 1));
        Transform child = new Transform(Vec3.ZERO,
            Quat.euler(0, 0, Math.PI / 2), Vec3.ONE);
        Vec3 point = new Vec3(1, 0, 0);
        Vec3 expected = apply(parent, apply(child, point));
        Vec3 actual = apply(parent.compose(child), point);
        assertVec(expected, actual, EPS);
    }

    @Test
    void additiveAndOverrideMasksHaveDeterministicWeights() {
        Transform swing = new Transform(new Vec3(2, 0, 0),
            Quat.euler(0, .4, 0), Vec3.ONE);
        var hidden = new AnimationLayer(Map.of("arm", swing), 1,
            java.util.Set.of("leg"), 1, AnimationLayer.Mode.OVERRIDE);
        assertTrue(PoseMixer.mix(Map.of(), List.of(hidden)).isEmpty());
        var upper = new AnimationLayer(Map.of("arm", swing), 1,
            java.util.Set.of("arm"), .5, AnimationLayer.Mode.ADDITIVE);
        var result = PoseMixer.mix(Map.of(), List.of(upper));
        assertEquals(1, result.get("arm").translation().x(), EPS);
        assertVec(new Vec3(1, 0, 0),
            result.get("arm").rotation().rotate(new Vec3(1, 0, 0)),
            .25);
    }

    @Test
    void bundledMnaeClipsArePresentAndImpactIsPresentationOnly() {
        NativeClip sword = NativeDemoLibrary.sword();
        NativeClip jab = NativeDemoLibrary.jab();
        assertEquals("mkm:mnae/sword_cut", sword.id());
        assertEquals("mkm:mnae/straight_punch", jab.id());
        assertEquals(14, sword.ticks());
        assertEquals(14, jab.ticks());
        for (NativeClip clip : List.of(sword, jab)) {
            assertTrue(clip.events().stream().anyMatch(e ->
                e.id().equals("impact") && Math.abs(e.time() - 5D / 14D) < 1e-5));
            assertFalse(PoseSampler.sample(clip, .35).isEmpty());
            assertEquals(PoseSampler.sample(clip, .35), PoseSampler.sample(clip, .35));
        }
    }

    @Test
    void importingLegacyC3MustPreserveSampledPoseBetweenKeyframes() {
        var library = AnimationLibrary.loadClasspath();
        for (String id : List.of("mkm:sword/light_1", "mkm:unarmed/jab")) {
            AnimationClip original = library.clip(id);
            assertNotNull(original);
            NativeClip imported = LegacyC3Importer.convert(original);
            assertEquals(original.id(), imported.id());
            double maxAngleError = 0D;
            for (int step = 0; step <= 100; step++) {
                double time = step / 100D;
                Map<AnimationClip.Bone, AnimationClip.Rotation> expected = original.sample(time);
                Map<String, Transform> actual = PoseSampler.sample(imported, time);
                for (var e : expected.entrySet()) {
                    var a = e.getValue();
                    Quat expectedRotation = Quat.euler(a.x(), a.y(), a.z());
                    Quat actualRotation = actual.get(e.getKey().name().toLowerCase(
                        java.util.Locale.ROOT)).rotation();
                    double dot = Math.abs(expectedRotation.x() * actualRotation.x()
                        + expectedRotation.y() * actualRotation.y()
                        + expectedRotation.z() * actualRotation.z()
                        + expectedRotation.w() * actualRotation.w());
                    maxAngleError = Math.max(maxAngleError,
                        2D * Math.acos(Math.clamp(dot, -1D, 1D)));
                }
            }
            assertTrue(maxAngleError < .005,
                id + " changed legacy C3 pose by " + maxAngleError
                    + " radians after import");
        }
    }
}
