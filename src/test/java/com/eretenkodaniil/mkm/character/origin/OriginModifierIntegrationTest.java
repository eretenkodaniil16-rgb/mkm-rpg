package com.eretenkodaniil.mkm.character.origin;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class OriginModifierIntegrationTest {
    @Test void allFourBuildsHaveMeaningfullyDifferentBoundedServerSnapshots() {
        CharacterData base = new CharacterData();
        Map<PlayerModifierId, Double> vanillaItems = EquipmentRules.aggregate(Map.of(
                EquipmentRules.Slot.MAIN_HAND, "minecraft:iron_sword",
                EquipmentRules.Slot.OFF_HAND, "minecraft:shield",
                EquipmentRules.Slot.CHEST, "minecraft:iron_chestplate"));
        assertEquals(PlayerModifierCalculator.calculate(base, vanillaItems).all(),
                OriginModifierRules.snapshot(base, vanillaItems, new CharacterOriginData()).all());

        for (var race : OriginCatalog.racesAll().values()) {
            for (var clazz : OriginCatalog.classesAll().values()) {
                var state = CharacterOriginRules.select(
                        new CharacterOriginData(), race.id(), clazz.id(), 0).data();
                var bonuses = OriginModifierRules.combined(vanillaItems, state);
                var actual = OriginModifierRules.snapshot(base, vanillaItems, state);
                var exactlyOnce = PlayerModifierCalculator.calculate(base, bonuses);
                assertEquals(exactlyOnce.all(), actual.all());
                assertTrue(actual.get(PlayerModifierId.MAX_HEALTH) >= 20);
                assertTrue(actual.get(PlayerModifierId.MAX_MANA) >= 100);
                assertTrue(actual.get(PlayerModifierId.STAMINA_REGENERATION) >= 10);
                assertTrue(actual.get(PlayerModifierId.MELEE_DAMAGE) >= 1);
                assertTrue(actual.get(PlayerModifierId.BLOCK_STABILITY) >= 1.25);
                if (race.id().equals(OriginCatalog.DWARF.id())) {
                    assertEquals(104, actual.get(PlayerModifierId.MAX_STAMINA));
                } else {
                    assertEquals(100, actual.get(PlayerModifierId.MAX_STAMINA));
                }
                if (clazz.id().equals(OriginCatalog.MAGE.id())) {
                    assertEquals(106, actual.get(PlayerModifierId.MAX_MANA));
                } else {
                    assertEquals(100, actual.get(PlayerModifierId.MAX_MANA));
                }
                assertTrue(actual.get(PlayerModifierId.MELEE_DAMAGE) <= 2.5);
            }
        }
    }

    @Test void equipmentBonusesAreAppliedOnceAndDoNotModifyOriginalInventoryData() {
        var eq = EquipmentRules.aggregate(Map.of(EquipmentRules.Slot.MAIN_HAND, "minecraft:iron_sword"));
        var before = Map.copyOf(eq);
        CharacterOriginData warrior = CharacterOriginRules.select(new CharacterOriginData(),
                "mkm:human", "mkm:warrior", 0).data();
        double baseline = PlayerModifierCalculator.calculate(new CharacterData(), eq)
                .get(PlayerModifierId.MELEE_DAMAGE);
        double actual = OriginModifierRules.snapshot(new CharacterData(), eq, warrior)
                .get(PlayerModifierId.MELEE_DAMAGE);
        assertEquals(baseline + 0.035D, actual, 1e-9);
        assertEquals(before, eq);
        assertThrows(UnsupportedOperationException.class,
                () -> OriginModifierRules.combined(eq, warrior).put(PlayerModifierId.MAX_MANA, 1000D));
        assertThrows(IllegalArgumentException.class, () -> OriginModifierRules.snapshot(
                new CharacterData(), Map.of(PlayerModifierId.MELEE_DAMAGE, Double.NaN), warrior));
    }

    @Test void unrecognizedPersistedProfileCannotGrantAnyBonus() {
        CharacterOriginData future = new CharacterOriginData(2, 1, "mkm:dwarf", "mkm:warrior");
        var same = OriginModifierRules.snapshot(new CharacterData(), Map.of(), future);
        assertEquals(PlayerModifierCalculator.calculate(new CharacterData()).all(), same.all());
        CharacterOriginData unknown = new CharacterOriginData(1, 1, "mkm:alien", "mkm:unknown");
        assertEquals(PlayerModifierCalculator.calculate(new CharacterData()).all(),
                OriginModifierRules.snapshot(new CharacterData(), Map.of(), unknown).all());
    }
}
