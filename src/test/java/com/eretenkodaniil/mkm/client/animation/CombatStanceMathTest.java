package com.eretenkodaniil.mkm.client.animation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CombatStanceMathTest {
    @Test
    void idleStanceVisibleButWalkingAndJumpingUseVanillaGait() {
        assertEquals(1F, CombatStanceMath.attackLegWeight(true, 0D), 1e-6);
        assertEquals(1F, CombatStanceMath.attackLegWeight(true, .025D * .025D), 1e-6);
        assertEquals(0F, CombatStanceMath.attackLegWeight(true, .14D * .14D), 1e-6);
        assertEquals(0F, CombatStanceMath.attackLegWeight(false, 0D), 1e-6);
        assertEquals(0F, CombatStanceMath.attackLegWeight(true, Double.NaN), 1e-6);
        assertEquals(0F, CombatStanceMath.attackLegWeight(true, Double.POSITIVE_INFINITY), 1e-6);
        assertEquals(0F, CombatStanceMath.attackLegWeight(true, -1D), 1e-6);
    }

    @Test
    void smoothMonotoneBlendNeverRaisesLegWeightAtHigherSpeed() {
        float prior = Float.POSITIVE_INFINITY;
        for (int i = 0; i <= 200; i++) {
            double speed = i * .001D;
            float weight = CombatStanceMath.attackLegWeight(true, speed * speed);
            assertTrue(weight >= 0F && weight <= 1F);
            assertTrue(weight <= prior + 1e-6F);
            prior = weight;
        }
    }
}
