package com.eretenkodaniil.mkm.combat.mode;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class CombatPacketBudgetTest {
    @Test void floodInOneTickIsBoundedAndRecoversNextTick() {
        var state = new CombatPacketBudget.State();
        for (int i=0; i<CombatPacketBudget.MAX_MESSAGES_PER_TICK; i++) {
            var next = CombatPacketBudget.accept(state, 100);
            assertTrue(CombatPacketBudget.accepted(state, next));
            state = next;
        }
        assertEquals(CombatPacketBudget.MAX_MESSAGES_PER_TICK, state.received());
        for (int i=0;i<500;i++) {
            var refused = CombatPacketBudget.accept(state, 100);
            assertSame(state, refused);
            assertFalse(CombatPacketBudget.accepted(state, refused));
        }
        var newer = CombatPacketBudget.accept(state, 101);
        assertEquals(1, newer.received());
        assertTrue(CombatPacketBudget.accepted(state, newer));
        assertSame(newer, CombatPacketBudget.accept(newer, 99));
    }
}
