package com.eretenkodaniil.mkm.enemy;

/**
 * Pure policy. Routing/pathfinding remains vanilla's responsibility.
 * No allocations, damage or random selection in the hot decision path.
 */
public final class EnemyTactics {
    public static final long HUMANOID_MEMORY_TICKS = 120L;
    public static final long SPIDER_MEMORY_TICKS = 65L;

    public enum Decision { PATROL, CHASE, INVESTIGATE, RETREAT, FLANK, ATTACK, RECOVER_PATH }

    private EnemyTactics() {}

    public static Decision decide(EnemyDefinition.Host host,
            boolean hasTargetOrMemory, boolean lineOfSight,
            double remainingHealthFraction, long ticksSinceLastSeen,
            double targetDistanceSquared, int stuckTicks) {
        if (stuckTicks >= 50) return Decision.RECOVER_PATH;
        if (host == EnemyDefinition.Host.HUSK) {
            if (hasTargetOrMemory && remainingHealthFraction <= 0.27) return Decision.RETREAT;
            if (lineOfSight && hasTargetOrMemory) return Decision.CHASE;
            if (hasTargetOrMemory && ticksSinceLastSeen < HUMANOID_MEMORY_TICKS)
                return Decision.INVESTIGATE;
            return Decision.PATROL;
        }
        if (lineOfSight && hasTargetOrMemory) {
            return targetDistanceSquared > 16.0 && targetDistanceSquared < 144.0
                    ? Decision.FLANK : Decision.ATTACK;
        }
        if (hasTargetOrMemory && ticksSinceLastSeen < SPIDER_MEMORY_TICKS)
            return Decision.INVESTIGATE;
        return Decision.PATROL;
    }

    public static boolean memoryExpired(EnemyDefinition.Host host, long age) {
        return age >= (host == EnemyDefinition.Host.HUSK
                ? HUMANOID_MEMORY_TICKS : SPIDER_MEMORY_TICKS);
    }

    public static boolean shouldRepath(long now, int entityId, int intervalTicks) {
        return Math.floorMod(now + entityId, intervalTicks) == 0;
    }
}
