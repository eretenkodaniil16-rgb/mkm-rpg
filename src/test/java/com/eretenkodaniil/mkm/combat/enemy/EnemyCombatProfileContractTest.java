package com.eretenkodaniil.mkm.combat.enemy;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.combat.PoiseRules;
import com.eretenkodaniil.mkm.combat.PoiseState;
import com.eretenkodaniil.mkm.combat.PhysicalMitigationRules;
import com.eretenkodaniil.mkm.equipment.PhysicalDamageType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Synthetic PvE fixtures: no mob type has new behavior unless registered. */
final class EnemyCombatProfileContractTest {
    private static final PhysicalDamageType SLASH = PhysicalDamageType.SLASHING;
    private static final PhysicalDamageType PIERCE = PhysicalDamageType.PIERCING;

    @Test void ordinaryArmoredAndHighPoiseEnemiesAreDifferentOnlyWhenApproved() {
        EnemyCombatProfile ordinary = EnemyCombatProfile.NEUTRAL;
        EnemyCombatProfile armored = new EnemyCombatProfile(
                EnemyCombatProfile.AttackDelivery.DIRECT_MELEE,
                PhysicalDamageType.BLUNT, .35D, Map.of(SLASH, .10D),
                1D, 1D);
        EnemyCombatProfile resilient = new EnemyCombatProfile(
                EnemyCombatProfile.AttackDelivery.DIRECT_MELEE,
                PhysicalDamageType.BLUNT, 0D, Map.of(),
                3D, 1D);

        assertEquals(10F, ordinary.mitigateDirectPhysical(10F, 0D, SLASH), 1e-5);
        assertEquals(5.5F, armored.mitigateDirectPhysical(10F, 0D, SLASH), 1e-5);
        assertEquals(6.5F, armored.mitigateDirectPhysical(10F, 0D, PIERCE), 1e-5);
        assertEquals(10F, resilient.mitigateDirectPhysical(10F, 0D, SLASH), 1e-5);
        assertFalse(resilient.equals(ordinary));

        PoiseState normalState = new PoiseState();
        PoiseState resilientState = new PoiseState();
        boolean normalStagger = false;
        boolean resilientStagger = false;
        for (int i = 0; i < 3; i++) {
            var normal = PoiseRules.onHit(normalState, 100 + i, 10F,
                    ordinary.attackStaggerPower(), ordinary.poiseResistance(), false);
            var heavy = PoiseRules.onHit(resilientState, 100 + i, 10F,
                    resilient.attackStaggerPower(), resilient.poiseResistance(), false);
            normalState = normal.state();
            resilientState = heavy.state();
            normalStagger |= normal.staggered();
            resilientStagger |= heavy.staggered();
        }
        assertTrue(normalStagger);
        assertFalse(resilientStagger);
    }

    @Test void signedTypedResistanceIsVulnerabilityAndPenetrationCannotWorsenIt() {
        EnemyCombatProfile susceptible = new EnemyCombatProfile(
                EnemyCombatProfile.AttackDelivery.DIRECT_MELEE,
                PhysicalDamageType.BLUNT, 0D, Map.of(SLASH, -.30D),
                1D, 1D);
        assertEquals(13F, susceptible.mitigateDirectPhysical(10F, 0D, SLASH), 1e-5);
        assertEquals(13F, susceptible.mitigateDirectPhysical(10F, .75D, SLASH), 1e-5);
        EnemyCombatProfile armored = new EnemyCombatProfile(
                EnemyCombatProfile.AttackDelivery.DIRECT_MELEE,
                PhysicalDamageType.BLUNT, .4D, Map.of(),
                1D, 1D);
        assertEquals(6F, armored.mitigateDirectPhysical(10F, 0D, SLASH), 1e-5);
        assertEquals(8F, armored.mitigateDirectPhysical(10F, .5D, SLASH), 1e-5);
        assertEquals(8F, PhysicalMitigationRules.apply(10F, .5D, .4D, 0D), 1e-5);
    }

