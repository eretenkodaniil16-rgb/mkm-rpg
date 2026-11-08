package com.eretenkodaniil.mkm.encounter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class EncounterAutoRulesTest {
    @Test void questGatedNoGmSpawnButRequiresLoadedWardenAndSafeNight() {
        assertTrue(EncounterAutoRules.canBegin(true, true, false,
                true, false, true, 40 * 40));
        assertFalse(EncounterAutoRules.canBegin(false, true, false,
                true, false, true, 40 * 40));
        assertFalse(EncounterAutoRules.canBegin(true, false, false,
                true, false, true, 40 * 40));
        assertFalse(EncounterAutoRules.canBegin(true, true, true,
                true, false, true, 40 * 40));
        assertFalse(EncounterAutoRules.canBegin(true, true, false,
                false, false, true, 40 * 40));
        assertFalse(EncounterAutoRules.canBegin(true, true, false,
                true, true, true, 40 * 40));
        assertFalse(EncounterAutoRules.canBegin(true, true, false,
                true, false, false, 40 * 40));
        assertFalse(EncounterAutoRules.canBegin(true, true, false,
                true, false, true, 23 * 23));
        assertFalse(EncounterAutoRules.canBegin(true, true, false,
                true, false, true, 97 * 97));
        assertFalse(EncounterAutoRules.canBegin(true, true, false,
                true, false, true, Double.NaN));
    }

    @Test void boundedCharacterLevelNeverScalesPastThirty() {
        assertEquals(1, EncounterAutoRules.encounterLevel(-50));
        assertEquals(1, EncounterAutoRules.encounterLevel(1));
        assertEquals(17, EncounterAutoRules.encounterLevel(17));
        assertEquals(30, EncounterAutoRules.encounterLevel(999));
    }
}
