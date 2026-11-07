package com.eretenkodaniil.mkm.abilities;

/**
 * Shared, pure preflight for player and server-controlled AI Renewal.
 * A rejected cast has no resource debit and cannot emit APPLIED.
 */
public final class RenewalCastPlan {
    private RenewalCastPlan() {}

    public static ManaLedger spendIfAllowed(
            boolean actorValid, boolean actionAllowed, boolean injured,
            ManaLedger previous, long now, RenewalParameters terms) {
        if (previous == null || terms == null || now < 0) return null;
        ManaLedger available = AbilityRules.regenerate(previous, now,
                terms.maximumMana(), terms.manaRegeneration());
        if (RenewalEligibility.check(actorValid, actionAllowed, injured,
                available, now, terms.cost()) != RenewalEligibility.Result.READY) return null;
        return AbilityRules.spend(available, AbilityCatalog.RENEWAL, now,
                terms.cost(), terms.cooldownTicks(), terms.regenerationDelayTicks());
    }
}
