package com.eretenkodaniil.mkm.abilities;

/**
 * Immutable ability metadata. Numerical modifiers are NOT calculated here:
 * every active effect obtains values from Character Core's PlayerModifierSnapshot.
 */
public record AbilityDefinition(
        String id,
        Kind kind,
        TargetMode targetMode,
        Resource resource,
        Effect effect,
        double baseCost,
        int castTicks,
        int cooldownTicks,
        double baseMagnitude) {

    public enum Kind { ACTIVE, PASSIVE }
    public enum TargetMode { NONE, SELF, ENTITY_RAY, GROUND_POINT, AREA, CONE, DIRECTION }
    public enum Resource { NONE, MANA, STAMINA }
    public enum Effect { HEAL, DAMAGE, BARRIER, CONTROL, MODIFIER }

    public AbilityDefinition {
        if (!AbilityIds.valid(id)) {
            throw new IllegalArgumentException("Ability ID must be a stable mkm: identifier");
        }
        if (kind == null || targetMode == null || resource == null || effect == null) {
            throw new IllegalArgumentException("Missing ability descriptor");
        }
        if (!Double.isFinite(baseCost) || baseCost < 0 || !Double.isFinite(baseMagnitude)
                || baseMagnitude < 0 || castTicks < 0 || cooldownTicks < 0) {
            throw new IllegalArgumentException("Invalid non-negative finite ability values");
        }
        if (kind == Kind.PASSIVE && (resource != Resource.NONE || baseCost != 0
                || castTicks != 0 || cooldownTicks != 0 || targetMode != TargetMode.NONE)) {
            throw new IllegalArgumentException("Passives are not activated or resource-consuming");
        }
        if (kind == Kind.ACTIVE && targetMode == TargetMode.NONE) {
            throw new IllegalArgumentException("Active ability requires target mode");
        }
    }
}
