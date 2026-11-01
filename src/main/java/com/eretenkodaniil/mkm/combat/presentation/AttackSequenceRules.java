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

    public record State(EquipmentRules.Family family, String weaponId,
                        int step, long lastTick, long sequenceId) {
        public State {
            Objects.requireNonNull(family, "family");
            Objects.requireNonNull(weaponId, "weaponId");
            if (weaponId.length() > 160 || step < 1 || step > 4
                    || lastTick < 0L || sequenceId < 0L) {
                throw new IllegalArgumentException("Invalid cosmetic sequence state");
            }
        }
    }

    public record Selection(State state, boolean alternatePause, boolean finisher) {}

    public static Selection next(State previous, EquipmentRules.Family family,
                                 long tick, int maxSteps) {
        return next(previous, family, "", tick, maxSteps,
                FAST_CHAIN_MAX_GAP_TICKS, ALTERNATE_PAUSE_MAX_GAP_TICKS);
    }

    public static Selection next(State previous, EquipmentRules.Family family,
                                 long tick, int maxSteps,
                                 long fastChainWindowTicks, long alternatePauseWindowTicks) {
        return next(previous, family, "", tick, maxSteps,
                fastChainWindowTicks, alternatePauseWindowTicks);
    }

    /** Switching weapon identity resets stage without recycling the sequence ID. */
    public static Selection next(State previous, EquipmentRules.Family family,
                                 String weaponId, long tick, int maxSteps,
                                 long fastChainWindowTicks, long alternatePauseWindowTicks) {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(weaponId, "weaponId");
        if (tick < 0L || weaponId.length() > 160 || maxSteps < 1 || maxSteps > 4
                || fastChainWindowTicks < 1L || fastChainWindowTicks > 40L
                || alternatePauseWindowTicks <= fastChainWindowTicks
                || alternatePauseWindowTicks > 60L) {
            throw new IllegalArgumentException("Invalid attack cadence input");
        }
        if (previous == null || family != previous.family()
                || !weaponId.equals(previous.weaponId()) || tick < previous.lastTick()) {
            return new Selection(new State(family, weaponId, 1, tick, previous == null ? 1L
                    : nextId(previous.sequenceId())), false, false);
        }
        long gap = tick - previous.lastTick();
        if (gap > alternatePauseWindowTicks) {
            return new Selection(new State(family, weaponId, 1, tick, nextId(previous.sequenceId())),
                    false, false);
        }
        if (gap > fastChainWindowTicks) {
            return new Selection(new State(family, weaponId, 1, tick, nextId(previous.sequenceId())),
                    true, false);
        }
        int step = Math.min(maxSteps, previous.step() + 1);
        return new Selection(new State(family, weaponId, step, tick, previous.sequenceId()),
                false, step == maxSteps && previous.step() < maxSteps);
    }

    private static long nextId(long id) {
        return id == Long.MAX_VALUE ? 1L : id + 1L;
    }
}
