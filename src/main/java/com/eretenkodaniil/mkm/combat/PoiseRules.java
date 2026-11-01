package com.eretenkodaniil.mkm.combat;

/** Pure, server-independent poise decay and stagger calculation. */
public final class PoiseRules {
    public static final float STAGGER_THRESHOLD = 20.0F;
    private static final float DECAY_PER_TICK = 0.20F;
    private static final long STAGGER_TICKS = 25L;

    private PoiseRules() {}

    public static Result onHit(
            PoiseState state, long now, float healthDamage, double staggerPower, boolean heavy) {
        return onHit(state, now, healthDamage, staggerPower, 1.0D, heavy);
    }

    public static Result onHit(
            PoiseState state, long now, float healthDamage,
            double staggerPower, double staggerResistance, boolean heavy) {
        if (!Float.isFinite(healthDamage) || healthDamage <= 0.0F
                || !Double.isFinite(staggerPower) || !Double.isFinite(staggerResistance)
                || now < state.staggeredUntilTick()) {
            return new Result(state, false);
        }

        long elapsed = Math.max(0L, now - state.lastHitTick());
        float decay = (float) Math.min(100000L, elapsed) * DECAY_PER_TICK;
        float current = Math.max(0.0F, state.buildup() - decay);
        double impact = Math.min(12.0D, healthDamage * 0.7D) + (heavy ? 10.0D : 3.0D);
        double effectivePower = Math.min(3.0D, Math.max(0.0D, staggerPower));
        double effectiveResistance = Math.min(3.0D, Math.max(0.25D, staggerResistance));
        float next = current + (float) (impact * effectivePower / effectiveResistance);

        if (next >= STAGGER_THRESHOLD) {
            return new Result(new PoiseState(0.0F, now, now + STAGGER_TICKS), true);
        }
        return new Result(new PoiseState(next, now, state.staggeredUntilTick()), false);
    }

    public record Result(PoiseState state, boolean staggered) {}
}
