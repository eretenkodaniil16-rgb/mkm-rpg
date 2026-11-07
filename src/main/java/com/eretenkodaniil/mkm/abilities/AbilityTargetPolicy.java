package com.eretenkodaniil.mkm.abilities;

/**
 * Actor-neutral target evidence contract. Minecraft adapters MUST derive all
 * facts from the server world, raycast and Combat permission provider.
 * Unsupported shapes fail closed until a reviewed executor exists.
 */
public final class AbilityTargetPolicy {
    private static final double EPSILON = 1.0E-8D;
    private static final double MAX_RAY_RANGE_BLOCKS = 64.0D;

    public record Evidence(
            boolean actorAlive, boolean targetAlive, boolean self,
            boolean sameDimension, boolean combatPermission,
            boolean unobstructed, boolean firstRayHit, double distanceSquared) {}

    private AbilityTargetPolicy() {}

    public static boolean allowed(AbilityDefinition definition, Evidence proof,
            double rangeBlocks) {
        if (definition == null || proof == null || !proof.actorAlive()
                || !proof.targetAlive() || !proof.sameDimension()) return false;
        return switch (definition.targetMode()) {
            case SELF -> proof.self();
            case ENTITY_RAY -> !proof.self() && proof.combatPermission()
                    && proof.unobstructed() && proof.firstRayHit()
                    && Double.isFinite(rangeBlocks) && rangeBlocks > 0D
                    && rangeBlocks <= MAX_RAY_RANGE_BLOCKS
                    && Double.isFinite(proof.distanceSquared())
                    && proof.distanceSquared() > EPSILON
                    && proof.distanceSquared() <= rangeBlocks * rangeBlocks;
            default -> false; // no arbitrary cone/area/ground/direction targeting
        };
    }
}
