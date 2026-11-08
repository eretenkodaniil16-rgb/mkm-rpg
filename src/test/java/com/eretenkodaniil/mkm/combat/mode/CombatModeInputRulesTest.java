package com.eretenkodaniil.mkm.combat.mode;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class CombatModeInputRulesTest {
    @Test void vanillaMiningAndScreensNeverIntercepted() {
        for (boolean enabled : new boolean[]{true,false}) {
            assertFalse(CombatModeInputRules.intercept(enabled,false,false,true,true,false,true),
                    "BLOCK and AIR must retain vanilla handling");
            assertFalse(CombatModeInputRules.intercept(enabled,false,true,true,true,true,true),
                    "GUI must retain vanilla click handling");
            assertFalse(CombatModeInputRules.intercept(enabled,false,false,true,false,true,true),
                    "Pickaxe, axe, mace, bow and empty hand must retain vanilla");
        }
        assertFalse(CombatModeInputRules.intercept(false,false,false,true,true,true,true));
        assertFalse(CombatModeInputRules.intercept(true,false,false,false,true,true,true));
        assertFalse(CombatModeInputRules.intercept(true,false,false,true,true,true,false));
        assertTrue(CombatModeInputRules.intercept(true,false,false,true,true,true,true));
        assertTrue(CombatModeInputRules.intercept(true,true,false,true,true,false,true),
                "Already-held entity attack must not turn into late block mining");
    }
    @Test void serverContactIsBoundedByRangeConeAndLineOfSight() {
        assertTrue(CombatTargetRules.valid(2.5D*2.5D, .95D, true));
        assertFalse(CombatTargetRules.valid(4D*4D, 1D, true));
        assertFalse(CombatTargetRules.valid(3D*3D, .5D, true));
        assertFalse(CombatTargetRules.valid(3D*3D, 1D, false));
        assertFalse(CombatTargetRules.valid(Double.NaN, 1D, true));
        assertFalse(CombatTargetRules.valid(1D, Double.NaN, true));
    }
}
