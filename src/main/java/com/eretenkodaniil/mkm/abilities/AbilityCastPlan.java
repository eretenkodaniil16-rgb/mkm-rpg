package com.eretenkodaniil.mkm.abilities;

/**
 * Actor-neutral atomic resource/cooldown preflight. The caller supplies
 * server-verified actor/target/channel evidence. No client amount accepted.
 * The immutable return value is committed once BEFORE one native effect.
 */
public final class AbilityCastPlan {
    private AbilityCastPlan() {}

    public static ManaLedger spendIfAllowed(AbilityDefinition ability,
            boolean actorValid, boolean combatIdle, boolean effectAllowed,
            ManaLedger previous, long tick, double maxMana, double regenPerSecond,
            double manaCostMultiplier, double cooldownRecovery, double regenDelaySeconds) {
        if (ability == null || previous == null || tick < 0
                || !actorValid || !combatIdle || !effectAllowed
                || ability.kind() != AbilityDefinition.Kind.ACTIVE
                || ability.resource() != AbilityDefinition.Resource.MANA
                || ability.castTicks() != 0) return null;
        ManaLedger current = AbilityRules.regenerate(previous, tick, maxMana, regenPerSecond);
        double cost = AbilityRules.scaledCost(ability.baseCost(), manaCostMultiplier);
        long cooldown = AbilityRules.scaledCooldown(ability.cooldownTicks(), cooldownRecovery);
        long delay = AbilityRules.regenDelayTicks(regenDelaySeconds);
        return AbilityRules.spend(current, ability, tick, cost, cooldown, delay);
    }
}
