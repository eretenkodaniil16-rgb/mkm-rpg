package com.eretenkodaniil.mkm.abilities;

/**
 * Metadata only. Tier describes spell complexity, not the player's RPG level.
 * A catalog grant and an appropriate rank never bypass a server effect allowlist.
 */
public record SpellProfile(
        String abilityId, SpellSchool school, int tier, int requiredCharacterLevel) {
    public static final int MIN_SPELL_TIER = 1;
    public static final int MAX_SPELL_TIER = 10;

    public SpellProfile {
        if (!AbilityIds.valid(abilityId) || school == null
                || tier < 0 || tier > MAX_SPELL_TIER
                || requiredCharacterLevel < 1 || requiredCharacterLevel > 100) {
            throw new IllegalArgumentException("Invalid spell profile");
        }
        if ((tier == 0) != (school == SpellSchool.NONE)) {
            throw new IllegalArgumentException("Only nonspells use NONE school/tier 0");
        }
        if (tier > 0 && requiredCharacterLevel < SpellProgression.minimumLevel(tier)) {
            throw new IllegalArgumentException("Spell tier exceeds character level gate");
        }
    }

    public boolean isSpell() {
        return tier >= MIN_SPELL_TIER;
    }
}
