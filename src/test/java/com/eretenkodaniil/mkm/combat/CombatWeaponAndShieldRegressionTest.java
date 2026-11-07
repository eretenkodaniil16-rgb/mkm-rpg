package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.combat.presentation.AttackPresentationEvent;
import com.eretenkodaniil.mkm.combat.presentation.WeaponPresentationResolver;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import com.eretenkodaniil.mkm.equipment.PhysicalDamageType;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class CombatWeaponAndShieldRegressionTest {
    @Test void meleeAttackTypesAreNotRangedProjectileTypes() {
        assertEquals(PhysicalDamageType.SLASHING, EquipmentRules.meleeDamageType("minecraft:iron_sword"));
        assertEquals(PhysicalDamageType.SLASHING, EquipmentRules.meleeDamageType("minecraft:diamond_axe"));
        assertEquals(PhysicalDamageType.BLUNT, EquipmentRules.meleeDamageType("minecraft:mace"));
        assertEquals(PhysicalDamageType.PIERCING, EquipmentRules.meleeDamageType("minecraft:trident"));
        assertEquals(PhysicalDamageType.BLUNT, EquipmentRules.meleeDamageType("minecraft:bow"));
        assertEquals(PhysicalDamageType.BLUNT, EquipmentRules.meleeDamageType("minecraft:crossbow"));
        assertEquals(PhysicalDamageType.BLUNT, EquipmentRules.meleeDamageType("minecraft:iron_pickaxe"));
        assertEquals(PhysicalDamageType.BLUNT, EquipmentRules.meleeDamageType("minecraft:shield"));
        assertEquals(PhysicalDamageType.BLUNT, EquipmentRules.meleeDamageType("minecraft:air"));
    }

    @Test void heldMainhandMatchesVisualFamilyAndNeverPromotesOffhand() {
        var actual = new WeaponPresentationResolver.Hands(
                "minecraft:iron_pickaxe", "minecraft:netherite_sword",
                AttackPresentationEvent.Arm.LEFT);
        var resolved = WeaponPresentationResolver.resolveVanillaAttack(
                actual, AttackPresentationEvent.ActionKind.VANILLA_SWING, null, 100L);
        assertEquals(EquipmentRules.Family.NONE, resolved.event().weaponFamily());
        assertEquals(AttackPresentationEvent.Hand.MAIN_HAND, resolved.event().hand());
        assertEquals(AttackPresentationEvent.Arm.LEFT, resolved.event().leadingArm());
        assertEquals(PhysicalDamageType.BLUNT,
                resolved.event().meleeDamageType().orElseThrow());
        assertEquals(EquipmentRules.Family.SWORD, resolved.hands().offHand().family());
        assertTrue(resolved.event().withOutcome(
                AttackPresentationEvent.Outcome.SERVER_CONFIRMED_HIT)
                .handContext() == resolved.hands());
        // Cosmetic SERVER_CONFIRMED_HIT status does NOT create a DamageSource.
    }

    @Test void playerPvPResistanceAndFutureMobResistanceUseSamePureFormula() {
        var victim = PlayerModifierCalculator.calculate(new CharacterData(),
                Map.of(com.eretenkodaniil.mkm.character.attribute.PlayerModifierId.PHYSICAL_RESISTANCE, .3D));
        float existingPlayer = PhysicalMitigationRules.apply(
                10F, 0D, victim, PhysicalDamageType.PIERCING);
        float newShared = PhysicalMitigationRules.apply(10F, 0D, .3D, 0D);
        assertEquals(existingPlayer, newShared, 1e-6);
        assertEquals(7F, existingPlayer, 1e-6);
    }

    @Test void guardExperimentIdenticalPvEAndPvPForEqualConfirmedVanillaDamage() {
        // Combat source is irrelevant to GuardRules; only actual vanilla block
        // callback may call this. This test models equal server damage for both.
        for (float dmg : new float[]{2F, 6F, 10F, 20F}) {
            float control = GuardRules.block(new CombatState(), 100, dmg,
                    1D, 1D, 100F, 10F, 30L, GuardCostExperiment.CONTROL).spent();
            float ab = GuardRules.block(new CombatState(), 100, dmg,
                    1D, 1D, 100F, 10F, 30L, GuardCostExperiment.EXPERIMENTAL).spent();
            assertEquals(control * 1.5F, ab, 1e-5);
        }
        var receipts = new ConfirmedBlockLedger<Object, Object>();
        Object defender = new Object(), sameSource = new Object();
        receipts.begin(defender, sameSource, 100);
        assertTrue(receipts.consume(defender, sameSource, 100));
        assertFalse(receipts.consume(defender, sameSource, 100));
        receipts.begin(defender, sameSource, 100);
        assertTrue(receipts.consume(defender, sameSource, 100));
    }

    @Test void clientPredictionIsNeverAcceptedAsDamageConfirmation() {
        var prediction = new WeaponPresentationResolver.Hands(
                "minecraft:iron_sword", "minecraft:shield", AttackPresentationEvent.Arm.RIGHT);
        var preview = WeaponPresentationResolver.resolveVanillaAttack(prediction,
                AttackPresentationEvent.ActionKind.VANILLA_SWING, null, 100);
        assertEquals(AttackPresentationEvent.Outcome.LOCAL_PREDICTED,
                preview.event().outcome());
        assertFalse(HitFeedbackRules.validServerConfirmedHit(
                HitFeedbackRules.Evidence.CLIENT_PREDICTION, false, 42, 8F));
        assertFalse(HitFeedbackRules.validServerConfirmedHit(
                HitFeedbackRules.Evidence.SERVER_AFTER_DAMAGE, true, 42, 8F));
        assertFalse(HitFeedbackRules.validServerConfirmedHit(
                HitFeedbackRules.Evidence.SERVER_AFTER_DAMAGE, false, 42, 0F));
        assertTrue(HitFeedbackRules.validServerConfirmedHit(
                HitFeedbackRules.Evidence.SERVER_AFTER_DAMAGE, false, 42, 8F));
    }

    @Test void serverHitFeedbackOnlyAfterPositiveUnblockedDamage() {
        assertFalse(HitFeedbackRules.validConfirmedHit(2, 0F));
        assertFalse(HitFeedbackRules.validConfirmedHit(2, Float.NaN));
        assertFalse(HitFeedbackRules.validConfirmedHit(-1, 10F));
        assertTrue(HitFeedbackRules.validConfirmedHit(2, 10F));
    }
}
