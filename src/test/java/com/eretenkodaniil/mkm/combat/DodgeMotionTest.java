package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class DodgeMotionTest {
    @Test
    void cardinalDirectionsAreRelativeToPlayersFacing() {
        DodgeMotion forward = DodgeMotion.fromFacing(0, 1, 0, 1);
        DodgeMotion backward = DodgeMotion.fromFacing(0, 1, 1, 1);
        DodgeMotion left = DodgeMotion.fromFacing(0, 1, 2, 1);
        DodgeMotion right = DodgeMotion.fromFacing(0, 1, 3, 1);

        assertEquals(DodgeMotion.BASE_IMPULSE, forward.z(), 1e-8);
        assertEquals(-DodgeMotion.BASE_IMPULSE, backward.z(), 1e-8);
        assertEquals(DodgeMotion.BASE_IMPULSE, left.x(), 1e-8);
        assertEquals(-DodgeMotion.BASE_IMPULSE, right.x(), 1e-8);
        assertEquals(0.0, forward.x(), 1e-8);
    }

    @Test
    void arbitraryFacingIsNormalized() {
        DodgeMotion forward = DodgeMotion.fromFacing(100, 100, 0, 1);
        assertNotNull(forward);
        assertEquals(DodgeMotion.BASE_IMPULSE, Math.hypot(forward.x(), forward.z()), 1e-8);
        DodgeMotion right = DodgeMotion.fromFacing(100, 100, 3, 1);
        assertEquals(0D, forward.x() * right.x() + forward.z() * right.z(), 1e-8);
    }

    @Test
    void diagonalImpulseIsNormalizedAndCorrectlyOriented() {
        DodgeMotion forwardLeft = DodgeMotion.fromFacing(0, 1, 4, 1);
        DodgeMotion forwardRight = DodgeMotion.fromFacing(0, 1, 5, 1);
        DodgeMotion backwardLeft = DodgeMotion.fromFacing(0, 1, 6, 1);
        DodgeMotion backwardRight = DodgeMotion.fromFacing(0, 1, 7, 1);
        for (DodgeMotion motion : new DodgeMotion[]{
                forwardLeft, forwardRight, backwardLeft, backwardRight}) {
            assertNotNull(motion);
            assertEquals(DodgeMotion.BASE_IMPULSE,
                    Math.hypot(motion.x(), motion.z()), 1e-8);
        }
        assertTrue(forwardLeft.x() > 0 && forwardLeft.z() > 0);
        assertTrue(forwardRight.x() < 0 && forwardRight.z() > 0);
        assertTrue(backwardLeft.x() > 0 && backwardLeft.z() < 0);
        assertTrue(backwardRight.x() < 0 && backwardRight.z() < 0);
    }

    @Test
    void attributeScalingIsBounded() {
        DodgeMotion boosted = DodgeMotion.fromFacing(1, 0, 0, 10000);
        assertEquals(DodgeMotion.MAX_IMPULSE, Math.hypot(boosted.x(), boosted.z()), 1e-8);
        DodgeMotion weakened = DodgeMotion.fromFacing(1, 0, 0, 0.5);
        assertEquals(DodgeMotion.BASE_IMPULSE * 0.5, Math.hypot(weakened.x(), weakened.z()), 1e-8);
    }

    @Test
    void invalidInputsDoNotProduceMovementOrConsumeResources() {
        assertNull(DodgeMotion.fromFacing(0, 1, 8, 1));
        assertNull(DodgeMotion.fromFacing(0, 1, -1, 1));
        assertNull(DodgeMotion.fromFacing(0, 0, 0, 1));
        assertNull(DodgeMotion.fromFacing(Double.NaN, 1, 0, 1));
        assertNull(DodgeMotion.fromFacing(1, 1, 0, Double.NaN));
        assertNull(DodgeMotion.fromFacing(1, 1, 0, -1));
    }
}
