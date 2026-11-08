package com.eretenkodaniil.mkm.abilities;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ten future circles are metadata, NOT ten implemented spell executors.
 * Level + class + actual server handler are separate mandatory checks.
 */
public final class SpellProgression {
    private static final Map<String, SpellProfile> PROFILES = build();
    private SpellProgression() {}

    public static int minimumLevel(int tier) {
        if (tier < 1 || tier > 10) throw new IllegalArgumentException("Tier I-X only");
        return tier == 1 ? 1 : (tier - 1) * 10;
    }

    public static int maximumTierAtLevel(int level) {
        if (level < 1 || level > 100) throw new IllegalArgumentException("Level 1..100");
        return Math.min(10, 1 + level / 10);
    }

    public static boolean meetsLevel(String id, int level) {
        var profile = PROFILES.get(id);
        return profile != null && level >= 1 && level <= 100
                && level >= profile.requiredCharacterLevel()
                && (profile.tier() == 0 || profile.tier() <= maximumTierAtLevel(level));
    }

    public static SpellProfile get(String id) { return PROFILES.get(id); }
    public static Map<String, SpellProfile> all() { return PROFILES; }

    private static Map<String, SpellProfile> build() {
        Map<String, SpellProfile> map = new LinkedHashMap<>();
        put(map, "mkm:renewal", SpellSchool.RESTORATION, 1);
        put(map, "mkm:arcane_bolt", SpellSchool.EVOCATION, 1);
        put(map, "mkm:arcane_barrier", SpellSchool.ABJURATION, 1);
        put(map, "mkm:repulsion_wave", SpellSchool.EVOCATION, 4);
        put(map, "mkm:iron_will", SpellSchool.NONE, 0);
        if (!map.keySet().equals(AbilityCatalog.all().keySet()))
            throw new IllegalStateException("Catalog/profile mismatch");
        return Collections.unmodifiableMap(map);
    }

    private static void put(Map<String, SpellProfile> map, String id, SpellSchool school, int tier) {
        if (AbilityCatalog.get(id) == null) throw new IllegalStateException(id);
        if (map.putIfAbsent(id, new SpellProfile(id, school, tier,
                tier == 0 ? 1 : minimumLevel(tier))) != null)
            throw new IllegalStateException("Duplicate school profile");
    }
}
