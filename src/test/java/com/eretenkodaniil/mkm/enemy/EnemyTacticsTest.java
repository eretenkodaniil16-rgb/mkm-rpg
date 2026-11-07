package com.eretenkodaniil.mkm.enemy;

import static com.eretenkodaniil.mkm.enemy.EnemyDefinition.Host.*;
import static com.eretenkodaniil.mkm.enemy.EnemyTactics.Decision.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EnemyTacticsTest {
    @Test void raiderPatrolChaseMemoryRetreatAndTimeout() {
        assertEquals(PATROL, EnemyTactics.decide(HUSK, false, false, 1, Long.MAX_VALUE, 999, 0));
        assertEquals(CHASE, EnemyTactics.decide(HUSK, true, true, 1, 0, 25, 0));
        assertEquals(INVESTIGATE, EnemyTactics.decide(HUSK, true, false, 1, 30, 25, 0));
        assertEquals(RETREAT, EnemyTactics.decide(HUSK, true, true, 0.20, 0, 9, 0));
        assertEquals(PATROL, EnemyTactics.decide(HUSK, true, false, 1, 120, 25, 0));
        assertTrue(EnemyTactics.memoryExpired(HUSK, 120));
        assertFalse(EnemyTactics.memoryExpired(HUSK, 119));
    }

    @Test void spiderHasDistinctFlankAndShortMemory() {
        assertEquals(FLANK, EnemyTactics.decide(SPIDER, true, true, 1, 0, 36, 0));
        assertEquals(ATTACK, EnemyTactics.decide(SPIDER, true, true, 1, 0, 4, 0));
        assertEquals(INVESTIGATE, EnemyTactics.decide(SPIDER, true, false, 1, 64, 36, 0));
        assertEquals(PATROL, EnemyTactics.decide(SPIDER, true, false, 1, 65, 36, 0));
        assertEquals(RECOVER_PATH, EnemyTactics.decide(SPIDER, true, true, 1, 0, 36, 50));
        assertTrue(EnemyTactics.memoryExpired(SPIDER, 65));
    }

    @Test void loadDistributionFortyControllersHasNoPerTickGlobalWork() {
        for (long tick = 0; tick < 200; tick++) {
            int decisions = 0;
            for (int entityId = 1; entityId <= 40; entityId++) {
                if (EnemyTactics.shouldRepath(tick, entityId, 5)) {
                    decisions++;
                    EnemyTactics.decide(entityId % 2 == 0 ? HUSK : SPIDER,
                            true, tick % 10 < 5, 0.8, tick % 150, 36.0, 0);
                }
            }
            assertEquals(8, decisions, "5-tick phased schedule per 40 entities");
        }
    }
}
