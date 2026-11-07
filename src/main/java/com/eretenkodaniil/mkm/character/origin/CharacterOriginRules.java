package com.eretenkodaniil.mkm.character.origin;

import java.util.Objects;

/**
 * Strict one-time server transition. A client may request only a pair of catalog IDs,
 * not stats, XP, grants, items, specialization, or a revision arbitrarily assigned.
 */
public final class CharacterOriginRules {
    public enum Outcome {
        SELECTED, ALREADY_SELECTED, LOCKED, STALE, INVALID, UNSUPPORTED
    }

    public record Result(Outcome outcome, CharacterOriginData data) {
        public Result {
            Objects.requireNonNull(outcome);
            Objects.requireNonNull(data);
        }
    }

    private CharacterOriginRules() {}

    public static Result select(CharacterOriginData current, String raceId, String classId,
                                int expectedRevision) {
        Objects.requireNonNull(current, "origin");
        if (current.schemaVersion() != CharacterOriginData.SCHEMA_VERSION)
            return new Result(Outcome.UNSUPPORTED, current);
        if (current.selected()) {
            return new Result(current.raceId().equals(raceId) && current.classId().equals(classId)
                    ? Outcome.ALREADY_SELECTED : Outcome.LOCKED, current);
        }
        if (expectedRevision != current.revision())
            return new Result(Outcome.STALE, current);
        if (!OriginIds.valid(raceId) || !OriginIds.valid(classId)
                || OriginCatalog.race(raceId) == null || OriginCatalog.clazz(classId) == null)
            return new Result(Outcome.INVALID, current);
        return new Result(Outcome.SELECTED, current.withChoice(raceId, classId));
    }
}
