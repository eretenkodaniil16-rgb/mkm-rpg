package com.eretenkodaniil.mkm.character;

/**
 * Display-only prototype derived values for the Character Sheet.
 *
 * <p>These previews deliberately do not modify vanilla attributes yet. They make all seven
 * persisted primary attributes visible and testable before Combat Core defines authoritative
 * modifier formulas and caps.</p>
 */
public record CharacterDerivedStats(
        int maxHealthPreview,
        int physicalPowerPercent,
        int attackSpeedPercent,
        int maxStaminaPreview,
        int magicPowerPercent,
        int maxManaPreview,
        int weakPointDamagePercent) {

    public static CharacterDerivedStats from(CharacterData data) {
        int strengthDelta = data.strength() - CharacterData.DEFAULT_ATTRIBUTE;
        int dexterityDelta = data.dexterity() - CharacterData.DEFAULT_ATTRIBUTE;
        int vitalityDelta = data.vitality() - CharacterData.DEFAULT_ATTRIBUTE;
        int enduranceDelta = data.endurance() - CharacterData.DEFAULT_ATTRIBUTE;
        int intelligenceDelta = data.intelligence() - CharacterData.DEFAULT_ATTRIBUTE;
        int willpowerDelta = data.willpower() - CharacterData.DEFAULT_ATTRIBUTE;
        int perceptionDelta = data.perception() - CharacterData.DEFAULT_ATTRIBUTE;

        int maxHealth = Math.max(1, 20 + vitalityDelta * 2);
        int physicalPower = Math.max(10, 100 + strengthDelta * 4);
        int attackSpeed = Math.max(25, 100 + dexterityDelta * 2);
        int maxStamina = Math.max(10, 100 + enduranceDelta * 5);
        int magicPower = Math.max(10, 100 + intelligenceDelta * 4);
        int maxMana = Math.max(0, 100 + willpowerDelta * 5);
        int weakPointDamage = Math.max(25, 100 + perceptionDelta * 3);

        return new CharacterDerivedStats(
                maxHealth,
                physicalPower,
                attackSpeed,
                maxStamina,
                magicPower,
                maxMana,
                weakPointDamage);
    }
}
