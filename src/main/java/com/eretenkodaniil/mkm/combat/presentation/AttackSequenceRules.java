package com.eretenkodaniil.mkm.combat.presentation;

import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.Objects;

/**
 * Pure cosmetic cadence: observes real LMB attempts, NEVER schedules attacks,
 * debits stamina or changes Minecraft attack strength/speed.
 */
public final class AttackSequenceRules {
    public static final long FAST_CHAIN_MAX_GAP_TICKS = 4L;
    public static final long ALTERNATE_PAUSE_MAX_GAP_TICKS = 11L;

    private AttackSequenceRules() {}

    public record State(EquipmentRules.Family family, int step, long lastTick, long sequenceId) {
        public State {
            Objects.requireNonNull(family, "family");
            if (step < 1 || step > 4 || lastTick < 0L || sequenceId < 0L) {
                throw new IllegalArgumentException("Invalid cosmetic sequence state");
            }
        }
    }

    public record Selection(State state, boolean alternatePause, boolean finisher) {}

    public static Selection next(State previous, EquipmentRules.Family family,
                                 long tick, int maxSteps) {
        Objects.requireNonNull(family, "family");
        if (tick < 0L || maxSteps < 1 || maxSteps > 4) {
            throw new IllegalArgumentException("Invalid attack cadence input");
        }
        if (previous == null || family != previous.family() || tick < previous.lastTick()) {
            return new Selection(new State(family, 1, tick, previous == null ? 1L
                    : nextId(previous.sequenceId())), false, false);
        }
        long gap = tick - previous.lastTick();
        if (gap > ALTERNATE_PAUSE_MAX_GAP_TICKS) {
            return new Selection(new State(family, 1, tick, nextId(previous.sequenceId())),
                    false, false);
        }
        if (gap > FAST_CHAIN_MAX_GAP_TICKS) {
            return new Selection(new State(family, 1, tick, nextId(previous.sequenceId())),
                    true, false);
        }
        int step = Math.min(maxSteps, previous.step() + 1);
        return new Selection(new State(family, step, tick, previous.sequenceId()),
                false, step == maxSteps && previous.step() < maxSteps);
    }

    private static long nextId(long id) {
        return id == Long.MAX_VALUE ? 1L : id + 1L;
    }
}
