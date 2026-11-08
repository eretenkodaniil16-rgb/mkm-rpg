package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class WarriorBraceRulesTest {
    @Test void requiresWarriorShieldGroundTimelineAndCoolDown() {
        assertTrue(WarriorBraceRules.eligible(true,true,true,true,true,200,200));
        assertFalse(WarriorBraceRules.eligible(false,true,true,true,true,200,0));
        assertFalse(WarriorBraceRules.eligible(true,false,true,true,true,200,0));
        assertFalse(WarriorBraceRules.eligible(true,true,false,true,true,200,0));
        assertFalse(WarriorBraceRules.eligible(true,true,true,false,true,200,0));
        assertFalse(WarriorBraceRules.eligible(true,true,true,true,false,200,0));
        assertFalse(WarriorBraceRules.eligible(true,true,true,true,true,199,200));
        assertEquals(60, WarriorBraceRules.duration(false));
        assertEquals(100, WarriorBraceRules.duration(true));
    }
    @Test void existingCombatStaminaSpendIsSoleAuthoritativeDebit() {
        var initial = new CombatState(26.0F, 0, 0, 0, 0);
        var paid = CombatRules.heavy(initial, 300, WarriorBraceRules.BASE_COST, 20);
        assertNotNull(paid);
        assertEquals(2.0F, paid.stamina());
        assertNull(CombatRules.heavy(paid, 300,
                WarriorBraceRules.BASE_COST, 20));
        assertNull(CombatRules.heavy(new CombatState(23F,0,0,0,0),
                300, WarriorBraceRules.BASE_COST, 20));
    }
}
