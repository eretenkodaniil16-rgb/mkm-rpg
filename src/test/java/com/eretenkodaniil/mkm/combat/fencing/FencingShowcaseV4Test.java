package com.eretenkodaniil.mkm.combat.fencing;

import static org.junit.jupiter.api.Assertions.*;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginData;
import com.eretenkodaniil.mkm.character.origin.OriginModifierRules;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** QA5 CharacterData v4 + race/class + fencing must have ONE final clamp. */
final class FencingShowcaseV4Test {
    @Test void idleSwordRequirementsAndWarmupKeepThreeSchoolsExclusive() {
        var empty = new FencingSchoolState();
        var forbidden = FencingSchoolRules.select(empty, FencingSchool.FIORE,
                EquipmentRules.Family.AXE, true, 100);
        assertEquals(FencingSchoolRules.ResultCode.REQUIRES_MAINHAND_SWORD, forbidden.code());
        var first = FencingSchoolRules.select(empty, FencingSchool.LIECHTENAUER,
                EquipmentRules.Family.SWORD, true, 100);
        assertTrue(first.applied());
        assertTrue(FencingSchoolRules.effectiveSchool(first.state(),
                EquipmentRules.Family.SWORD, 119).isEmpty());
        assertEquals(FencingSchool.LIECHTENAUER,
                FencingSchoolRules.effectiveSchool(first.state(),
                        EquipmentRules.Family.SWORD, 120).orElseThrow());
        assertTrue(FencingSchoolRules.effectiveSchool(first.state(),
                EquipmentRules.Family.NONE, 121).isEmpty());
        assertEquals(FencingSchoolRules.ResultCode.SWITCH_COOLDOWN,
                FencingSchoolRules.select(first.state(), FencingSchool.FIORE,
                        EquipmentRules.Family.SWORD, true, 121).code());
        assertEquals(FencingSchoolRules.ResultCode.ACTION_BUSY,
                FencingSchoolRules.select(first.state(), FencingSchool.FIORE,
                        EquipmentRules.Family.SWORD, false, 180).code());
    }
    @Test void originAndEquipmentAndOneSchoolShareExistingV4Calculator() {
        CharacterData data = new CharacterData();
        assertEquals(4, data.schemaVersion());
        Map<PlayerModifierId,Double> equipment = EquipmentRules.aggregate(
                Map.of(EquipmentRules.Slot.MAIN_HAND, "minecraft:iron_sword",
                        EquipmentRules.Slot.OFF_HAND,"minecraft:shield"));
        var origin = new CharacterOriginData();
        var original = OriginModifierRules.snapshot(data, equipment, origin);
        var none = OriginModifierRules.snapshot(data,
                FencingSchoolModifierRules.combine(equipment, Map.of()), origin);
        assertEquals(original.all(), none.all());
        var defensive = OriginModifierRules.snapshot(data,
                FencingSchoolModifierRules.combine(equipment,
                        FencingSchool.FIORE.additiveDeltas()), origin);
        assertTrue(defensive.get(PlayerModifierId.BLOCK_STABILITY)
                > original.get(PlayerModifierId.BLOCK_STABILITY));
        var pressure = OriginModifierRules.snapshot(data,
                FencingSchoolModifierRules.combine(equipment,
                        FencingSchool.LIECHTENAUER.additiveDeltas()), origin);
        assertTrue(pressure.get(PlayerModifierId.STAGGER_POWER)
                > original.get(PlayerModifierId.STAGGER_POWER));
        var precise = OriginModifierRules.snapshot(data,
                FencingSchoolModifierRules.combine(equipment,
                        FencingSchool.DESTREZA.additiveDeltas()), origin);
        assertTrue(precise.get(PlayerModifierId.ARMOR_PENETRATION)
                > original.get(PlayerModifierId.ARMOR_PENETRATION));
    }
    @Test void offhandSwordCannotQualifyAndNoPoseIsAnAttack() {
        var selected = FencingSchoolRules.select(new FencingSchoolState(),
                FencingSchool.DESTREZA, EquipmentRules.Family.NONE, true, 200);
        assertFalse(selected.applied());
        for (FencingSchool school : new FencingSchool[]{
                FencingSchool.FIORE,FencingSchool.LIECHTENAUER,FencingSchool.DESTREZA}) {
            assertNotEquals("mkm:guard/neutral",school.visualGuardCue());
            assertThrows(UnsupportedOperationException.class,
                    () -> school.additiveDeltas().clear());
        }
    }
}
