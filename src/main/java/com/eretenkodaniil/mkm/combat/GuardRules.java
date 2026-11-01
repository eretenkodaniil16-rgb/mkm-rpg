package com.eretenkodaniil.mkm.combat;

/**
 * Shield guard resource math, with no Minecraft dependencies.
 * Only a confirmed vanilla shield block may call this rule.
 */
public final class GuardRules {
    public static final int GUARD_BREAK_COOLDOWN_TICKS = 60;
    public static final float BLOCK_BASE_COST = 4.0F;
    public static final float BLOCK_DAMAGE_COST_FACTOR = 0.5F;
    public static final float BLOCK_MAX_COST = 18.0F;

    private GuardRules() {}

    public static Result block(
            CombatState state, long now, float blockedDamage,
            double blockStability, double staminaCostMultiplier,
            float maximumStamina, float regenerationPerSecond, long regenerationDelayTicks) {
        if (!Float.isFinite(blockedDamage) || blockedDamage < 0.0F
                || !Double.isFinite(blockStability) || blockStability <= 0.0D
                || !Double.isFinite(staminaCostMultiplier) || staminaCostMultiplier < 0.0D) {
            throw new IllegalArgumentException("Invalid guard resource parameters");
        }

        CombatState current = CombatRules.regenerate(state, now, maximumStamina, regenerationPerSecond);
        double boundedDamage = Math.min(10000.0D, blockedDamage);
        double rawCost = Math.min(BLOCK_MAX_COST, BLOCK_BASE_COST
                + boundedDamage * BLOCK_DAMAGE_COST_FACTOR);
        float cost = (float) Math.min(100000.0D,
                rawCost * staminaCostMultiplier / Math.max(0.25D, blockStability));
        boolean broken = current.stamina() + 0.0001F < cost;
        float spent = Math.min(current.stamina(), cost);
        CombatState next = new CombatState(
                Math.max(0.0F, current.stamina() - spent),
                now, Math.max(current.regenReadyTick(), now + Math.max(0L, regenerationDelayTicks)),
                current.heavyReadyTick(), current.dodgeReadyTick());
        return new Result(next, broken, spent);
    }

    public record Result(CombatState state, boolean broken, float spent) {}
}
