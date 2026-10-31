package com.eretenkodaniil.mkm.combat;

/** Pure combat resource rules. No Minecraft runtime dependency. */
public final class CombatRules {
    public static final float HEAVY_BASE_COST = 25.0F;
    public static final float DODGE_BASE_COST = 20.0F;
    public static final long HEAVY_RECOVERY_TICKS = 12L;
    public static final long DODGE_RECOVERY_TICKS = 8L;
    public static final long DODGE_COOLDOWN_TICKS = 18L;
    public static final float HEAVY_DAMAGE_MULTIPLIER = 1.65F;

    private CombatRules() {}

    public static CombatState regenerate(
            CombatState state, long now, float maxStamina, float regenPerSecond) {
        float max = Math.max(0.0F, maxStamina);
        long begin = Math.max(state.lastUpdateTick(), state.regenReadyTick());
        long elapsed = Math.max(0L, now - begin);
        float recovered = (float) (Math.min(elapsed, 200L) * Math.max(0.0F, regenPerSecond) / 20.0D);
        float stamina = Math.min(max, state.stamina() + recovered);
        return new CombatState(
                stamina, Math.max(state.lastUpdateTick(), now),
                state.regenReadyTick(), state.heavyReadyTick(), state.dodgeReadyTick());
    }

    /** Returns null when resource/cooldown validation fails. */
    public static CombatState heavy(
            CombatState state, long now, float cost, long regenDelayTicks) {
        if (!Float.isFinite(cost) || cost < 0.0F
                || now < state.heavyReadyTick() || now < state.dodgeReadyTick()
                || state.stamina() + 0.0001F < cost) {
            return null;
        }
        return new CombatState(state.stamina() - cost, now,
                now + Math.max(0L, regenDelayTicks),
                now + HEAVY_RECOVERY_TICKS, state.dodgeReadyTick());
    }

    /** Returns null when resource/cooldown validation fails. */
    public static CombatState dodge(
            CombatState state, long now, float cost, long regenDelayTicks) {
        if (!Float.isFinite(cost) || cost < 0.0F
                || now < state.heavyReadyTick() || now < state.dodgeReadyTick()
                || state.stamina() + 0.0001F < cost) {
            return null;
        }
        return new CombatState(state.stamina() - cost, now,
                now + Math.max(0L, regenDelayTicks),
                now + DODGE_RECOVERY_TICKS, now + DODGE_COOLDOWN_TICKS);
    }

    public static float scaleIncomingMelee(float vanilla, double power, double melee, boolean heavy) {
        if (!Float.isFinite(vanilla) || vanilla <= 0F) {
            return 0.0F;
        }
        double multiplier = Math.max(0.0D, power) * Math.max(0.0D, melee);
        if (heavy) multiplier *= HEAVY_DAMAGE_MULTIPLIER;
        return (float) Math.min(1000000.0D, vanilla * multiplier);
    }
}
