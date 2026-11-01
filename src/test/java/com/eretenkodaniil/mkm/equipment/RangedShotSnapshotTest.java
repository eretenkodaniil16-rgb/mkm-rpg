package com.eretenkodaniil.mkm.equipment;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class RangedShotSnapshotTest {
    @Test
    void valuesAreCapturedAtLaunchNotReadFromCurrentWeaponAtImpact() {
        var atLaunch = PlayerModifierCalculator.calculate(new CharacterData(), Map.of(
                PlayerModifierId.PHYSICAL_POWER, 0.5D,
                PlayerModifierId.RANGED_DAMAGE, 0.25D,
                PlayerModifierId.ARMOR_PENETRATION, 0.2D));
        var shot = RangedShotSnapshot.capture("minecraft:bow", atLaunch, 100L).orElseThrow();
        float original = shot.scaleVanillaProjectile(10F);
        var afterSwap = PlayerModifierCalculator.calculate(new CharacterData(), Map.of(
                PlayerModifierId.PHYSICAL_POWER, 1.2D));
        assertTrue(RangedShotSnapshot.capture("minecraft:crossbow", afterSwap, 101L)
                .orElseThrow().scaleVanillaProjectile(10F) != original);
        assertEquals(18.75F, shot.scaleVanillaProjectile(10F), 0.0001F);
        assertEquals(0.2D, shot.armorPenetration(), 1e-9);
        assertEquals(100L, shot.firedGameTick());
    }

    @Test
    void compatibleVanillaWeaponsOnlyAreCaptured() {
        var stats = PlayerModifierCalculator.calculate(new CharacterData());
        assertTrue(RangedShotSnapshot.capture("minecraft:trident", stats, 50L).isPresent());
        assertTrue(RangedShotSnapshot.capture("minecraft:crossbow", stats, 50L).isPresent());
        assertTrue(RangedShotSnapshot.capture("minecraft:iron_sword", stats, 50L).isEmpty());
        assertTrue(RangedShotSnapshot.capture("mkm:unknown_bow", stats, 50L).isEmpty());
        assertTrue(RangedShotSnapshot.capture(null, stats, 50L).isEmpty());
    }

    @Test
    void onePassDamageAndCustomResistanceExcludeDoubleApplication() {
        var stats = PlayerModifierCalculator.calculate(new CharacterData(), Map.of(
                PlayerModifierId.PHYSICAL_POWER, 0.5D,
                PlayerModifierId.RANGED_DAMAGE, 0.25D,
                PlayerModifierId.ARMOR_PENETRATION, 0.2D));
        var defender = PlayerModifierCalculator.calculate(new CharacterData(), Map.of(
                PlayerModifierId.PHYSICAL_RESISTANCE, 0.4D));
        var shot = RangedShotSnapshot.capture("minecraft:bow", stats, 200L).orElseThrow();
        assertEquals(12.75F, shot.scaleAndMitigate(10F, defender), 0.0001F);
    }

    @Test
    void invalidSnapshotsAndDamageFailSafely() {
        assertThrows(IllegalArgumentException.class, () -> new RangedShotSnapshot(
                "minecraft:bow", PhysicalDamageType.PIERCING, Double.NaN, 1D, 0D, 10L));
        assertThrows(IllegalArgumentException.class, () -> new RangedShotSnapshot(
                "minecraft:bow", PhysicalDamageType.PIERCING, 1D, 1D, 0.9D, 10L));
        assertThrows(IllegalArgumentException.class, () -> new RangedShotSnapshot(
                "minecraft:bow", PhysicalDamageType.PIERCING, 1D, 1D, 0D, -1L));
        var shot = RangedShotSnapshot.capture(
                "minecraft:bow", PlayerModifierCalculator.calculate(new CharacterData()), 20L).orElseThrow();
        assertEquals(0F, shot.scaleVanillaProjectile(Float.NaN));
        assertEquals(0F, shot.scaleVanillaProjectile(-10F));
    }
}
