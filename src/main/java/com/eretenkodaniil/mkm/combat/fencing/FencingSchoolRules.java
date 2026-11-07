package com.eretenkodaniil.mkm.combat.fencing;

import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.Objects;

/**
 * Pure server selection policy. A school switch requires an idle actor and
 * carries a warmup and cooldown to prevent mid-hit/guard stat swapping.
 * No client animation/crosshair event is consulted.
 */
public final class FencingSchoolRules {
    public static final long WARMUP_TICKS = 20L;
    public static final long SWITCH_COOLDOWN_TICKS = 80L;

    public enum ResultCode {
        APPLIED, ALREADY_SELECTED, ACTION_BUSY, SWITCH_COOLDOWN,
        REQUIRES_MAINHAND_SWORD, INVALID_TICK
    }

    public record Result(ResultCode code, FencingSchoolState state) {
        public Result {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(state, "state");
        }
        public boolean applied() {
            return code == ResultCode.APPLIED;
        }
    }

    private FencingSchoolRules() {}

    public static Result select(FencingSchoolState current,
                                FencingSchool requested,
                                EquipmentRules.Family actualMainhandFamily,
                                boolean actorReady, long serverTick) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(actualMainhandFamily, "actualMainhandFamily");
        if (serverTick < 0L) return new Result(ResultCode.INVALID_TICK, current);
        if (!actorReady) return new Result(ResultCode.ACTION_BUSY, current);
        if (requested == current.school()) {
            return new Result(ResultCode.ALREADY_SELECTED, current);
        }
        if (serverTick < current.nextSwitchTick()) {
            return new Result(ResultCode.SWITCH_COOLDOWN, current);
        }
        // Disarming a stance to NONE is possible regardless of held item;
        // activating a school always requires a REAL mainhand sword.
        if (requested != FencingSchool.NONE && actualMainhandFamily != EquipmentRules.Family.SWORD) {
            return new Result(ResultCode.REQUIRES_MAINHAND_SWORD, current);
        }
        long warmup = serverTick > Long.MAX_VALUE - WARMUP_TICKS
                ? Long.MAX_VALUE : serverTick + WARMUP_TICKS;
        long nextSwitch = serverTick > Long.MAX_VALUE - SWITCH_COOLDOWN_TICKS
                ? Long.MAX_VALUE : serverTick + SWITCH_COOLDOWN_TICKS;
        return new Result(ResultCode.APPLIED,
                new FencingSchoolState(requested,
                        requested == FencingSchool.NONE ? serverTick : warmup, nextSwitch));
    }
}
