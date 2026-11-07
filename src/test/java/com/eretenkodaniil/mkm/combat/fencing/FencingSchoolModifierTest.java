package com.eretenkodaniil.mkm.combat.fencing;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.combat.CombatModifierContributions;
import com.eretenkodaniil.mkm.combat.CombatRules;
import com.eretenkodaniil.mkm.combat.CombatState;
import com.eretenkodaniil.mkm.combat.GuardCostExperiment;
import com.eretenkodaniil.mkm.combat.GuardRules;
import com.eretenkodaniil.mkm.combat.PlayerCombatModifierAccess;
import com.eretenkodaniil.mkm.combat.PoiseRules;
import com.eretenkodaniil.mkm.combat.PoiseState;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Schools only compose existing player stats. They never call attack()/hurt(),
 * change hitboxes or override vanilla mining and i-frames.
 */
final class FencingSchoolModifierTest {
    private static final CharacterData CHARACTER = new CharacterData();
    private static final Map<EquipmentRules.Slot, String> SWORD =
            Map.of(EquipmentRules.Slot.MAIN_HAND, "minecraft:iron_sword");
    private static final Map<EquipmentRules.Slot, String> SWORD_SHIELD =
            Map.of(EquipmentRules.Slot.MAIN_HAND, "minecraft:iron_sword",
                    EquipmentRules.Slot.OFF_HAND, "minecraft:shield");

    @Test void noSchoolIsIdenticalToPreviousEquipmentAndCharacterStats() {
        var old = PlayerModifierCalculator.calculate(CHARACTER, EquipmentRules.aggregate(SWORD));
        var unchanged = PlayerCombatModifierAccess.snapshot(CHARACTER, SWORD, List.of());
        assertEquals(old.all(), unchanged.all());
        assertTrue(FencingSchool.NONE.contribution().isEmpty());
    }

    @Test void germanPressureUsesExistingMeleePoiseAndCostCalculators() {
        var baseline = PlayerCombatModifierAccess.snapshot(CHARACTER, SWORD, List.of());
        var german = PlayerCombatModifierAccess.snapshot(CHARACTER, SWORD,
                FencingSchool.LIECHTENAUER.contribution().stream().toList());
        assertEquals(baseline.get(PlayerModifierId.MELEE_DAMAGE) + .04D,
                german.get(PlayerModifierId.MELEE_DAMAGE), 1e-8);
        assertEquals(baseline.get(PlayerModifierId.STAGGER_POWER) + .15D,
                german.get(PlayerModifierId.STAGGER_POWER), 1e-8);
        assertEquals(baseline.get(PlayerModifierId.STAMINA_COST) + .12D,
                german.get(PlayerModifierId.STAMINA_COST), 1e-8);
        double baseDamage = CombatRules.scaleIncomingMelee(10F,
                baseline.get(PlayerModifierId.PHYSICAL_POWER),
                baseline.get(PlayerModifierId.MELEE_DAMAGE), false);
        double germanDamage = CombatRules.scaleIncomingMelee(10F,
                german.get(PlayerModifierId.PHYSICAL_POWER),
                german.get(PlayerModifierId.MELEE_DAMAGE), false);
        assertTrue(germanDamage > baseDamage);
        var poiseNeutral = PoiseRules.onHit(new PoiseState(), 100, 10F,
                baseline.get(PlayerModifierId.STAGGER_POWER), false);
        var poiseGerman = PoiseRules.onHit(new PoiseState(), 100, 10F,
                german.get(PlayerModifierId.STAGGER_POWER), false);
        assertTrue(poiseGerman.state().buildup() > poiseNeutral.state().buildup());
    }

    @Test void italianGuardAffectsOnlyOriginalConfirmedShieldFormula() {
        var baseline = PlayerCombatModifierAccess.snapshot(CHARACTER, SWORD_SHIELD, List.of());
        var fiore = PlayerCombatModifierAccess.snapshot(CHARACTER, SWORD_SHIELD,
                FencingSchool.FIORE.contribution().stream().toList());
        assertEquals(baseline.get(PlayerModifierId.BLOCK_STABILITY) + .20D,
                fiore.get(PlayerModifierId.BLOCK_STABILITY), 1e-8);
        assertEquals(baseline.get(PlayerModifierId.STAGGER_RESISTANCE) + .10D,
                fiore.get(PlayerModifierId.STAGGER_RESISTANCE), 1e-8);
        assertTrue(fiore.get(PlayerModifierId.MELEE_DAMAGE)
                < baseline.get(PlayerModifierId.MELEE_DAMAGE));
        for (double factor : new double[]{
                GuardCostExperiment.CONTROL, GuardCostExperiment.EXPERIMENTAL}) {
            float control = GuardRules.block(new CombatState(), 100L, 10F,
                    baseline.get(PlayerModifierId.BLOCK_STABILITY),
                    baseline.get(PlayerModifierId.STAMINA_COST),
                    100F, 10F, 30L, factor).spent();
            float guarded = GuardRules.block(new CombatState(), 100L, 10F,
                    fiore.get(PlayerModifierId.BLOCK_STABILITY),
                    fiore.get(PlayerModifierId.STAMINA_COST),
                    100F, 10F, 30L, factor).spent();
            assertTrue(guarded < control);
            // An actual offhand shield contributes +0.25 BLOCK_STABILITY through
            // EquipmentRules, so baseline cost is 9/1.25, not 9.
            assertEquals((float) (9D * factor
                    / baseline.get(PlayerModifierId.BLOCK_STABILITY)), control, 1e-4);
        }
    }

    @Test void spanishPrecisionDoesNotTouchVanillaReachOrMining() {
        var baseline = PlayerCombatModifierAccess.snapshot(CHARACTER, SWORD, List.of());
        var spanish = PlayerCombatModifierAccess.snapshot(CHARACTER, SWORD,
                FencingSchool.DESTREZA.contribution().stream().toList());
        assertEquals(baseline.get(PlayerModifierId.ARMOR_PENETRATION) + .06D,
                spanish.get(PlayerModifierId.ARMOR_PENETRATION), 1e-8);
        assertEquals(baseline.get(PlayerModifierId.DODGE_COST) - .10D,
                spanish.get(PlayerModifierId.DODGE_COST), 1e-8);
        assertEquals(baseline.get(PlayerModifierId.STAGGER_POWER) - .10D,
                spanish.get(PlayerModifierId.STAGGER_POWER), 1e-8);
        assertEquals(baseline.get(PlayerModifierId.MOVEMENT_SPEED),
                spanish.get(PlayerModifierId.MOVEMENT_SPEED), 1e-8);
        assertEquals(baseline.get(PlayerModifierId.ATTACK_SPEED),
                spanish.get(PlayerModifierId.ATTACK_SPEED), 1e-8);
    }

    @Test void schoolsAreMutuallyExclusiveAndCannotDoubleGrantSameStatus() {
        var one = FencingSchool.FIORE.contribution().orElseThrow();
        assertThrows(IllegalArgumentException.class, () ->
                CombatModifierContributions.combine(SWORD, List.of(one, one)));
        for (FencingSchool school : new FencingSchool[]{
                FencingSchool.LIECHTENAUER, FencingSchool.FIORE, FencingSchool.DESTREZA}) {
            var direct = PlayerModifierCalculator.calculate(
                    CHARACTER, CombatModifierContributions.combine(
                            SWORD, school.contribution().stream().toList()));
            var viaBridge = PlayerCombatModifierAccess.snapshot(
                    CHARACTER, SWORD, school.contribution().stream().toList());
            assertEquals(direct.all(), viaBridge.all());
        }
    }
}
