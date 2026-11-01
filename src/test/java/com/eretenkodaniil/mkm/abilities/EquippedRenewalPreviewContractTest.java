package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.EnumMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Combined M2/M3 + D1 contract: existing vanilla equipment has only physical
 * bonuses and therefore must not apply an accidental extra magic multiplier.
 * The live player adapter delegates to EquipmentSnapshots.snapshot.
 */
class EquippedRenewalPreviewContractTest {
    @Test void currentPhysicalGearLeavesRenewalManaAndHealingParametersIntact() {
        CharacterData character = new CharacterData();
        var naked = RenewalParameters.from(PlayerModifierCalculator.calculate(character));
        EnumMap<EquipmentRules.Slot, String> equipped =
                new EnumMap<>(EquipmentRules.Slot.class);
        equipped.put(EquipmentRules.Slot.MAIN_HAND, "minecraft:netherite_sword");
        equipped.put(EquipmentRules.Slot.OFF_HAND, "minecraft:shield");
        equipped.put(EquipmentRules.Slot.CHEST, "minecraft:netherite_chestplate");
        equipped.put(EquipmentRules.Slot.HEAD, "minecraft:diamond_helmet");

        var modifiers = EquipmentRules.aggregate(equipped);
        assertFalse(modifiers.isEmpty());
        var equippedSnapshot = PlayerModifierCalculator.calculate(character, modifiers);
        var armored = RenewalParameters.from(equippedSnapshot);
        assertEquals(naked, armored);
    }

    @Test void temporaryMagicDeltaIsClampedOnceAndReadByRenewal() {
        CharacterData character = new CharacterData();
        var neutral = PlayerModifierCalculator.calculate(character);
        var boosted = PlayerModifierCalculator.calculate(character,
                java.util.Map.of(
                        com.eretenkodaniil.mkm.character.attribute.PlayerModifierId.MAX_MANA, 20D,
                        com.eretenkodaniil.mkm.character.attribute.PlayerModifierId.HEALING_POWER, 0.2D));
        var normal = RenewalParameters.from(neutral);
        var increased = RenewalParameters.from(boosted);
        assertTrue(increased.maximumMana() >= normal.maximumMana());
        assertTrue(increased.healing() >= normal.healing());
    }
}
