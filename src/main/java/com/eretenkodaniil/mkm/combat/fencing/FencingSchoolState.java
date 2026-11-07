package com.eretenkodaniil.mkm.combat.fencing;

import java.util.Objects;
import java.util.Optional;

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

    /** For local-player MNAE only; never authorizes hits or clip IDs. */
    public Optional<String> visualGuardCue(long now) {
        return activeAt(now) ? Optional.of(school.visualGuardCue()) : Optional.empty();
    }
}