    @Test void profileIsImmutableAndRejectsUnboundedOrInvalidValues() {
        Map<PhysicalDamageType, Double> input = new HashMap<>();
        input.put(SLASH, .2D);
        EnemyCombatProfile p = new EnemyCombatProfile(
                EnemyCombatProfile.AttackDelivery.DIRECT_MELEE,
                PIERCE, .1D, input, 2D, 1.2D);
        input.put(SLASH, -.5D);
        assertEquals(.2D, p.resistanceFor(SLASH), 1e-8);
        assertThrows(UnsupportedOperationException.class, () -> p.typedResistance().put(PIERCE, .5D));
        assertThrows(IllegalArgumentException.class, () -> new EnemyCombatProfile(
                EnemyCombatProfile.AttackDelivery.DIRECT_MELEE, PIERCE,
                Double.NaN, Map.of(), 1D, 1D));
        assertThrows(IllegalArgumentException.class, () -> new EnemyCombatProfile(
                EnemyCombatProfile.AttackDelivery.DIRECT_MELEE, PIERCE,
                0D, Map.of(), 0D, 1D));
        assertThrows(IllegalArgumentException.class, () -> new EnemyCombatProfile(
                EnemyCombatProfile.AttackDelivery.DIRECT_MELEE, PIERCE,
                0D, Map.of(SLASH, .95D), 1D, 1D));
    }

    @Test void enemyWeaponAndNaturalAttackTypeUseSameEquipmentClassifier() {
        EnemyCombatProfile claw = new EnemyCombatProfile(
                EnemyCombatProfile.AttackDelivery.DIRECT_MELEE,
                PIERCE, 0D, Map.of(), 1D, 1D);
        EnemyCombatProfile ranged = new EnemyCombatProfile(
                EnemyCombatProfile.AttackDelivery.PROJECTILE,
                PIERCE, 0D, Map.of(), 1D, 1D);
        assertEquals(SLASH, ServerEnemyCombatAdapter.selectDirectMeleeType(
                "minecraft:iron_sword", java.util.Optional.of(claw)));
        assertEquals(PhysicalDamageType.BLUNT,
                ServerEnemyCombatAdapter.selectDirectMeleeType(
                        "minecraft:air", java.util.Optional.empty()));
        assertEquals(PIERCE, ServerEnemyCombatAdapter.selectDirectMeleeType(
                "minecraft:air", java.util.Optional.of(claw)));
        assertEquals(PhysicalDamageType.BLUNT,
                ServerEnemyCombatAdapter.selectDirectMeleeType(
                        "minecraft:air", java.util.Optional.of(ranged)),
                "Projectile profile must not change a direct melee hit");
        assertEquals(PhysicalDamageType.BLUNT,
                ServerEnemyCombatAdapter.selectDirectMeleeType(
                        "minecraft:bow", java.util.Optional.of(ranged)),
                "Improvised bow melee stays blunt");
    }

    @Test void optionalRegistryIsEmptyUntilExplicitlyConfiguredAndSealed() {
        EnemyCombatFeatureRegistry registry = new EnemyCombatFeatureRegistry();
        assertTrue(registry.find("minecraft:zombie").isEmpty());
        registry.register("mkm:heavy_guardian", EnemyCombatProfile.NEUTRAL);
        assertTrue(registry.find("mkm:heavy_guardian").isPresent());
        assertTrue(registry.find("minecraft:zombie").isEmpty());
        assertThrows(IllegalArgumentException.class, () ->
                registry.register("mkm:heavy_guardian", EnemyCombatProfile.NEUTRAL));
        assertThrows(IllegalArgumentException.class, () ->
                registry.register("minecraft:player", EnemyCombatProfile.NEUTRAL));
        assertThrows(IllegalArgumentException.class, () ->
                registry.register("bad id", EnemyCombatProfile.NEUTRAL));
        registry.seal();
        assertTrue(registry.isSealed());
        assertThrows(IllegalStateException.class, () ->
                registry.register("minecraft:skeleton", EnemyCombatProfile.NEUTRAL));
        assertTrue(registry.find("mkm:heavy_guardian").isPresent());
    }
}
