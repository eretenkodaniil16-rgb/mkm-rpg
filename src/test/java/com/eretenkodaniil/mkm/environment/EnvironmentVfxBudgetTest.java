package com.eretenkodaniil.mkm.environment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentVfxBudgetTest {
    @Test void ambientScheduleIsPhasedAndBounded() {
        for (int id = 0; id < 40; id++) {
            int count = 0;
            for (int tick = 0; tick < 40; tick++)
                if (EnvironmentVfxBudget.ambientDue(tick, id)) count++;
            assertEquals(1, count);
        }
        for (int tick = 0; tick < 40; tick++) {
            int scheduled = 0;
            for (int id = 0; id < 40; id++)
                if (EnvironmentVfxBudget.ambientDue(tick, id)) scheduled++;
            assertEquals(1, scheduled, "Evenly phased 40-enemy cohort");
        }
    }

    @Test void reducedEffectsSuppressAmbientAndLimitImpact() {
        for (var profile : EnvironmentVisualSettings.Profile.values()) {
            assertEquals(0, EnvironmentVfxBudget.particles(profile, true,
                    EnvironmentVfxBudget.RAIDER_AMBIENT));
            assertEquals(0, EnvironmentVfxBudget.particles(profile, true,
                    EnvironmentVfxBudget.STALKER_AMBIENT));
            assertTrue(EnvironmentVfxBudget.particles(profile, true,
                    EnvironmentVfxBudget.STALKER_HIT) <= 1);
            assertTrue(EnvironmentVfxBudget.particles(profile, false,
                    EnvironmentVfxBudget.STALKER_HIT) <= 7);
        }
        assertEquals(0, EnvironmentVfxBudget.particles(
                EnvironmentVisualSettings.Profile.CINEMATIC, false, (byte) 127));
    }

    @Test void cinemaNeverEnablesCameraFlashOrDamage() {
        assertEquals(3, EnvironmentVfxBudget.particles(
                EnvironmentVisualSettings.Profile.CINEMATIC, false,
                EnvironmentVfxBudget.STALKER_AMBIENT));
        assertEquals(0, EnvironmentVfxBudget.particles(
                EnvironmentVisualSettings.Profile.ECONOMY, false,
                EnvironmentVfxBudget.STALKER_AMBIENT));
    }
}
