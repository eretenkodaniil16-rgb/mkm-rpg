package com.eretenkodaniil.mkm.abilities;

import java.util.Map;
import java.util.Set;

/**
 * Declarative PREVIEW of future Character Core class/race grants.
 * No class/race fields or saves are created here. Character Core will supply
 * selected IDs and own unlock/persistence/migration.
 */
public final class AbilityArchetypes {
    public static final String WARRIOR = "mkm:warrior";
    public static final String MAGE = "mkm:mage";
    public static final String STONEBORN = "mkm:stoneborn";
    public static final String WINDBORN = "mkm:windborn";

    private static final Map<String, Set<String>> CLASSES = Map.of(
            WARRIOR, Set.of("mkm:iron_will"),
            MAGE, Set.of("mkm:arcane_bolt"));
    private static final Map<String, Set<String>> RACES = Map.of(
            STONEBORN, Set.of("mkm:stone_resilience"),
            WINDBORN, Set.of("mkm:ancestral_focus"));

    private AbilityArchetypes() {}

    public static Set<String> classGrants(String classId) {
        return CLASSES.getOrDefault(classId, Set.of());
    }

    public static Set<String> raceGrants(String raceId) {
        return RACES.getOrDefault(raceId, Set.of());
    }
}
