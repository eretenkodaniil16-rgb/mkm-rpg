package com.eretenkodaniil.mkm.abilities;

/**
 * Pure Renewal preflight shared by the server executor and the informational HUD.
 * This is NOT permission to cast: only the server owns final state and debits.
 */
public final class RenewalEligibility {
    public enum Result {
        READY, INVALID_ACTOR, BUSY, FULL_HEALTH, COOLDOWN, LOW_MANA
    }

    private RenewalEligibility() {}

    public static Result check(boolean actorValid, boolean actionIdle, boolean injured,
                               ManaLedger ledger, long now, double cost) {
        if (ledger == null || now < 0 || !Double.isFinite(cost) || cost < 0) {
            throw new IllegalArgumentException("Invalid Renewal evaluation");
        }
        if (!actorValid) return Result.INVALID_ACTOR;
        if (!actionIdle) return Result.BUSY;
        if (!injured) return Result.FULL_HEALTH;
        ManaLedger normalized = AbilityRules.reconcileClock(ledger, now);
        if (now < normalized.readyTick(AbilityCatalog.RENEWAL.id())) return Result.COOLDOWN;
        if (normalized.mana() + 0.000001D < cost) return Result.LOW_MANA;
        return Result.READY;
    }
}
