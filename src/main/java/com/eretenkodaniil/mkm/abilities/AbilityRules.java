package com.eretenkodaniil.mkm.abilities;

import java.util.HashMap;
import java.util.Map;

/** Pure, deterministic, server-tick-based accounting. No client state or Minecraft types. */
public final class AbilityRules {
    private static final double EPSILON = 0.00001D;
    private static final long REGEN_CATCHUP_CAP_TICKS = 200L;

    private AbilityRules() {}

    public static double scaledCost(double baseCost, double manaCostMultiplier) {
        if (!Double.isFinite(baseCost) || baseCost < 0
                || !Double.isFinite(manaCostMultiplier) || manaCostMultiplier < 0) {
            throw new IllegalArgumentException("Invalid mana cost");
        }
        return baseCost * manaCostMultiplier;
    }

    public static long scaledCooldown(int baseTicks, double recoveryMultiplier) {
        if (baseTicks < 0 || !Double.isFinite(recoveryMultiplier) || recoveryMultiplier <= 0) {
            throw new IllegalArgumentException("Invalid cooldown modifiers");
        }
        return (long) Math.ceil(baseTicks / recoveryMultiplier);
    }

    public static long regenDelayTicks(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Invalid regen delay");
        }
        return (long) Math.ceil(seconds * 20.0D);
    }

    public static ManaLedger regenerate(ManaLedger current, long now, double maximum,
                                        double regenPerSecond) {
        if (now < 0 || !Double.isFinite(maximum) || maximum < 0
                || !Double.isFinite(regenPerSecond) || regenPerSecond < 0) {
            throw new IllegalArgumentException("Invalid regeneration parameters");
        }
        long begin = Math.max(current.lastUpdateTick(), current.regenReadyTick());
        long elapsed = now > begin ? Math.min(REGEN_CATCHUP_CAP_TICKS, now - begin) : 0L;
        double nextMana = Math.min(maximum,
                current.mana() + elapsed * regenPerSecond / 20.0D);
        return new ManaLedger(nextMana, Math.max(current.lastUpdateTick(), now),
                current.regenReadyTick(), current.cooldowns());
    }

    /**
     * One atomic debit and cooldown reservation on the server. Returns null on refusal.
     * Call only after target/eligibility and effect-precondition validation.
     */
    public static ManaLedger spend(ManaLedger current, AbilityDefinition ability,
                                   long now, double cost, long cooldownTicks,
                                   long regenDelayTicks) {
        if (now < 0 || ability.kind() != AbilityDefinition.Kind.ACTIVE
                || ability.resource() != AbilityDefinition.Resource.MANA
                || ability.castTicks() != 0 || !Double.isFinite(cost) || cost < 0
                || cooldownTicks < 0 || regenDelayTicks < 0
                || current.mana() + EPSILON < cost
                || now < current.readyTick(ability.id())) {
            return null;
        }
        Map<String, Long> nextCooldowns = new HashMap<>(current.cooldowns());
        if (!nextCooldowns.containsKey(ability.id())
                && nextCooldowns.size() >= ManaLedger.MAX_COOLDOWNS) {
            return null;
        }
        nextCooldowns.put(ability.id(), saturatingAdd(now, cooldownTicks));
        return new ManaLedger(Math.max(0.0D, current.mana() - cost), now,
                saturatingAdd(now, regenDelayTicks), nextCooldowns);
    }

    private static long saturatingAdd(long a, long b) {
        return b > Long.MAX_VALUE - a ? Long.MAX_VALUE : a + b;
    }
}
