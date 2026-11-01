package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class GuardRulesSafetyTest {
    @Test
    void zeroDamageBlockRetainsBaseCost() {
        var result = GuardRules.block(new CombatState(), 100L, 0F, 1D, 1D, 100F, 10F, 30L);
        assertFalse(result.broken());
        assertEquals(4F, result.spent(), 1E-4F);
    }

    @Test
    void rejectsInvalidResourceArguments() {
        assertThrows(IllegalArgumentException.class, () -> GuardRules.block(new CombatState(),
                100L, 2F, 1D, 1D, Float.NaN, 10F, 30L));
        assertThrows(IllegalArgumentException.class, () -> GuardRules.block(new CombatState(),
                100L, 2F, 1D, 1D, 100F, Float.POSITIVE_INFINITY, 30L));
        assertThrows(IllegalArgumentException.class, () -> GuardRules.block(new CombatState(),
                100L, 2F, 1D, 1D, 100F, -1F, 30L));
        assertThrows(IllegalArgumentException.class, () -> GuardRules.block(new CombatState(),
                100L, 2F, 1D, 1D, 100F, 10F, -1L));
    }

    @Test
    void extremeFiniteBlockedDamageIsCapped() {
        var result = GuardRules.block(new CombatState(), 100L, Float.MAX_VALUE,
                1D, 1D, 100F, 10F, 30L);
        assertEquals(GuardRules.BLOCK_MAX_COST, result.spent(), 1E-4F);
        assertTrue(Float.isFinite(result.state().stamina()));
    }
}
