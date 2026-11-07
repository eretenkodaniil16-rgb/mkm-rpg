package com.eretenkodaniil.mkm.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Deterministic planner-only workload. Not an integrated Minecraft MSPT test.
 * Report timings in JUnit XML without flaky wall-clock performance assertions.
 */
class EnemyTacticsPerformanceTest {
    private static volatile int checksum;

    @Test void plannerWorkloadForTwentyAndFortyControllers() {
        for (int n : new int[] {20, 40}) {
            int decisions = 0;
            long start = System.nanoTime();
            for (int tick = 0; tick < 20_000; tick++) {
                for (int entityId = 0; entityId < n; entityId++) {
                    if (!EnemyTactics.shouldRepath(tick, entityId, 5)) continue;
                    EnemyTactics.Decision decision = EnemyTactics.decide(
                            (entityId & 1) == 0 ? EnemyDefinition.Host.HUSK
                                    : EnemyDefinition.Host.SPIDER,
                            true, tick % 50 < 35, 0.9,
                            tick % 150, 64.0, tick % 100 == 0 ? 50 : 0);
                    checksum ^= decision.ordinal();
                    decisions++;
                }
            }
            long elapsed = System.nanoTime() - start;
            assertEquals(20_000 * (n / 5), decisions);
            System.out.printf(java.util.Locale.ROOT,
                    "Enemy planner-only synthetic cohort n=%d, decisions=%d, ns/decision=%.2f (no Minecraft world/pathfinding)%n",
                    n, decisions, (double) elapsed / decisions);
        }
    }
}
