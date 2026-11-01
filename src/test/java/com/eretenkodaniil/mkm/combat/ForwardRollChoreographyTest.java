package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ForwardRollChoreographyTest {
    @Test
    void forwardRotationIsNotABackflip() {
        ForwardRollChoreography.Pose halfway = ForwardRollChoreography.sample(0.41F);
        assertEquals(-1F, halfway.axisX(), 0.00001);
        assertEquals(0F, halfway.axisZ(), 0.00001);
        // Local avatar forward is -Z; rotating vertical +Y by -X moves
        // its head towards -Z, i.e. in the server movement direction.
        double nearStart = ForwardRollChoreography.sample(0.14F).radians();
        assertTrue(Math.sin(nearStart) > 0D);
        double transformedUpZ = -Math.sin(nearStart);
        assertTrue(transformedUpZ < 0D, "Avatar must tumble forward, not backflip");
    }

    @Test
    void allFramesPreserveProjectedGroundContactAndReturnToStanding() {
        double prev = -1D;
        for (int i = 0; i <= 200; i++) {
            float time = i / 200F;
            ForwardRollChoreography.Pose pose = ForwardRollChoreography.sample(time);
            assertTrue(Float.isFinite(pose.radians()));
            assertTrue(Float.isFinite(pose.visualYOffset()));
            assertTrue(pose.radians() + 1e-5 >= prev, "Forward rotation must not reverse");
            assertEquals(0D, ForwardRollChoreography.silhouetteMinHeight(pose), 2e-6,
                    "Silhouette should contact, but not pass below, reference ground at " + time);
            if (i > 0) {
                assertTrue(pose.radians() - prev < 0.08D,
                        "Abrupt rotation between 5ms-normalized samples");
            }
            prev = pose.radians();
        }
        ForwardRollChoreography.Pose beginning = ForwardRollChoreography.sample(0);
        ForwardRollChoreography.Pose finished = ForwardRollChoreography.sample(1);
        assertEquals(0F, beginning.radians(), 1e-6);
        assertEquals(0F, beginning.visualYOffset(), 1e-6);
        assertEquals(2F * (float) Math.PI, finished.radians(), 1e-6);
        assertEquals(0F, finished.visualYOffset(), 1e-6);
        assertEquals(2F * (float) Math.PI,
                ForwardRollChoreography.sample(0.76F).radians(), 1e-5);
    }

    @Test
    void visualCorrectionDoesNotTouchServerMovementRules() {
        // The independent velocity calculation remains exactly C1.3.
        DodgeMotion forward = DodgeMotion.fromFacing(0D, 1D, DodgeDirection.FORWARD, 1D);
        assertNotNull(forward);
        assertEquals(DodgeMotion.BASE_IMPULSE, forward.z(), 1e-8);
        assertEquals(0D, forward.x(), 1e-8);
    }

    @Test
    void invalidInputDoesNotReachNonFiniteAnimationMatrices() {
        for (float p : new float[]{-100F, 2F, Float.NaN, Float.POSITIVE_INFINITY}) {
            ForwardRollChoreography.Pose pose = ForwardRollChoreography.sample(p);
            assertTrue(Float.isFinite(pose.radians()));
            assertTrue(Float.isFinite(pose.visualYOffset()));
            assertEquals(0D, ForwardRollChoreography.silhouetteMinHeight(pose), 2e-6);
        }
    }
}
