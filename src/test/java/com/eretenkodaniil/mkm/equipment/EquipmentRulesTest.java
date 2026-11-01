package com.eretenkodaniil.mkm.equipment;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class EquipmentRulesTest {
    @Test
    void vanillaWeaponFamiliesHaveStableDamageCategories() {
        assertEquals(PhysicalDamageType.SLASHING,
                EquipmentRules.profile("minecraft:iron_sword", EquipmentRules.Slot.MAIN_HAND).damageType());
        assertEquals(PhysicalDamageType.PIERCING,
                EquipmentRules.profile("minecraft:trident", EquipmentRules.Slot.MAIN_HAND).damageType());
        assertEquals(PhysicalDamageType.BLUNT,
                EquipmentRules.profile("minecraft:mace", EquipmentRules.Slot.MAIN_HAND).damageType());
        assertEquals(EquipmentRules.Family.AXE,
                EquipmentRules.profile("minecraft:diamond_axe", EquipmentRules.Slot.MAIN_HAND).family());
    }

    @Test
    void heldBowUsedAsMeleeIsBluntButLaunchedArrowIsPiercing() {
        assertEquals(PhysicalDamageType.BLUNT,
                EquipmentRules.meleePhysicalType("minecraft:bow"));
        assertEquals(PhysicalDamageType.BLUNT,
                EquipmentRules.meleePhysicalType("minecraft:crossbow"));
        assertEquals(PhysicalDamageType.PIERCING,
                EquipmentRules.profile("minecraft:bow", EquipmentRules.Slot.MAIN_HAND).damageType());
        assertEquals(PhysicalDamageType.PIERCING,
                EquipmentRules.meleePhysicalType("minecraft:trident"));
        assertEquals(PhysicalDamageType.SLASHING,
                EquipmentRules.meleePhysicalType("minecraft:diamond_sword"));
    }

    @Test
    void tiersAreMaterialGradesNotMutableSaveFields() {
        var iron = EquipmentRules.profile("minecraft:iron_sword", EquipmentRules.Slot.MAIN_HAND);
        var diamond = EquipmentRules.profile("minecraft:diamond_sword", EquipmentRules.Slot.MAIN_HAND);
        var netherite = EquipmentRules.profile("minecraft:netherite_sword", EquipmentRules.Slot.MAIN_HAND);
        assertEquals(EquipmentRules.Quality.STANDARD, iron.quality());
        assertEquals(EquipmentRules.Quality.REFINED, diamond.quality());
        assertEquals(EquipmentRules.Quality.MASTERWORK, netherite.quality());
        assertTrue(diamond.deltas().get(PlayerModifierId.MELEE_DAMAGE)
                > iron.deltas().get(PlayerModifierId.MELEE_DAMAGE));
    }

    @Test
    void offSlotAndUnknownItemsGiveNoCombatBonuses() {
        assertTrue(EquipmentRules.profile("minecraft:diamond_sword",
                EquipmentRules.Slot.CHEST).deltas().isEmpty());
        assertTrue(EquipmentRules.profile("othermod:diamond_sword",
                EquipmentRules.Slot.MAIN_HAND).deltas().isEmpty());
        assertTrue(EquipmentRules.profile("minecraft:codex",
                EquipmentRules.Slot.MAIN_HAND).deltas().isEmpty());
    }

    @Test
    void equippedArmorAddsOnceAndInventoryDoesNotCount() {
        var base = EquipmentRules.aggregate(Map.of());
        assertTrue(base.isEmpty());
        var gear = EquipmentRules.aggregate(Map.of(
                EquipmentRules.Slot.MAIN_HAND, "minecraft:iron_sword",
                EquipmentRules.Slot.CHEST, "minecraft:iron_chestplate",
                EquipmentRules.Slot.HEAD, "minecraft:iron_helmet"));
        assertEquals(0.008D, gear.get(PlayerModifierId.PHYSICAL_RESISTANCE), 1e-9);
        assertEquals(0.04D, gear.get(PlayerModifierId.MELEE_DAMAGE), 1e-9);
    }

    @Test
    void twoShieldsDoNotStackBlockBonus() {
        var gear = EquipmentRules.aggregate(Map.of(
                EquipmentRules.Slot.MAIN_HAND, "minecraft:shield",
                EquipmentRules.Slot.OFF_HAND, "minecraft:shield"));
        assertEquals(0.25D, gear.get(PlayerModifierId.BLOCK_STABILITY), 1e-9);
    }
}
