package com.eretenkodaniil.mkm.enemy;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class EnemyShowcaseRulesTest {
    @Test void torchSuppressesLungeAndDarknessAllowsIt() {
        assertTrue(EnemyShowcaseRules.pouncePossible(true, 0, 25, false));
        assertTrue(EnemyShowcaseRules.pouncePossible(true, 9, 64, false));
        assertFalse(EnemyShowcaseRules.pouncePossible(true, 10, 25, false));
        assertFalse(EnemyShowcaseRules.pouncePossible(true, 15, 25, false));
        assertFalse(EnemyShowcaseRules.pouncePossible(false, 0, 25, false));
        assertFalse(EnemyShowcaseRules.pouncePossible(true, 0, 25, true));
        assertFalse(EnemyShowcaseRules.pouncePossible(true, 0, 2, false));
        assertFalse(EnemyShowcaseRules.pouncePossible(true, 0, 65, false));
        assertFalse(EnemyShowcaseRules.pouncePossible(true, 0, Double.NaN, false));
    }

    @Test void guardAndEliteTagsDoNotCollideWithStandardIDs() {
        assertNotEquals(EnemyFactory.GUARD_TAG, EnemyFactory.ELITE_STALKER_TAG);
        assertNotEquals(EnemyFactory.GUARD_TAG, EnemyCatalog.ROAD_RAIDER.persistentTag());
        assertNotEquals(EnemyFactory.ELITE_STALKER_TAG, EnemyCatalog.CAVE_STALKER.persistentTag());
    }
}
