package com.eretenkodaniil.mkm.combat.mode;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class CombatModeRulesTest {
    private static final String SWORD = "minecraft:iron_sword";

    @Test void offByDefaultAndOneReleaseProducesOneScheduledNativeHit() {
        var state = new CombatModeRules.State();
        assertSame(state, CombatModeRules.press(state, 1, 100, SWORD, true));
        state = CombatModeRules.mode(state, true);
        var pressed = CombatModeRules.press(state, 1, 100, SWORD, true);
        assertEquals(CombatModeRules.Phase.HELD, pressed.phase());
        assertSame(pressed, CombatModeRules.press(pressed, 2, 101, SWORD, true));
        var quick = CombatModeRules.release(pressed, 1, 105, true);
        assertEquals(CombatModeRules.Strike.QUICK, quick.strike());
        assertEquals(109, quick.impactAt());
        assertEquals(118, quick.cooldownUntil());
        assertSame(quick, CombatModeRules.release(quick, 1, 106, true));
        assertFalse(CombatModeRules.readyToImpact(quick, 108, SWORD, true));
        assertTrue(CombatModeRules.readyToImpact(quick, 109, SWORD, true));
        assertFalse(CombatModeRules.readyToImpact(quick, 109, "minecraft:iron_axe", true));
        var consumed = CombatModeRules.cancel(quick);
        assertEquals(CombatModeRules.Phase.IDLE, consumed.phase());
        assertFalse(CombatModeRules.readyToImpact(consumed, 110, SWORD, true));
        assertSame(consumed, CombatModeRules.press(consumed, 2, 117, SWORD, true));
        assertEquals(CombatModeRules.Phase.HELD,
                CombatModeRules.press(consumed, 2, 118, SWORD, true).phase());
    }

    @Test void chargedIsAuthorizedByServerHoldNotClientTimestamp() {
        var start = CombatModeRules.press(
                CombatModeRules.mode(new CombatModeRules.State(), true),
                7, 300, SWORD, true);
        var tooEarly = CombatModeRules.release(start, 7, 311, true);
        assertEquals(CombatModeRules.Strike.QUICK, tooEarly.strike());
        var fresh = CombatModeRules.press(
                CombatModeRules.mode(new CombatModeRules.State(), true),
                8, 300, SWORD, true);
        var charged = CombatModeRules.release(fresh, 8, 312, true);
        assertEquals(CombatModeRules.Strike.CHARGED, charged.strike());
        assertEquals(319, charged.impactAt());
        assertEquals(336, charged.cooldownUntil());
        assertFalse(CombatModeRules.readyToImpact(charged, 318, SWORD, true));
        assertTrue(CombatModeRules.readyToImpact(charged, 319, SWORD, true));
        assertTrue(CombatModeRules.release(fresh, 8, 336, true).phase()
                == CombatModeRules.Phase.IDLE, "Oversized hold expires, no hit");
    }

    @Test void packetLossReorderAndSpamCannotScheduleMoreHits() {
        var idle = CombatModeRules.mode(new CombatModeRules.State(), true);
        assertSame(idle, CombatModeRules.release(idle, 1, 100, true));
        var active = CombatModeRules.press(idle, 100, 1000, SWORD, true);
        assertSame(active, CombatModeRules.release(active, 101, 1012, true));
        assertSame(active, CombatModeRules.press(active, 101, 1001, SWORD, true));
        assertSame(active, CombatModeRules.press(active, 100, 1001, SWORD, true));
        assertSame(active, CombatModeRules.release(active, 100, 1005, false));
        var good = CombatModeRules.release(active, 100, 1012, true);
        assertEquals(CombatModeRules.Phase.QUEUED, good.phase());
        assertSame(good, CombatModeRules.release(good, 100, 1013, true));
        var done = CombatModeRules.cancel(good);
        assertSame(done, CombatModeRules.press(done, 100, 2000, SWORD, true));
        assertSame(done, CombatModeRules.press(done, 99, 2000, SWORD, true));
        assertSame(done, CombatModeRules.press(done, 101, 1023, SWORD, true));
        assertEquals(CombatModeRules.Phase.HELD,
                CombatModeRules.press(done, 101, 1024, SWORD, true).phase());
    }

    @Test void guiCancellationAndExpiryFailClosed() {
        var enabled = CombatModeRules.mode(new CombatModeRules.State(), true);
        var held = CombatModeRules.press(enabled, 1, 100, SWORD, true);
        assertSame(held, CombatModeRules.expire(held, 135));
        assertEquals(CombatModeRules.Phase.IDLE, CombatModeRules.expire(held, 136).phase());
        assertEquals(CombatModeRules.Phase.IDLE, CombatModeRules.cancel(held).phase());
        assertFalse(CombatModeRules.readyToImpact(
                CombatModeRules.release(held, 1, 120, true), 140, SWORD, false));
        assertEquals(CombatModeRules.Phase.IDLE,
                CombatModeRules.mode(CombatModeRules.release(held, 1, 120, true), false).phase());
    }

    @Test void invalidSequencingAndCooldownDontBypassServerPolicy() {
        var state = CombatModeRules.mode(new CombatModeRules.State(), true);
        assertSame(state, CombatModeRules.press(state, -1, 100, SWORD, true));
        assertSame(state, CombatModeRules.press(state, 1, -1, SWORD, true));
        var held = CombatModeRules.press(state, 10, 100, SWORD, true);
        assertSame(held, CombatModeRules.release(held, 10, 99, true));
        assertSame(held, CombatModeRules.release(held, 10, 101, false));
        var next = CombatModeRules.cancel(CombatModeRules.release(held, 10, 105, true));
        assertSame(next, CombatModeRules.press(next, 11, 106, SWORD, true));
    }
}
