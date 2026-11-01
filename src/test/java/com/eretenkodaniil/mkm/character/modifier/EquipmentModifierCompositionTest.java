package com.eretenkodaniil.mkm.character.modifier;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class EquipmentModifierCompositionTest {
    @Test
    void equipmentMergesIntoTheExistingSnapshotWithoutChangingCharacterData() {
        CharacterData character = new CharacterData();
        var baseline = PlayerModifierCalculator.calculate(character);
        var modifiers = EquipmentRules.aggregate(Map.of(
                EquipmentRules.Slot.MAIN_HAND, "minecraft:iron_sword",
                EquipmentRules.Slot.CHEST, "minecraft:iron_chestplate"));
        var equipped = PlayerModifierCalculator.calculate(character, modifiers);
        assertTrue(equipped.get(PlayerModifierId.MELEE_DAMAGE)
                > baseline.get(PlayerModifierId.MELEE_DAMAGE));
        assertTrue(equipped.get(PlayerModifierId.PHYSICAL_RESISTANCE)
                > baseline.get(PlayerModifierId.PHYSICAL_RESISTANCE));
        assertEquals(baseline.get(PlayerModifierId.MAGIC_POWER),
                equipped.get(PlayerModifierId.MAGIC_POWER), 1e-9);
        assertEquals(CharacterData.CURRENT_SCHEMA_VERSION, character.schemaVersion());
    }

    @Test
    void singleFinalSafetyClampAndFiniteContributions() {
        var extreme = PlayerModifierCalculator.calculate(new CharacterData(),
                Map.of(PlayerModifierId.MELEE_DAMAGE, 1000D));
        assertEquals(ModifierDefinitions.get(PlayerModifierId.MELEE_DAMAGE).maxValue(),
                extreme.get(PlayerModifierId.MELEE_DAMAGE), 1e-9);
        assertThrows(IllegalArgumentException.class, () ->
                PlayerModifierCalculator.calculate(new CharacterData(),
                        Map.of(PlayerModifierId.MELEE_DAMAGE, Double.NaN)));
    }
}
