package com.eretenkodaniil.mkm.combat.mode;

import java.util.Objects;

/**
 * Pure and deterministic SERVER tick-only sword input contract. A PRESS/RELEASE
 * pair produces at most one scheduled vanilla attack. Client tick/target/damage
 * are never accepted as inputs, only an increasing sequence and intent code.
 */
public final class CombatModeRules {
    public static final int CHARGE_TICKS = 12;
    public static final int MAX_HOLD_TICKS = 35;
    public static final int QUICK_WINDUP_TICKS = 4;
    public static final int CHARGED_WINDUP_TICKS = 7;
    public static final int QUICK_COOLDOWN_TICKS = 13;
    public static final int CHARGED_COOLDOWN_TICKS = 24;

    public enum Phase { IDLE, HELD, QUEUED }
    public enum Strike { NONE, QUICK, CHARGED }

    public record State(boolean enabled, Phase phase, int latestSequence,
                        long pressedAt, long impactAt, long cooldownUntil,
                        String itemId, Strike strike) {
        public State {
            Objects.requireNonNull(phase);
            Objects.requireNonNull(itemId);
            Objects.requireNonNull(strike);
            if (latestSequence < -1 || pressedAt < 0L || impactAt < 0L
                    || cooldownUntil < 0L || itemId.length() > 160) {
                throw new IllegalArgumentException("Malformed sword state");
            }
        }
        public State() {
            this(false, Phase.IDLE, -1, 0L, 0L, 0L, "minecraft:air", Strike.NONE);
        }
        public boolean busy() { return phase != Phase.IDLE; }
    }

    private CombatModeRules() {}

    public static State mode(State old, boolean enabled) {
        Objects.requireNonNull(old);
        return new State(enabled, Phase.IDLE, old.latestSequence(), 0L, 0L,
                old.cooldownUntil(), "minecraft:air", Strike.NONE);
    }

    public static State press(State old, int sequence, long now, String actualItemId,
                              boolean allowed) {
        Objects.requireNonNull(old);
        Objects.requireNonNull(actualItemId);
        if (!allowed || !old.enabled() || old.busy() || now < 0L
                || now < old.cooldownUntil() || sequence < 0
                || sequence <= old.latestSequence()) return old;
        return new State(true, Phase.HELD, sequence, now, 0L, old.cooldownUntil(),
                actualItemId, Strike.NONE);
    }

    public static State release(State old, int sequence, long now, boolean allowed) {
        Objects.requireNonNull(old);
        if (!allowed || !old.enabled() || old.phase() != Phase.HELD
                || sequence != old.latestSequence() || now < old.pressedAt()) return old;
        long held = now - old.pressedAt();
        if (held > MAX_HOLD_TICKS) return cancel(old);
        Strike kind = held >= CHARGE_TICKS ? Strike.CHARGED : Strike.QUICK;
        long impact = now + (kind == Strike.CHARGED ? CHARGED_WINDUP_TICKS : QUICK_WINDUP_TICKS);
        long cooldown = now + (kind == Strike.CHARGED
                ? CHARGED_COOLDOWN_TICKS : QUICK_COOLDOWN_TICKS);
        return new State(true, Phase.QUEUED, sequence, old.pressedAt(), impact,
                cooldown, old.itemId(), kind);
    }

    public static State cancel(State state) {
        Objects.requireNonNull(state);
        return new State(state.enabled(), Phase.IDLE, state.latestSequence(), 0L, 0L,
                state.cooldownUntil(), "minecraft:air", Strike.NONE);
    }

    public static State expire(State state, long now) {
        if (state.phase() == Phase.HELD
                && (now < state.pressedAt() || now - state.pressedAt() > MAX_HOLD_TICKS)) {
            return cancel(state);
        }
        return state;
    }

    public static boolean readyToImpact(State state, long now, String actualItemId,
                                        boolean stillAllowed) {
        return stillAllowed && state.enabled() && state.phase() == Phase.QUEUED
                && now >= state.impactAt() && now >= state.pressedAt()
                && state.itemId().equals(actualItemId);
    }
}
