package com.eretenkodaniil.mkm.character;

/**
 * Display-only prototype derived values for the first Character Sheet.
 *
 * <p>These values deliberately do not modify vanilla attributes yet. Combat Core will decide
 * which derived stats become authoritative gameplay rules. Keeping this calculation isolated
 * lets us iterate on the UI without changing persisted character data.</p>
 *
 * <p>The current STR/DEX/VIT persistence format is a legacy prototype. Until the candidate
 * primary-attribute model is migrated, VIT temporarily acts as a proxy for stamina preview.</p>
 */
public record CharacterDerivedStats(
        int maxHealthPreview,
        int physicalPowerPercent,
        int maxStaminaPreview) {

    public static CharacterDerivedStats from(CharacterData data) {
        int vitalityDelta = data.vitality() - CharacterData.DEFAULT_ATTRIBUTE;
        int strengthDelta = data.strength() - CharacterData.DEFAULT_ATTRIBUTE;

        int maxHealth = Math.max(1, 20 + vitalityDelta * 2 + Math.max(0, data.level() - 1));
        int physicalPower = Math.max(10, 100 + strengthDelta * 5);
        int maxStamina = Math.max(10, 100 + vitalityDelta * 5);

        return new CharacterDerivedStats(maxHealth, physicalPower, maxStamina);
    }
}
