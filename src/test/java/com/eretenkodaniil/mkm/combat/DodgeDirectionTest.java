package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class DodgeDirectionTest {
    @Test
    void mapsCardinalInputsWithoutDefaultingToForward() {
        assertEquals(DodgeDirection.FORWARD, DodgeDirection.fromMovement(true, false, false, false));
        assertEquals(DodgeDirection.BACKWARD, DodgeDirection.fromMovement(false, true, false, false));
        assertEquals(DodgeDirection.LEFT, DodgeDirection.fromMovement(false, false, true, false));
        assertEquals(DodgeDirection.RIGHT, DodgeDirection.fromMovement(false, false, false, true));
        assertEquals(DodgeDirection.FORWARD, DodgeDirection.fromMovement(false, false, false, false));
    }

    @Test
    void mapsDiagonals() {
        assertEquals(DodgeDirection.FORWARD_LEFT, DodgeDirection.fromMovement(true, false, true, false));
        assertEquals(DodgeDirection.FORWARD_RIGHT, DodgeDirection.fromMovement(true, false, false, true));
        assertEquals(DodgeDirection.BACKWARD_LEFT, DodgeDirection.fromMovement(false, true, true, false));
        assertEquals(DodgeDirection.BACKWARD_RIGHT, DodgeDirection.fromMovement(false, true, false, true));
    }

    @Test
    void opposingKeysCancelConsistently() {
        assertEquals(DodgeDirection.FORWARD, DodgeDirection.fromMovement(true, true, false, false));
        assertEquals(DodgeDirection.LEFT, DodgeDirection.fromMovement(true, true, true, false));
        assertEquals(DodgeDirection.BACKWARD, DodgeDirection.fromMovement(false, true, true, true));
        assertEquals(DodgeDirection.FORWARD, DodgeDirection.fromMovement(true, false, true, true));
    }
}
