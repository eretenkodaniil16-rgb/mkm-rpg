package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class DirectionalRollChoreographyTest {
    @Test
    void sevenNonForwardDirectionsHaveGroundedSmoothTurns() {
        for (byte d = 1; d <= DodgeDirection.LAST; d++) {
            double prev = -1D;
            for (int i = 0; i <= 200; i++) {
                float t = i / 200F;
                DirectionalRollChoreography.Pose pose = DirectionalRollChoreography.sample(d, t);
                assertTrue(Float.isFinite(pose.radians()));
                assertTrue(Float.isFinite(pose.visualYOffset()));
                assertEquals(1D, Math.hypot(pose.axisX(), pose.axisZ()), 1e-6);
                assertEquals(0D, DirectionalRollChoreography.silhouetteMinHeight(pose),
                        2e-6, "ground projection d=" + d + ", t=" + t);
                assertTrue(pose.radians() + 1e-5D >= prev, "reverse angular motion d=" + d);
                if (i > 0) assertTrue(pose.radians() - prev < .08D);
                prev = pose.radians();
            }
            assertEquals(0F, DirectionalRollChoreography.sample(d, 0F).radians(), 1e-6);
            assertEquals(0F, DirectionalRollChoreography.sample(d, 0F).visualYOffset(), 1e-6);
            assertEquals(2F * (float) Math.PI,
                    DirectionalRollChoreography.sample(d, 1F).radians(), 1e-6);
            assertEquals(0F, DirectionalRollChoreography.sample(d, 1F).visualYOffset(), 1e-6);
        }
    }

    @Test
    void allAxesTumbleTowardTheirTravelDirection() {
        for (byte d = 0; d <= DodgeDirection.LAST; d++) {
            if (d == DodgeDirection.FORWARD) continue;
            var pose = DirectionalRollChoreography.sample(d, .3F);
            float x = CombatAnimationMath.rollAxisX(d);
            float z = CombatAnimationMath.rollAxisZ(d);
            assertEquals(-x / Math.hypot(x, z), pose.axisX(), 1e-6);
            assertEquals(z / Math.hypot(x, z), pose.axisZ(), 1e-6);
        }
    }

    @Test
    void invalidProgressNeverProducesNonFiniteMatrix() {
        float[] invalid = {-10F, Float.NaN, Float.POSITIVE_INFINITY, 10F};
        for (float t : invalid) {
            var pose = DirectionalRollChoreography.sample(DodgeDirection.RIGHT, t);
            assertTrue(Float.isFinite(pose.radians()));
            assertTrue(Float.isFinite(pose.visualYOffset()));
            assertEquals(0D, DirectionalRollChoreography.silhouetteMinHeight(pose), 2e-6);
        }
    }

    @Test
    void serverDodgeImpulseRemainsIndependent() {
        DodgeMotion motion = DodgeMotion.fromFacing(0D, 1D, DodgeDirection.RIGHT, 1D);
        assertNotNull(motion);
        assertEquals(DodgeMotion.BASE_IMPULSE,
                Math.hypot(motion.x(), motion.z()), 1e-8);
    }
}
