package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class CombatRulesTest {
    @Test
    void heavyCostsResourceAndAppliesRecovery() {
        CombatState fresh = new CombatState();
        CombatState heavy = CombatRules.heavy(fresh, 100, 25F, 30);
        assertNotNull(heavy);
        assertEquals(75F, heavy.stamina());
        assertEquals(130L, heavy.regenReadyTick());
        assertNull(CombatRules.heavy(heavy, 101, 25F, 30));
    }

    @Test
    void dodgeRejectsRepeatedIntentAndPoorStamina() {
        CombatState dodged = CombatRules.dodge(new CombatState(), 100, 20F, 20);
        assertNotNull(dodged);
        assertEquals(80F, dodged.stamina());
        assertNull(CombatRules.dodge(dodged, 101, 20F, 20));
        assertNull(CombatRules.heavy(dodged, 101, 25F, 20));
        assertNull(CombatRules.dodge(new CombatState(5, 0, 0, 0, 0), 100, 20F, 20));
    }

    @Test
    void staminaRegenerationRespectsDelayAndCaps() {
        CombatState spent = CombatRules.heavy(new CombatState(), 100, 25F, 20);
        CombatState waiting = CombatRules.regenerate(spent, 115, 100F, 10F);
        assertEquals(75F, waiting.stamina());
        CombatState recovered = CombatRules.regenerate(waiting, 140, 100F, 10F);
        assertEquals(85F, recovered.stamina(), 0.001);
        CombatState full = CombatRules.regenerate(recovered, 200, 100F, 10F);
        assertEquals(100F, full.stamina());
    }

    @Test
    void damageScalingNeverProducesNegativeDamage() {
        assertEquals(0F, CombatRules.scaleIncomingMelee(-1F, 2, 2, true));
        assertEquals(5F, CombatRules.scaleIncomingMelee(5F, 1, 1, false));
        assertEquals(8.25F, CombatRules.scaleIncomingMelee(5F, 1, 1, true), 0.0001);
    }

    @Test
    void poiseAccumulationDecaysAndThresholdTriggersStagger() {
        PoiseState initial = new PoiseState();
        PoiseRules.Result light = PoiseRules.onHit(initial, 100, 4F, 1, false);
        assertFalse(light.staggered());
        PoiseRules.Result heavy = PoiseRules.onHit(light.state(), 102, 12F, 1.5, true);
        assertTrue(heavy.staggered());
        assertEquals(0F, heavy.state().buildup());
        assertFalse(PoiseRules.onHit(heavy.state(), 105, 12F, 1.0, true).staggered());
    }
}
