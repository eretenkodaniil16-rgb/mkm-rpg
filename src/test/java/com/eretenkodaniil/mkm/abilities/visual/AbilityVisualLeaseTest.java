package com.eretenkodaniil.mkm.abilities.visual;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityVisualLeaseTest {
    @Test void oneShotRenewalHasExactlyFourteenCosmeticTicks() {
        assertFalse(AbilityVisualLease.expired(100, 100, 14));
        assertFalse(AbilityVisualLease.expired(100, 113, 14));
        assertTrue(AbilityVisualLease.expired(100, 114, 14));
        assertTrue(AbilityVisualLease.expired(100, 115, 14));
    }

    @Test void worldTimeRollbackCancelsInsteadOfFreezingOldPresentation() {
        assertTrue(AbilityVisualLease.expired(1000, 900, 14));
        assertTrue(AbilityVisualLease.expired(-1, 0, 14));
        assertTrue(AbilityVisualLease.expired(100, 100, 0));
        assertTrue(AbilityVisualLease.expired(100, 100, 101));
        assertTrue(AbilityVisualLease.expired(100, 100, -1));
    }

    @Test void monotoneClockAtLongBoundaryDoesNotOverflow() {
        assertFalse(AbilityVisualLease.expired(Long.MAX_VALUE - 10, Long.MAX_VALUE - 5, 14));
        assertTrue(AbilityVisualLease.expired(Long.MAX_VALUE - 15, Long.MAX_VALUE, 14));
    }
}
