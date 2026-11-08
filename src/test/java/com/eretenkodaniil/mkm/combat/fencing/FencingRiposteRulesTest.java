package com.eretenkodaniil.mkm.combat.fencing;

import static org.junit.jupiter.api.Assertions.*;
import com.eretenkodaniil.mkm.combat.mode.CombatModeRules;
import org.junit.jupiter.api.Test;

final class FencingRiposteRulesTest {
    @Test void onlyFioreServerShieldBlockArmsWindow() {
        long at = 500L;
        assertEquals(540L, FencingRiposteRules.openedUntil(
                FencingSchool.FIORE, true, true, true, at));
        for (FencingSchool school : new FencingSchool[]{
                FencingSchool.NONE, FencingSchool.LIECHTENAUER, FencingSchool.DESTREZA}) {
            assertEquals(0L, FencingRiposteRules.openedUntil(
                    school, true, true, true, at));
        }
        assertEquals(0L, FencingRiposteRules.openedUntil(
                FencingSchool.FIORE, false, true, true, at));
        assertEquals(0L, FencingRiposteRules.openedUntil(
                FencingSchool.FIORE, true, false, true, at));
        assertEquals(0L, FencingRiposteRules.openedUntil(
                FencingSchool.FIORE, true, true, false, at));
    }

    @Test void onlyConfirmedQuickHealthHitUsesRiposteAndNotAnimationOrCharge() {
        long expiry = FencingRiposteRules.openedUntil(
                FencingSchool.FIORE, true, true, true, 100L);
        assertTrue(FencingRiposteRules.riposte(
                FencingSchool.FIORE, CombatModeRules.Strike.QUICK, true, 140, expiry));
        assertFalse(FencingRiposteRules.riposte(
                FencingSchool.FIORE, CombatModeRules.Strike.QUICK, true, 141, expiry));
        assertFalse(FencingRiposteRules.riposte(
                FencingSchool.FIORE, CombatModeRules.Strike.QUICK, false, 120, expiry));
        assertFalse(FencingRiposteRules.riposte(
                FencingSchool.FIORE, CombatModeRules.Strike.CHARGED, true, 120, expiry));
        assertFalse(FencingRiposteRules.riposte(
                FencingSchool.NONE, CombatModeRules.Strike.QUICK, true, 120, expiry));
        assertFalse(FencingRiposteRules.riposte(
                FencingSchool.FIORE, CombatModeRules.Strike.QUICK, true, 120, 0L));
    }

    @Test void longRunTickOverflowDoesNotForgeOldRiposte() {
        long expiry = FencingRiposteRules.openedUntil(
                FencingSchool.FIORE, true, true, true, Long.MAX_VALUE - 10);
        assertEquals(Long.MAX_VALUE, expiry);
    }
}
