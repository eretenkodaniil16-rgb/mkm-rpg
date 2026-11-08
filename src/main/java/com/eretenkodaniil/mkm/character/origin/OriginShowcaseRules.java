package com.eretenkodaniil.mkm.character.origin;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.quests.FordQuestState;

/** Deterministic race dialogue and mentorship predicates. No Minecraft/client dependency. */
public final class OriginShowcaseRules {
    public enum Clue { HUMAN_PARLEY, DWARF_RUNES }
    public enum Unlock { GRANTED, ALREADY_UNLOCKED, NOT_READY, UNSUPPORTED }

    private OriginShowcaseRules() {}

    private static boolean validOrigin(CharacterOriginData origin) {
        return origin != null && origin.schemaVersion() == CharacterOriginData.SCHEMA_VERSION
                && origin.selected()
                && OriginCatalog.race(origin.raceId()) != null
                && OriginCatalog.clazz(origin.classId()) != null;
    }

    public static boolean canDiscover(CharacterOriginData origin, FordQuestState quest, Clue clue) {
        if (!validOrigin(origin) || quest == null
                || quest.schemaVersion() != FordQuestState.SCHEMA_VERSION || quest.foundTracks())
            return false;
        if (!quest.stage().equals(FordQuestState.NOT_STARTED)
                && !quest.stage().equals(FordQuestState.ACTIVE)) return false;
        return switch (clue) {
            case HUMAN_PARLEY -> origin.raceId().equals(OriginCatalog.HUMAN.id());
            case DWARF_RUNES -> origin.raceId().equals(OriginCatalog.DWARF.id());
        };
    }

    public static Unlock canLearn(CharacterData character, CharacterOriginData origin,
                                  OriginProgressData progress, FordQuestState quest) {
        if (character == null || origin == null || progress == null || quest == null
                || character.schemaVersion() != CharacterData.CURRENT_SCHEMA_VERSION
                || !validOrigin(origin) || progress.schemaVersion() != OriginProgressData.SCHEMA_VERSION
                || quest.schemaVersion() != FordQuestState.SCHEMA_VERSION) return Unlock.UNSUPPORTED;
        if (progress.has(OriginProgressData.FORD_MENTORSHIP)) return Unlock.ALREADY_UNLOCKED;
        if (progress.unlockedNodes().size() >= OriginProgressData.MAX_NODES
                || character.level() < 2 || !quest.foundTracks()
                || !FordQuestState.REWARDED.equals(quest.stage())) return Unlock.NOT_READY;
        return Unlock.GRANTED;
    }

    public static boolean allowedClassAction(CharacterOriginData origin, String clazz) {
        return validOrigin(origin) && origin.classId().equals(clazz);
    }
}
