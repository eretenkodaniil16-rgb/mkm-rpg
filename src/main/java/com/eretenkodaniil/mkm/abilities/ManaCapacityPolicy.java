package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.modifier.AttributeScaling;

/**
 * Extra MAX_MANA contribution supplied to EquipmentSnapshots BEFORE the
 * single Character Core final clamp. Follows the GDD's diminishing returns.
 * WIL, equipment and class bonuses are NOT recalculated here.
 */
public final class ManaCapacityPolicy {
    private ManaCapacityPolicy() {}

    public static double levelBonus(int level) {
        if (level < 1 || level > 100) throw new IllegalArgumentException("Invalid level");
        return 6D * Math.sqrt(level - 1D);
    }

    public static double intelligenceBonus(CharacterData character) {
        if (character == null) throw new IllegalArgumentException("Missing character");
        return 2D * AttributeScaling.effectiveDelta(character.intelligence());
    }

    public static double extra(CharacterData character) {
        if (character == null || character.schemaVersion() != CharacterData.CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException("Requires current v4 CharacterData");
        return levelBonus(character.level()) + intelligenceBonus(character);
    }
}
