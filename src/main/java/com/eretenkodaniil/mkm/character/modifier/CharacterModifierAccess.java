package com.eretenkodaniil.mkm.character.modifier;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.Map;
import java.util.Objects;

/**
 * Consumer-facing snapshot access. Combat, Magic, Equipment and UI all use the SAME
 * PlayerModifierCalculator and immutable PlayerModifierSnapshot; no independent formulas.
 *
 * The Equipment stream supplies its equipped-only delta map, resolved on the server from
 * native equipment slots. Never reconstruct equipment from client animation/pose data.
 * Pass the same authoritative delta set for a staged Character Sheet preview.
 */
public final class CharacterModifierAccess {
    private CharacterModifierAccess() {}

    public static PlayerModifierSnapshot base(CharacterData character) {
        return PlayerModifierCalculator.calculate(Objects.requireNonNull(character, "character"));
    }

    public static PlayerModifierSnapshot withContributions(
            CharacterData character, Map<PlayerModifierId, Double> equippedAndPassiveDeltas) {
        return PlayerModifierCalculator.calculate(
                Objects.requireNonNull(character, "character"),
                Objects.requireNonNull(equippedAndPassiveDeltas, "contributions"));
    }

    /**
     * Preview-only calculation: the character changes, not the verified equipped bonuses.
     * The server still owns actual actions and never uses a client's preview for damage.
     */
    public static PlayerModifierSnapshot preview(
            CharacterData stagedCharacter, Map<PlayerModifierId, Double> existingEquipmentDeltas) {
        return withContributions(stagedCharacter, existingEquipmentDeltas);
    }
}
