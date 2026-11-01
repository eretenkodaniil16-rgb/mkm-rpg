package com.eretenkodaniil.mkm.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.AllocationRules;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class IntegratedModifierContractTest {
    @Test
    void progressionAndEquipmentUseOneSnapshotAndPreserveTheLedger() {
        CharacterData gained = new CharacterData().withExperience(100);
        CharacterData allocated = AllocationRules.tryAllocate(
                gained, new int[] {1, 0, 0, 0, 1, 0, 0});
        assertNotNull(allocated);
        assertEquals(0, allocated.availablePoints());
        var base = PlayerModifierCalculator.calculate(gained);
        var progressed = PlayerModifierCalculator.calculate(allocated);
        var gear = EquipmentRules.aggregate(Map.of(
                EquipmentRules.Slot.MAIN_HAND, "minecraft:iron_sword",
                EquipmentRules.Slot.CHEST, "minecraft:iron_chestplate"));
        var combined = PlayerModifierCalculator.calculate(allocated, gear);
        assertTrue(progressed.get(PlayerModifierId.PHYSICAL_POWER) > base.get(PlayerModifierId.PHYSICAL_POWER));
        assertTrue(progressed.get(PlayerModifierId.MAGIC_POWER) > base.get(PlayerModifierId.MAGIC_POWER));
        assertTrue(combined.get(PlayerModifierId.MELEE_DAMAGE) > progressed.get(PlayerModifierId.MELEE_DAMAGE));
        assertTrue(combined.get(PlayerModifierId.PHYSICAL_RESISTANCE) > progressed.get(PlayerModifierId.PHYSICAL_RESISTANCE));
        assertEquals(progressed.get(PlayerModifierId.MAGIC_POWER),
                combined.get(PlayerModifierId.MAGIC_POWER), 1e-9);
        assertEquals(2, allocated.spentPoints());
        assertEquals(0, gained.spentPoints());
        assertNull(AllocationRules.tryAllocate(allocated, new int[] {1, 0, 0, 0, 1, 0, 0}));
    }
}
