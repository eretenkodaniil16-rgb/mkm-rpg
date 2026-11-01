package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.equipment.PhysicalDamageType;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PhysicalMitigationRulesTest {
    private static PlayerModifierSnapshot stats(Map<PlayerModifierId, Double> deltas) {
        return PlayerModifierCalculator.calculate(new CharacterData(), deltas);
    }

    @Test
    void unarmoredNeutralCombatPreservesDamage() {
        assertEquals(10.0F, PhysicalMitigationRules.apply(10F, stats(Map.of()),
                stats(Map.of()), PhysicalDamageType.SLASHING), 0.0001F);
    }

    @Test
    void onlySelectedTypeResistanceApplies() {
        var defender = stats(Map.of(
                PlayerModifierId.PHYSICAL_RESISTANCE, 0.1D,
                PlayerModifierId.SLASHING_RESISTANCE, 0.2D));
        assertEquals(7.0F, PhysicalMitigationRules.apply(10F, stats(Map.of()), defender,
                PhysicalDamageType.SLASHING), 0.0001F);
        assertEquals(9.0F, PhysicalMitigationRules.apply(10F, stats(Map.of()), defender,
                PhysicalDamageType.PIERCING), 0.0001F);
    }

    @Test
    void penetrationBypassesCustomProtectionButNotVulnerability() {
        var attacker = stats(Map.of(PlayerModifierId.ARMOR_PENETRATION, 0.5D));
        var target = stats(Map.of(PlayerModifierId.PHYSICAL_RESISTANCE, 0.4D));
        assertEquals(8F, PhysicalMitigationRules.apply(10F, attacker, target,
                PhysicalDamageType.BLUNT), 0.0001F);
        var vulnerable = stats(Map.of(PlayerModifierId.PHYSICAL_RESISTANCE, -0.2D));
        assertEquals(12F, PhysicalMitigationRules.apply(10F, attacker, vulnerable,
                PhysicalDamageType.BLUNT), 0.0001F);
    }

    @Test
    void invalidAndBoundaryDamageIsSafe() {
        var neutral = stats(Map.of());
        assertEquals(0F, PhysicalMitigationRules.apply(Float.NaN, neutral, neutral,
                PhysicalDamageType.BLUNT));
        assertEquals(0F, PhysicalMitigationRules.apply(-10F, neutral, neutral,
                PhysicalDamageType.BLUNT));
    }
}
