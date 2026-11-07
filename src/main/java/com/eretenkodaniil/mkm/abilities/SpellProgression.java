package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.ProgressionRules;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ten spell circles across Character Core levels 1..100.
 * Metadata never registers an executable spell handler, damage action,
 * passive modifier, learned grant or class/race progression state.
 */
public final class SpellProgression {
    private static final Map<String, SpellProfile> PROFILES = build();

    private SpellProgression() {}

    /** Tier I is available at character level 1; II at 10; ... X at 90. */
    public static int minimumLevel(int tier) {
        if (tier < 1 || tier > SpellProfile.MAX_SPELL_TIER) {
            throw new IllegalArgumentException("Spell tier must be I-X");
        }
        return tier == 1 ? 1 : (tier - 1) * 10;
    }

    public static int maximumTierAtLevel(int characterLevel) {
        if (characterLevel < ProgressionRules.MIN_LEVEL
                || characterLevel > ProgressionRules.MAX_LEVEL) {
            throw new IllegalArgumentException("Character level must be 1..100");
        }
        return Math.min(SpellProfile.MAX_SPELL_TIER,
                1 + characterLevel / 10);
    }

    public static boolean meetsLevel(String abilityId, int actorLevel) {
        SpellProfile profile = PROFILES.get(abilityId);
        return profile != null && actorLevel >= ProgressionRules.MIN_LEVEL
                && actorLevel <= ProgressionRules.MAX_LEVEL
                && actorLevel >= profile.requiredCharacterLevel()
                && (profile.tier() == 0 || profile.tier() <= maximumTierAtLevel(actorLevel));
    }

    public static SpellProfile get(String abilityId) {
        return PROFILES.get(abilityId);
    }

    public static Map<String, SpellProfile> all() {
        return PROFILES;
    }

    private static Map<String, SpellProfile> build() {
        Map<String, SpellProfile> out = new LinkedHashMap<>();
        put(out, "mkm:renewal", SpellSchool.RESTORATION, 1);
        put(out, "mkm:arcane_bolt", SpellSchool.EVOCATION, 1);
        put(out, "mkm:arcane_barrier", SpellSchool.ABJURATION, 3);
        put(out, "mkm:repulsion_wave", SpellSchool.EVOCATION, 4);
        put(out, "mkm:ancestral_focus", SpellSchool.ENCHANTMENT, 2);
        // Non-spell class/racial passives are not magical circles.
        put(out, "mkm:iron_will", SpellSchool.NONE, 0);
        put(out, "mkm:stone_resilience", SpellSchool.NONE, 0);
        if (!out.keySet().equals(AbilityCatalog.all().keySet())) {
            throw new IllegalStateException("Every catalog ability must have a spell profile");
        }
        return Collections.unmodifiableMap(out);
    }

    private static void put(Map<String, SpellProfile> out,
            String abilityId, SpellSchool school, int tier) {
        if (AbilityCatalog.get(abilityId) == null) {
            throw new IllegalStateException("Unknown ability profile: " + abilityId);
        }
        int level = tier == 0 ? 1 : minimumLevel(tier);
        if (out.putIfAbsent(abilityId,
                new SpellProfile(abilityId, school, tier, level)) != null) {
            throw new IllegalStateException("Duplicate spell profile: " + abilityId);
        }
    }
}
