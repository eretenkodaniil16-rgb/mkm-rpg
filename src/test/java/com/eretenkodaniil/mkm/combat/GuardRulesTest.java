package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class GuardRulesTest {
    @Test
    void successfulBlockDebitsStaminaAndDelaysRegeneration() {
        var result = GuardRules.block(new CombatState(), 100, 12F,
                1.0D, 1.0D, 100F, 10F, 30L);
        assertFalse(result.broken());
        assertEquals(10F, result.spent(), 0.0001);
        assertEquals(90F, result.state().stamina(), 0.0001);
        assertEquals(130L, result.state().regenReadyTick());
    }

    @Test
    void shieldStabilityLowersCostAndDoesNotReverseIt() {
        var normal = GuardRules.block(new CombatState(), 100, 12F,
                1D, 1D, 100F, 10F, 30L);
        var sturdy = GuardRules.block(new CombatState(), 100, 12F,
                1.25D, 1D, 100F, 10F, 30L);
        assertTrue(sturdy.spent() < normal.spent());
        assertEquals(8F, sturdy.spent(), 0.0001);
    }

    @Test
    void guardBreakCannotSpendNegativeStamina() {
        var poor = new CombatState(2F, 100, 0, 0, 0);
        var result = GuardRules.block(poor, 102, 40F,
                1D, 1D, 100F, 10F, 30L);
        assertTrue(result.broken());
        assertEquals(0F, result.state().stamina(), 0.0001);
        assertEquals(2F, result.spent(), 0.0001);
    }

    @Test
    void repeatedBlocksDoNotGetFreeRegenerationDuringDelay() {
        var first = GuardRules.block(new CombatState(), 100, 0F,
                1D, 1D, 100F, 10F, 30L);
        var second = GuardRules.block(first.state(), 101, 0F,
                1D, 1D, 100F, 10F, 30L);
        assertEquals(92F, second.state().stamina(), 0.0001);
        assertEquals(131L, second.state().regenReadyTick());
    }

    @Test
    void invalidInputsDoNotCorruptState() {
        assertThrows(IllegalArgumentException.class, () ->
                GuardRules.block(new CombatState(), 10, Float.NaN, 1D, 1D, 100F, 10F, 30L));
        assertThrows(IllegalArgumentException.class, () ->
                GuardRules.block(new CombatState(), 10, 5F, 0D, 1D, 100F, 10F, 30L));
        assertThrows(IllegalArgumentException.class, () ->
                GuardRules.block(new CombatState(), 10, 5F, 1D, Double.POSITIVE_INFINITY, 100F, 10F, 30L));
    }
}
