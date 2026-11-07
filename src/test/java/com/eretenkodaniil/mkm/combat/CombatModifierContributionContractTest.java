package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class CombatModifierContributionContractTest {
    private static CombatModifierContributions.Contribution bonus(
            CombatModifierContributions.Kind kind, String id, PlayerModifierId mod, double value) {
        return new CombatModifierContributions.Contribution(kind, id, Map.of(mod, value));
    }

    @Test void equipmentWithoutFeaturesMatchesOriginalSingleCalculatorExactly() {
        var equipped = Map.of(
                EquipmentRules.Slot.MAIN_HAND, "minecraft:iron_sword",
                EquipmentRules.Slot.OFF_HAND, "minecraft:shield",
                EquipmentRules.Slot.CHEST, "minecraft:iron_chestplate");
        CharacterData player = new CharacterData();
        var before = PlayerModifierCalculator.calculate(player, EquipmentRules.aggregate(equipped));
        var after = PlayerCombatModifierAccess.snapshot(player, equipped, List.of());
        assertEquals(before.all(), after.all());
    }

    @Test void raceClassAndMultipleClassFeaturesAreMergedBeforeFinalClamp() {
        var equipped = Map.of(EquipmentRules.Slot.MAIN_HAND, "minecraft:iron_sword");
        var skills = List.of(
                bonus(CombatModifierContributions.Kind.RACE, "mkm:race_human",
                        PlayerModifierId.MELEE_DAMAGE, .05D),
                bonus(CombatModifierContributions.Kind.CLASS, "mkm:warrior_training",
                        PlayerModifierId.MELEE_DAMAGE, .10D),
                bonus(CombatModifierContributions.Kind.CLASS, "mkm:warrior_weapon_mastery",
                        PlayerModifierId.MELEE_DAMAGE, .08D),
                bonus(CombatModifierContributions.Kind.STATUS, "mkm:temporary_focus",
                        PlayerModifierId.STAGGER_POWER, .25D));
        var merged = CombatModifierContributions.combine(equipped, skills);
        assertEquals(.04D + .05D + .10D + .08D,
                merged.get(PlayerModifierId.MELEE_DAMAGE), 1e-8);
        var throughBridge = PlayerCombatModifierAccess.snapshot(new CharacterData(), equipped, skills);
        var directOriginal = PlayerModifierCalculator.calculate(new CharacterData(), merged);
        assertEquals(directOriginal.all(), throughBridge.all());
        assertEquals(1.27D, throughBridge.get(PlayerModifierId.MELEE_DAMAGE), 1e-6);
    }

    @Test void repeatedFeatureReceiptsAndInvalidStatCannotDoubleApply() {
        var one = bonus(CombatModifierContributions.Kind.CLASS, "mkm:warrior_training",
                PlayerModifierId.MELEE_DAMAGE, .1D);
        assertThrows(IllegalArgumentException.class, () ->
                CombatModifierContributions.combine(Map.of(), List.of(one, one)));
        assertThrows(IllegalArgumentException.class, () ->
                new CombatModifierContributions.Contribution(
                        CombatModifierContributions.Kind.RACE,
                        "mkm:bad_race", Map.of(PlayerModifierId.STAMINA_COST, Double.NaN)));
        assertThrows(IllegalArgumentException.class, () ->
                CombatModifierContributions.combine(Map.of(), List.of(
                        bonus(CombatModifierContributions.Kind.CLASS, "mkm:a",
                                PlayerModifierId.STAGGER_POWER, Double.MAX_VALUE),
                        bonus(CombatModifierContributions.Kind.CLASS, "mkm:b",
                                PlayerModifierId.STAGGER_POWER, Double.MAX_VALUE))));
    }

    @Test void calculatorStillOwnsFinalCapsAndReconciliation() {
        var equipment = Map.of(EquipmentRules.Slot.MAIN_HAND, "minecraft:iron_sword");
        var large = bonus(CombatModifierContributions.Kind.CLASS, "mkm:boss_strength",
                PlayerModifierId.MELEE_DAMAGE, 100D);
        var merged = CombatModifierContributions.combine(equipment, List.of(large));
        var stats = PlayerCombatModifierAccess.snapshot(new CharacterData(), equipment, List.of(large));
        assertEquals(2.5D, stats.get(PlayerModifierId.MELEE_DAMAGE), 1e-8);
        assertEquals(PlayerModifierCalculator.calculate(new CharacterData(), merged).all(), stats.all());
    }
}
