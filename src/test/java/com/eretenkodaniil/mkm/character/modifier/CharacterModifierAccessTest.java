package com.eretenkodaniil.mkm.character.modifier;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.AllocationRules;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterRewardRules;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class CharacterModifierAccessTest {
    @Test
    void consumersShareExactExistingCalculatorWithoutSecondScaling() {
        CharacterData initial = new CharacterData().withExperience(6000);
        CharacterData allocated = AllocationRules.tryAllocate(initial,
                new int[]{4,3,3,4,3,3,0});
        assertNotNull(allocated);
        PlayerModifierSnapshot raw = CharacterModifierAccess.base(allocated);
        assertEquals(PlayerModifierCalculator.calculate(allocated).all(), raw.all());
        assertTrue(raw.get(PlayerModifierId.PHYSICAL_POWER) > 1);
        assertTrue(raw.get(PlayerModifierId.MELEE_DAMAGE) > 1);
        assertTrue(raw.get(PlayerModifierId.ATTACK_SPEED) > 1);
        assertTrue(raw.get(PlayerModifierId.PHYSICAL_RESISTANCE) > 0);
        assertTrue(raw.get(PlayerModifierId.MAX_STAMINA) > 100);
        assertTrue(raw.get(PlayerModifierId.STAMINA_REGENERATION) > 10);
        assertTrue(raw.get(PlayerModifierId.STAMINA_COST) < 1);
        assertTrue(raw.get(PlayerModifierId.DODGE_COST) < 1);
        assertTrue(raw.get(PlayerModifierId.MAGIC_POWER) > 1);
        assertTrue(raw.get(PlayerModifierId.MAX_MANA) > 100);
        assertTrue(raw.get(PlayerModifierId.MANA_REGENERATION) > 5);
        assertTrue(raw.get(PlayerModifierId.HEALING_POWER) > 1);
        assertTrue(raw.get(PlayerModifierId.HEALING_RECEIVED) > 1);
        assertTrue(raw.get(PlayerModifierId.MANA_COST) < 1);
        assertTrue(raw.get(PlayerModifierId.STAMINA_REGEN_DELAY) < 1.5);
    }

    @Test
    void stagedAttributesKeepSameEquipmentDeltasAndSingleFinalClamp() {
        CharacterData original = new CharacterData().withExperience(100);
        CharacterData preview = AllocationRules.tryAllocate(original,
                new int[]{1,0,0,1,0,0,0});
        assertNotNull(preview);
        var equipped = Map.of(
                PlayerModifierId.MELEE_DAMAGE, 0.04D,
                PlayerModifierId.PHYSICAL_RESISTANCE, 0.02D,
                PlayerModifierId.BLOCK_STABILITY, 0.25D,
                PlayerModifierId.STAMINA_COST, -0.05D);
        var current = CharacterModifierAccess.withContributions(original, equipped);
        var projected = CharacterModifierAccess.preview(preview, equipped);
        assertEquals(PlayerModifierCalculator.calculate(original, equipped).all(), current.all());
        assertEquals(PlayerModifierCalculator.calculate(preview, equipped).all(), projected.all());
        assertTrue(projected.get(PlayerModifierId.MELEE_DAMAGE) > current.get(PlayerModifierId.MELEE_DAMAGE));
        assertTrue(projected.get(PlayerModifierId.STAMINA_COST) < current.get(PlayerModifierId.STAMINA_COST));
        assertEquals(current.get(PlayerModifierId.BLOCK_STABILITY),
                projected.get(PlayerModifierId.BLOCK_STABILITY), 1.0e-9);
        // Character-only preview is NOT equivalent to a real equipped snapshot.
        assertNotEquals(CharacterModifierAccess.base(preview).all(), projected.all());
        assertThrows(UnsupportedOperationException.class,
                () -> projected.all().put(PlayerModifierId.MAX_STAMINA, 99999.0D));
    }

    @Test
    void hostileOrOverflowContributionsCannotBypassDefinitions() {
        CharacterData player = new CharacterData();
        assertThrows(IllegalArgumentException.class, () -> CharacterModifierAccess.withContributions(
                player, Map.of(PlayerModifierId.MELEE_DAMAGE, Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> CharacterModifierAccess.withContributions(
                player, Map.of(PlayerModifierId.MELEE_DAMAGE, Double.POSITIVE_INFINITY)));
        var capped = CharacterModifierAccess.withContributions(
                player, Map.of(PlayerModifierId.MAX_STAMINA, 1.0e20));
        assertEquals(ModifierDefinitions.get(PlayerModifierId.MAX_STAMINA).clamp(1.0e20 + 100),
                capped.get(PlayerModifierId.MAX_STAMINA), 1.0e-6);
    }

    @Test
    void QuestReceiptsDoNotAffectDerivedCombatOrMagicSnapshot() {
        CharacterData before = new CharacterData().withExperience(100);
        CharacterData after = CharacterRewardRules.grantOnce(
                before, "mkm:warden/complete", 100).character();
        assertEquals(CharacterModifierAccess.base(before).all(),
                CharacterModifierAccess.base(after).all());
        assertEquals(200, after.experience());
    }
}
