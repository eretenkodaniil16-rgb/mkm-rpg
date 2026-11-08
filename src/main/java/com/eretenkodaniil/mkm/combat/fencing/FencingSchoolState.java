package com.eretenkodaniil.mkm.combat.fencing;

import java.util.Objects;

/**
 * Transient owner-synchronized school selection, NOT saved in CharacterData.
 * A chosen school becomes effective after the warmup: neither server damage
 * nor MNAE may infer a new attack from this state transition.
 */
public record FencingSchoolState(
        FencingSchool school, long effectiveFromTick, long nextSwitchTick) {
    public FencingSchoolState {
        Objects.requireNonNull(school, "school");
        if (effectiveFromTick < 0L || nextSwitchTick < 0L
                || effectiveFromTick > nextSwitchTick) {
            throw new IllegalArgumentException("Invalid fencing switch clock");
        }
    }

    public FencingSchoolState() {
        this(FencingSchool.NONE, 0L, 0L);
    }

    public boolean activeAt(long now) {
        return now >= effectiveFromTick && school != FencingSchool.NONE;
    }

    public long remainingWarmup(long now) {
        return school == FencingSchool.NONE ? 0L
                : Math.max(0L, effectiveFromTick - now);
    }

    public long remainingSwitchCooldown(long now) {
        return Math.max(0L, nextSwitchTick - now);
    }

}
