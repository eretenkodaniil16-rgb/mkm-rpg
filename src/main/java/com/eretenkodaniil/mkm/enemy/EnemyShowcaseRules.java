package com.eretenkodaniil.mkm.enemy;

/** Testable, allocation-free danger cues: light suppresses lunges, not vanilla movement. */
public final class EnemyShowcaseRules {
    private EnemyShowcaseRules() {}
    public static boolean shadowEnough(int blockLight) { return blockLight < 10; }
    public static boolean shadowEnough(int stalkerLight, int targetLight) {
        return shadowEnough(Math.max(stalkerLight, targetLight));
    }

    public static boolean pouncePossible(boolean visible, int blockLight,
            double distanceSquared, boolean coolingDown) {
        return visible && shadowEnough(blockLight) && !coolingDown
                && Double.isFinite(distanceSquared) && distanceSquared >= 9.0
                && distanceSquared <= 64.0;
    }
}
