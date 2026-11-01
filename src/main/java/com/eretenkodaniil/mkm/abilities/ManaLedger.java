package com.eretenkodaniil.mkm.abilities;

import java.util.Map;

/**
 * Immutable owner-owned mana and cooldown ledger. Separate from CharacterData v2
 * and CombatState; does not implement a second action/damage state machine.
 */
public record ManaLedger(
        double mana,
        long lastUpdateTick,
        long regenReadyTick,
        Map<String, Long> cooldowns) {
    public static final int MAX_COOLDOWNS = 64;

    public ManaLedger {
        if (!Double.isFinite(mana) || mana < 0
                || lastUpdateTick < 0 || regenReadyTick < 0 || cooldowns == null
                || cooldowns.size() > MAX_COOLDOWNS) {
            throw new IllegalArgumentException("Invalid mana ledger");
        }
        for (var entry : cooldowns.entrySet()) {
            if (!AbilityIds.valid(entry.getKey())
                    || entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("Invalid cooldown entry");
            }
        }
        cooldowns = Map.copyOf(cooldowns);
    }

    public ManaLedger() {
        this(100.0D, 0L, 0L, Map.of());
    }

    public long readyTick(String abilityId) {
        return cooldowns.getOrDefault(abilityId, 0L);
    }
}
