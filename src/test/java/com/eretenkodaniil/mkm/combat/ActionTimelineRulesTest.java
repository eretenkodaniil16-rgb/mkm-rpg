package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ActionTimelineRulesTest {
    @Test
    void phaseTransitionsAreDeterministicAndHitWindowOccursOnce() {
        ActionTimeline windup = ActionTimelineRules.beginCombo(new ActionTimeline(), 100);
        assertEquals(ActionTimeline.Phase.WINDUP, windup.phase());
        assertEquals(1, windup.comboStep());
        assertSame(windup, ActionTimelineRules.advance(windup, 103));
        ActionTimeline active = ActionTimelineRules.advance(windup, 104);
        assertEquals(ActionTimeline.Phase.ACTIVE, active.phase());
        assertSame(active, ActionTimelineRules.advance(active, 104));
        ActionTimeline recovery = ActionTimelineRules.advance(active, 105);
        assertEquals(ActionTimeline.Phase.RECOVERY, recovery.phase());
        assertNull(ActionTimelineRules.beginCombo(recovery, 106));
        ActionTimeline idle = ActionTimelineRules.advance(recovery, 111);
        assertTrue(idle.idle());
        assertEquals(127L, idle.comboUntilTick());
    }

    @Test
    void threeAttackChainResetsAfterExpiryOrCompletion() {
        ActionTimeline state = new ActionTimeline();
        long now = 50;
        for (int step = 1; step <= 3; step++) {
            state = ActionTimelineRules.beginCombo(state, now);
            assertEquals(step, state.comboStep());
            assertNull(ActionTimelineRules.beginCombo(state, now));
            now = state.phaseUntilTick();
            state = ActionTimelineRules.advance(state, now);
            now = state.phaseUntilTick();
            state = ActionTimelineRules.advance(state, now);
            now = state.phaseUntilTick();
            state = ActionTimelineRules.advance(state, now);
            assertTrue(state.idle());
            now++;
        }
        assertEquals(1, ActionTimelineRules.beginCombo(state, now).comboStep());
        assertEquals(1, ActionTimelineRules.beginCombo(state, state.comboUntilTick() + 1).comboStep());
    }

    @Test
    void dodgeBlocksComboUntilRecoveryCompletes() {
        ActionTimeline dodging = ActionTimelineRules.beginDodge(new ActionTimeline(), 30);
        assertEquals(ActionTimeline.Phase.DODGING, dodging.phase());
        assertNull(ActionTimelineRules.beginCombo(dodging, 35));
        assertNull(ActionTimelineRules.beginDodge(dodging, 35));
        assertSame(dodging, ActionTimelineRules.advance(dodging, 37));
        assertTrue(ActionTimelineRules.advance(dodging, 38).idle());
    }

    @Test
    void regenCostRefusesInsufficientStaminaForCombo() {
        CombatState low = new CombatState(2, 0, 0, 0, 0);
        assertNull(CombatRules.combo(low, 25, 8F, 20));
        CombatState good = CombatRules.combo(new CombatState(), 25, 8F, 20);
        assertEquals(92F, good.stamina(), 0.001);
        assertEquals(45L, good.regenReadyTick());
        assertNull(CombatRules.combo(good, 26, Float.NaN, 20));
    }
}
