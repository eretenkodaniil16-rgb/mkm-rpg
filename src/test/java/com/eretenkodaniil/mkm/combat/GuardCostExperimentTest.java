package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class GuardCostExperimentTest {
    @Test void defaultStaysOneAndInvalidConfigurationIsSafe() {
        assertEquals(1D, GuardCostExperiment.parse(null));
        assertEquals(1D, GuardCostExperiment.parse(""));
        assertEquals(1D, GuardCostExperiment.parse("1.0"));
        assertEquals(1D, GuardCostExperiment.parse("0"));
        assertEquals(1D, GuardCostExperiment.parse("NaN"));
        assertEquals(1D, GuardCostExperiment.parse("3.0"));
        assertEquals(1.5D, GuardCostExperiment.parse("1.5"));
    }

    @Test void bothVariantsAcrossPvEAndPvPDamageProxies() {
        // The pure formula is source-independent: actual block detection remains vanilla.
        for (float blocked : new float[]{2F, 6F, 10F, 20F}) {
            float base = GuardRules.block(new CombatState(), 100, blocked,
                    1D, 1D, 100F, 10F, 30L, 1D).spent();
            float tuned = GuardRules.block(new CombatState(), 100, blocked,
                    1D, 1D, 100F, 10F, 30L, 1.5D).spent();
            assertEquals(1.5F * base, tuned, .0001F);
            assertTrue(tuned > base);
        }
    }

    @Test void blockStabilityAndLowStaminaStillApply() {
        for (double factor : new double[]{1D, 1.5D}) {
            float normal = GuardRules.block(new CombatState(), 100, 10F,
                    1D, 1D, 100F, 10F, 30L, factor).spent();
            float stable = GuardRules.block(new CombatState(), 100, 10F,
                    1.5D, 1D, 100F, 10F, 30L, factor).spent();
            assertTrue(stable < normal);
            var poor = GuardRules.block(new CombatState(2F, 100, 0, 0, 0), 100, 20F,
                    1D, 1D, 100F, 10F, 30L, factor);
            assertTrue(poor.broken());
            assertEquals(0F, poor.state().stamina(), .0001F);
        }
    }

    @Test void invalidCoefficientCannotCreateFreeOrInfiniteGuard() {
        for (double bad : new double[]{0D, .5D, 1.25D, 2D, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> GuardRules.block(
                    new CombatState(), 100, 10F, 1D, 1D, 100F, 10F, 30L, bad));
        }
    }
}
