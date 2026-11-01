package com.eretenkodaniil.mkm.abilities;

import java.util.HashMap;
import java.util.Map;

/**
 * Pure server-tick-based resource accounting. No Minecraft types, client state,
 * alternative attribute formula or damage resolution.
 */
public final class AbilityRules {
    private static final double EPSILON = 0.000001D;
    private static final long REGEN_CATCHUP_CAP_TICKS = 200L;

    private AbilityRules() {}

    public static double scaledCost(double baseCost, double manaCostMultiplier) {
        if (!Double.isFinite(baseCost) || baseCost < 0
                || !Double.isFinite(manaCostMultiplier) || manaCostMultiplier < 0) {
            throw new IllegalArgumentException("Invalid mana cost");
        }
        double cost = baseCost * manaCostMultiplier;
        if (!Double.isFinite(cost)) throw new IllegalArgumentException("Mana cost overflow");
        return cost;
    }

    public static long scaledCooldown(int baseTicks, double recoveryMultiplier) {
        if (baseTicks < 0 || !Double.isFinite(recoveryMultiplier) || recoveryMultiplier <= 0) {
            throw new IllegalArgumentException("Invalid cooldown modifiers");
        }
        double scaled = Math.ceil(baseTicks / recoveryMultiplier);
        if (!Double.isFinite(scaled) || scaled > Long.MAX_VALUE) {
            throw new IllegalArgumentException("Cooldown overflow");
        }
        return (long) scaled;
    }

    public static long regenDelayTicks(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0
                || seconds > (double) Long.MAX_VALUE / 20.0D) {
            throw new IllegalArgumentException("Invalid regen delay");
        }
        return (long) Math.ceil(seconds * 20.0D);
    }

    /**
     * World clocks can move backwards after a world rollback or restoring a
     * backup. Preserve remaining cooldown/delay duration rather than blocking
     * casting indefinitely on deadlines from the old future clock.
     */
    public static ManaLedger reconcileClock(ManaLedger current, long now) {
        if (now < 0) throw new IllegalArgumentException("Negative server tick");
        if (now >= current.lastUpdateTick()) return current;
        long original = current.lastUpdateTick();
        long delay = remaining(current.regenReadyTick(), original);
        Map<String, Long> rebased = new HashMap<>();
        for (var cooldown : current.cooldowns().entrySet()) {
            long timeLeft = remaining(cooldown.getValue(), original);
            if (timeLeft > 0) rebased.put(cooldown.getKey(), saturatingAdd(now, timeLeft));
        }
        return new ManaLedger(current.mana(), now, saturatingAdd(now, delay), rebased);
    }

    public static ManaLedger regenerate(ManaLedger current, long now,
                                        double maximum, double regenPerSecond) {
        if (now < 0 || !Double.isFinite(maximum) || maximum < 0
                || !Double.isFinite(regenPerSecond) || regenPerSecond < 0) {
            throw new IllegalArgumentException("Invalid regeneration parameters");
        }
        ManaLedger state = reconcileClock(current, now);
        long begin = Math.max(state.lastUpdateTick(), state.regenReadyTick());
        long elapsed = now > begin ? Math.min(REGEN_CATCHUP_CAP_TICKS, now - begin) : 0L;
        double nextMana = Math.min(maximum,
                state.mana() + elapsed * regenPerSecond / 20.0D);
        if (!Double.isFinite(nextMana)) throw new IllegalArgumentException("Mana overflow");
        return new ManaLedger(nextMana, Math.max(state.lastUpdateTick(), now),
                state.regenReadyTick(), state.cooldowns());
    }

    /**
     * Atomic server-owned debit/cooldown. Eligibility and target checks must
     * precede this call; cost and cooldown are always derived server-side.
     */
    public static ManaLedger spend(ManaLedger current, AbilityDefinition ability,
                                   long now, double cost, long cooldownTicks,
                                   long regenDelayTicks) {
        if (ability == null || now < 0 || ability.kind() != AbilityDefinition.Kind.ACTIVE
                || ability.resource() != AbilityDefinition.Resource.MANA
                || ability.castTicks() != 0 || !Double.isFinite(cost) || cost < 0
                || cooldownTicks < 0 || regenDelayTicks < 0) {
            return null;
        }
        ManaLedger state = reconcileClock(current, now);
        if (state.mana() + EPSILON < cost || now < state.readyTick(ability.id())) {
            return null;
        }

        // Prune expired deadlines before adding an ID, otherwise a player
        // who has tried many abilities can hit the fixed 64-entry safety cap.
        Map<String, Long> deadlines = new HashMap<>();
        for (var entry : state.cooldowns().entrySet()) {
            if (entry.getValue() > now) deadlines.put(entry.getKey(), entry.getValue());
        }
        if (!deadlines.containsKey(ability.id()) && deadlines.size() >= ManaLedger.MAX_COOLDOWNS) {
            return null;
        }
        long readyTick = saturatingAdd(now, cooldownTicks);
        if (readyTick > now) deadlines.put(ability.id(), readyTick);
        return new ManaLedger(Math.max(0.0D, state.mana() - cost), now,
                saturatingAdd(now, regenDelayTicks), deadlines);
    }

    private static long remaining(long deadline, long previous) {
        return deadline > previous ? deadline - previous : 0L;
    }

    private static long saturatingAdd(long a, long b) {
        return b > Long.MAX_VALUE - a ? Long.MAX_VALUE : a + b;
    }
}
