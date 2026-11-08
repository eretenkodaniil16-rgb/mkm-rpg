package com.eretenkodaniil.mkm.combat.mode;

/**
 * Bounded server-thread-only inbound packet budget per connected actor.
 * Rate-limit covers invalid/cancel and mode-toggle traffic, not just hits.
 */
public final class CombatPacketBudget {
    public static final int MAX_MESSAGES_PER_TICK = 8;
    public record State(long lastTick, int received) {
        public State {
            if (lastTick < -1L || received < 0 || received > MAX_MESSAGES_PER_TICK) {
                throw new IllegalArgumentException("Invalid packet counter");
            }
        }
        public State() { this(-1L, 0); }
    }
    private CombatPacketBudget() {}

    /** Returns unchanged state for a packet rejected by the per-tick limit. */
    public static State accept(State old, long now) {
        if (now < 0 || now < old.lastTick()) return old;
        if (now != old.lastTick()) return new State(now, 1);
        if (old.received() >= MAX_MESSAGES_PER_TICK) return old;
        return new State(now, old.received() + 1);
    }

    public static boolean accepted(State before, State after) {
        return before != after;
    }
}
