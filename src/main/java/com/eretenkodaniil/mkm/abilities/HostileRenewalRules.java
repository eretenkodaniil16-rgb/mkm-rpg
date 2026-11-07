package com.eretenkodaniil.mkm.abilities;

/** Pure AI activation and line-of-sight policy; self-healing cannot attack a player. */
public final class HostileRenewalRules {
    public static final double MAX_THREAT_DISTANCE_SQUARED = 16D * 16D;

    public record Facts(
            boolean tagged, boolean hostile, boolean actorAlive,
            boolean targetIsPlayer, boolean targetAlive, boolean sameDimension,
            boolean hasLineOfSight, boolean idle, boolean injured,
            double targetDistanceSquared) {}

    private HostileRenewalRules() {}

    public static boolean allowed(Facts facts) {
        return facts != null && facts.tagged() && facts.hostile() && facts.actorAlive()
                && facts.targetIsPlayer() && facts.targetAlive() && facts.sameDimension()
                && facts.hasLineOfSight() && facts.idle() && facts.injured()
                && Double.isFinite(facts.targetDistanceSquared())
                && facts.targetDistanceSquared() > 0
                && facts.targetDistanceSquared() <= MAX_THREAT_DISTANCE_SQUARED;
    }
}
