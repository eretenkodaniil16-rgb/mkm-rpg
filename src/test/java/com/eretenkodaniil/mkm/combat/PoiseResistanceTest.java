package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class PoiseResistanceTest {
    @Test
    void targetStaggerResistanceActuallyReducesImpact() {
        var ordinary = PoiseRules.onHit(new PoiseState(), 100, 8F, 1.0D, 1.0D, false);
        var sturdy = PoiseRules.onHit(new PoiseState(), 100, 8F, 1.0D, 2.0D, false);
        assertTrue(sturdy.state().buildup() < ordinary.state().buildup());
        assertFalse(sturdy.staggered());
    }

    @Test
    void extremePowerIsBoundedAndInvalidModifiersAreIgnored() {
        var extreme = PoiseRules.onHit(new PoiseState(), 100, 5F, 100.0D, 0.25D, true);
        assertTrue(extreme.staggered());
        var invalid = PoiseRules.onHit(new PoiseState(), 100, 5F, Double.NaN, 1D, true);
        assertEquals(0F, invalid.state().buildup());
        assertFalse(invalid.staggered());
    }

    @Test
    void legacyOverloadRetainsNeutralBehavior() {
        var legacy = PoiseRules.onHit(new PoiseState(), 100, 5F, 1.5D, false);
        var current = PoiseRules.onHit(new PoiseState(), 100, 5F, 1.5D, 1.0D, false);
        assertEquals(legacy.state(), current.state());
    }
}
