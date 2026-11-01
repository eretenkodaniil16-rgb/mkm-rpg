package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class HitFeedbackRulesTest {
    @Test
    void noCosmeticHitWithoutValidServerConfirmation() {
        assertFalse(HitFeedbackRules.validConfirmedHit(-1, 2F));
        assertFalse(HitFeedbackRules.validConfirmedHit(7, 0F));
        assertFalse(HitFeedbackRules.validConfirmedHit(7, -10F));
        assertFalse(HitFeedbackRules.validConfirmedHit(7, Float.NaN));
        assertFalse(HitFeedbackRules.validConfirmedHit(7, Float.POSITIVE_INFINITY));
        assertTrue(HitFeedbackRules.validConfirmedHit(7, 1F));
        assertEquals(0, HitFeedbackRules.particleCount(0F, true));
        assertEquals(0, HitFeedbackRules.particleCount(Float.NaN, false));
    }
    @Test
    void finiteBurstsHaveHardUpperBoundEvenWithExtremelyHighDamage() {
        for (float damage : new float[]{.001F, 1F, 5F, 20F, 10000F, Float.MAX_VALUE}) {
            int normal = HitFeedbackRules.particleCount(damage, false);
            int stagger = HitFeedbackRules.particleCount(damage, true);
            assertTrue(normal >= 4 && normal <= 12);
            assertTrue(stagger >= normal && stagger <= 12);
        }
    }
}
