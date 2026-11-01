package com.eretenkodaniil.mkm.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CombatAnimationMathTest {
    @Test
    void phaseProgressIsBoundedAndHitsKnownEndpoints() {
        ActionTimeline combo = ActionTimelineRules.beginCombo(new ActionTimeline(), 100);
        assertNotNull(combo);
        assertEquals(0F, CombatAnimationMath.phaseProgress(combo, 100, 0), 0.001);
        assertEquals(0.5F, CombatAnimationMath.phaseProgress(combo, 102, 0.5F), 0.001);
        assertEquals(1F, CombatAnimationMath.phaseProgress(combo, 105, 0), 0.001);
        assertEquals(0F, CombatAnimationMath.phaseProgress(combo, 95, 0), 0.001);
        assertEquals(1F, CombatAnimationMath.phaseProgress(combo, 900, 0), 0.001);
        assertEquals(0F, CombatAnimationMath.phaseProgress(new ActionTimeline(), 120, 0), 0.001);
    }

    @Test
    void animationDoesNotChangeAuthoritativeHitTime() {
        ActionTimeline windup = ActionTimelineRules.beginCombo(new ActionTimeline(), 10);
        assertEquals(ActionTimeline.Phase.WINDUP, windup.phase());
        assertEquals(0.8F, CombatAnimationMath.poseWeight(
                ActionTimeline.Phase.WINDUP, 1.0F), 0.001);
        assertEquals(1F, CombatAnimationMath.poseWeight(
                ActionTimeline.Phase.ACTIVE, 0F), 0.001);
        assertEquals(0F, CombatAnimationMath.poseWeight(
                ActionTimeline.Phase.RECOVERY, 1F), 0.001);
        assertEquals(ActionTimeline.Phase.ACTIVE,
                ActionTimelineRules.advance(windup, 15).phase());
    }

    @Test
    void dodgeDirectionsAndRollAnglesAreNormalized() {
        for (byte direction = 0; direction <= DodgeDirection.LAST; direction++) {
            float x = CombatAnimationMath.rollAxisX(direction);
            float z = CombatAnimationMath.rollAxisZ(direction);
            assertTrue(x != 0 || z != 0);
            float magnitude = (float) Math.hypot(x, z);
            assertTrue(magnitude >= 1.0F && magnitude <= 1.415F);
            ActionTimeline dodge = ActionTimelineRules.beginDodge(new ActionTimeline(), 20, direction);
            assertNotNull(dodge);
            assertEquals(direction, dodge.dodgeDirection());
            assertEquals(0F, CombatAnimationMath.phaseProgress(dodge, 20, 0), 0.001);
            assertEquals(1F, CombatAnimationMath.phaseProgress(dodge, 30, 0), 0.001);
        }
        assertNull(ActionTimelineRules.beginDodge(new ActionTimeline(), 20, (byte) 8));
        assertEquals(0F, CombatAnimationMath.rollRadians(0), 0.001);
        assertEquals((float)(2 * Math.PI), CombatAnimationMath.rollRadians(1), 0.001);
        assertEquals((float)Math.PI, CombatAnimationMath.rollRadians(0.5F), 0.001);
    }
}
