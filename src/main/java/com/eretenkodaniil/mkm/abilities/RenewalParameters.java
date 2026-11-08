package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;

/** Translates one shared snapshot into Renewal parameters without reinterpretation of INT/WIL. */
public record RenewalParameters(
        double maximumMana, double manaRegeneration,
        long regenerationDelayTicks, double cost,
        long cooldownTicks, float healing) {

    /** Historical level-I mapping retained for AI fixtures and M3 tests. */
    public static RenewalParameters from(PlayerModifierSnapshot stats) {
        return from(stats, 1);
    }

    /** One level-scaled mana capacity, all other factors from Character Core. */
    public static RenewalParameters from(PlayerModifierSnapshot stats, int characterLevel) {
        if (stats == null) throw new IllegalArgumentException("Missing character snapshot");
        double magnitude = AbilityCatalog.RENEWAL.baseMagnitude()
                * stats.get(PlayerModifierId.HEALING_POWER)
                * stats.get(PlayerModifierId.HEALING_RECEIVED);
        if (!Double.isFinite(magnitude) || magnitude <= 0D || magnitude > Float.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid healing magnitude");
        }
        return new RenewalParameters(
                ManaCapacityPolicy.maximum(stats, characterLevel),
                stats.get(PlayerModifierId.MANA_REGENERATION),
                AbilityRules.regenDelayTicks(stats.get(PlayerModifierId.MANA_REGEN_DELAY)),
                AbilityRules.scaledCost(AbilityCatalog.RENEWAL.baseCost(),
                        stats.get(PlayerModifierId.MANA_COST)),
                AbilityRules.scaledCooldown(AbilityCatalog.RENEWAL.cooldownTicks(),
                        stats.get(PlayerModifierId.COOLDOWN_RECOVERY)),
                (float) magnitude);
    }

    public RenewalParameters {
        if (!Double.isFinite(maximumMana) || maximumMana < 0
                || !Double.isFinite(manaRegeneration) || manaRegeneration < 0
                || regenerationDelayTicks < 0 || !Double.isFinite(cost) || cost < 0
                || cooldownTicks < 0 || !Float.isFinite(healing) || healing <= 0) {
            throw new IllegalArgumentException("Invalid Renewal parameters");
        }
    }
}
