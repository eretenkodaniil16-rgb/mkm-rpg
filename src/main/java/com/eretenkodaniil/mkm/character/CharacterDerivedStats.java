package com.eretenkodaniil.mkm.character;

/**
 * Display-only prototype derived values for the first Character Sheet.
 *
 * <p>These values deliberately do not modify vanilla attributes yet. Combat Core will decide
 * which derived stats become authoritative gameplay rules. Keeping this calculation isolated
 * lets us iterate on the UI without changing persisted character data.</p>
 */
public record CharacterDerivedStats(
        int maxHealthPreview,
        int physicalPowerPercent,
        int initiativeBonus) {

    public static CharacterDerivedStats from(CharacterData data) {
        int vitalityDelta = data.vitality() - CharacterData.DEFAULT_ATTRIBUTE;
        int strengthDelta = data.strength() - CharacterData.DEFAULT_ATTRIBUTE;
        int dexterityDelta = data.dexterity() - CharacterData.DEFAULT_ATTRIBUTE;

        int maxHealth = Math.max(1, 20 + vitalityDelta * 2 + Math.max(0, data.level() - 1));
        int physicalPower = Math.max(10, 100 + strengthDelta * 5);

        return new CharacterDerivedStats(maxHealth, physicalPower, dexterityDelta);
    }
}
