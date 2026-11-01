package com.eretenkodaniil.mkm.abilities;

/**
 * Pure server-target verification predicate for future mkm:arcane_bolt.
 * Facts MUST come from one authoritative server raycast with block occlusion,
 * entity visibility, server-side PVP checks and current world state.
 * No damage or mana is applied by this class.
 */
public final class ArcaneBoltTargetRules {
    public static final double BASE_RANGE_BLOCKS = 12.0D;
    private static final double EPSILON = 1.0E-8D;

    public record TargetFacts(
            boolean casterAlive,
            boolean targetAlive,
            boolean targetIsCaster,
            boolean sameDimension,
            boolean attackAllowed,
            boolean blockRayClear,
            boolean firstHitEntity,
            double distanceSquared) {}

    private ArcaneBoltTargetRules() {}

    public static boolean eligible(TargetFacts facts, double rangeMultiplier) {
        if (facts == null || !Double.isFinite(rangeMultiplier)
                || rangeMultiplier <= 0.0D || !Double.isFinite(facts.distanceSquared())
                || facts.distanceSquared() < 0.0D) return false;
        double reach = BASE_RANGE_BLOCKS * rangeMultiplier;
        if (!Double.isFinite(reach) || reach <= 0.0D) return false;
        return facts.casterAlive() && facts.targetAlive() && !facts.targetIsCaster()
                && facts.sameDimension() && facts.attackAllowed()
                && facts.blockRayClear() && facts.firstHitEntity()
                && facts.distanceSquared() > EPSILON
                && facts.distanceSquared() <= reach * reach;
    }
}
