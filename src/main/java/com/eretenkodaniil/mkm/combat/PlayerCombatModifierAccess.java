package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Strict player-side access: compose verified sources, calculate once through
 * Character's original PlayerModifierCalculator. No new formula or storage.
 */
public final class PlayerCombatModifierAccess {
    private PlayerCombatModifierAccess() {}

    public static PlayerModifierSnapshot snapshot(
            CharacterData data, Map<EquipmentRules.Slot, String> verifiedEquipment,
            List<CombatModifierContributions.Contribution> serverFeatures) {
        return PlayerModifierCalculator.calculate(
                Objects.requireNonNull(data, "data"),
                CombatModifierContributions.combine(verifiedEquipment, serverFeatures));
    }
}
