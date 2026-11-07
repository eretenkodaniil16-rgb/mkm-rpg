package com.eretenkodaniil.mkm.combat.presentation;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import org.junit.jupiter.api.Test;

/** #43: real attacking hand and independent offhand weapon presentation. */
final class WeaponPresentationResolverTest {
    private static WeaponPresentationResolver.Hands hands(
            String main, String off, AttackPresentationEvent.Arm dominant) {
        return new WeaponPresentationResolver.Hands(main, off, dominant);
    }

    private static WeaponPresentationResolver.Resolved lmb(
            String main, String off, AttackPresentationEvent.Arm dominant, long tick) {
        return WeaponPresentationResolver.resolveVanillaAttack(
                hands(main, off, dominant), AttackPresentationEvent.ActionKind.VANILLA_SWING,
                null, tick);
    }

    @Test void mainhandSwordIsActualAttackWeapon() {
        var resolved = lmb("minecraft:diamond_sword", "minecraft:shield",
                AttackPresentationEvent.Arm.RIGHT, 100L);
        var c = resolved.hands();
        assertEquals(EquipmentRules.Family.SWORD, c.mainHand().family());
        assertEquals(EquipmentRules.Family.SHIELD, c.offHand().family());
        assertEquals(EquipmentRules.Family.SWORD, c.activeItem().family());
        assertEquals("minecraft:diamond_sword", c.activeItem().id());
        assertEquals(AttackPresentationEvent.Hand.MAIN_HAND, c.actionHand());
        assertEquals(AttackPresentationEvent.Arm.RIGHT, c.attackingArm());
        assertEquals(1, resolved.event().sequenceStep());
        assertEquals("mkm:attack/sword/light", resolved.event().animationId());
        assertSame(c, resolved.event().handContext());
        assertEquals(MnaeDemoClipRouting.SWORD_DEMO,
                MnaeDemoClipRouting.demoClipFor(resolved.event()).orElseThrow());
    }

    @Test void offhandSwordCannotBecomeTheMainhandWeapon() {
        var resolved = lmb("minecraft:air", "minecraft:iron_sword",
                AttackPresentationEvent.Arm.RIGHT, 120L);
        var c = resolved.hands();
        assertEquals(EquipmentRules.Family.NONE, c.mainHand().family());
        assertTrue(c.mainHand().empty());
        assertEquals(EquipmentRules.Family.SWORD, c.offHand().family());
        assertEquals(EquipmentRules.Family.NONE, resolved.event().weaponFamily());
        assertEquals("minecraft:air", c.activeItem().id());
        assertEquals("mkm:attack/unarmed/light", resolved.event().animationId());
        assertTrue(MnaeDemoClipRouting.demoClipFor(resolved.event()).isEmpty(),
                "No independent offhand-sword grip exists; never pose it as a native punch");
        assertTrue(EquipmentRules.profile("minecraft:iron_sword",
                EquipmentRules.Slot.OFF_HAND).deltas().isEmpty(),
                "Observing an offhand sword must not grant mainhand stat bonuses");
    }

    @Test void unsupportedAxesMacePickaxeBowAndShieldNeverClaimSword() {
        var inputs = new String[] {
                "minecraft:diamond_axe", "minecraft:mace", "minecraft:iron_pickaxe",
                "minecraft:bow", "minecraft:crossbow", "minecraft:shield"
        };
        var expected = new EquipmentRules.Family[] {
                EquipmentRules.Family.AXE, EquipmentRules.Family.MACE,
                EquipmentRules.Family.NONE, EquipmentRules.Family.BOW,
                EquipmentRules.Family.CROSSBOW, EquipmentRules.Family.SHIELD
        };
        for (int i=0; i<inputs.length; i++) {
            var r = lmb(inputs[i], "minecraft:air",
                    AttackPresentationEvent.Arm.RIGHT, 150L);
            assertEquals(expected[i], r.event().weaponFamily(), inputs[i]);
            assertFalse(r.hands().activeItem().empty(), inputs[i]);
            assertNotEquals("mkm:attack/sword/light", r.event().animationId(), inputs[i]);
            assertTrue(MnaeDemoClipRouting.demoClipFor(r.event()).isEmpty(), inputs[i]);
        }
    }

    @Test void shieldInOffhandDoesNotReplaceMainhandAction() {
        var r = lmb("minecraft:air", "minecraft:shield",
                AttackPresentationEvent.Arm.RIGHT, 151L);
        assertEquals(EquipmentRules.Family.SHIELD, r.hands().offHand().family());
        assertEquals(EquipmentRules.Family.NONE, r.hands().activeItem().family());
        assertEquals(AttackPresentationEvent.Hand.MAIN_HAND, r.event().hand());
        assertEquals(AttackPresentationEvent.ActionKind.VANILLA_SWING, r.event().actionKind());
    }

    @Test void physicalDominantArmAndActionHandAreDifferentConcepts() {
        var leftMain = lmb("minecraft:iron_sword", "minecraft:air",
                AttackPresentationEvent.Arm.LEFT, 160L);
        assertEquals(AttackPresentationEvent.Hand.MAIN_HAND, leftMain.event().hand());
        assertEquals(AttackPresentationEvent.Arm.LEFT, leftMain.event().leadingArm());
        assertEquals(AttackPresentationEvent.Arm.LEFT, leftMain.hands().attackingArm());

        // Explicit OFF_HAND is a read-only context for use-item animations.
        // There is NO synthetic offhand LMB attack or damage.
        var explicitOff = WeaponPresentationResolver.inspect(
                hands("minecraft:air", "minecraft:iron_sword",
                        AttackPresentationEvent.Arm.LEFT),
                AttackPresentationEvent.Hand.OFF_HAND);
        assertEquals(AttackPresentationEvent.Arm.RIGHT, explicitOff.attackingArm());
        assertEquals(EquipmentRules.Family.SWORD, explicitOff.activeItem().family());
        assertEquals(AttackPresentationEvent.Hand.OFF_HAND, explicitOff.actionHand());
        assertEquals(AttackPresentationEvent.Arm.LEFT, explicitOff.preferredMainArm());
    }

    @Test void sequenceStagesKeepCurrentMainhandAndOriginalOutcome() {
        var hands = hands("minecraft:iron_sword", "minecraft:iron_axe",
                AttackPresentationEvent.Arm.LEFT);
        var stage1 = WeaponPresentationResolver.resolveVanillaAttack(
                hands, AttackPresentationEvent.ActionKind.VANILLA_SWING, null, 100L);
        var stage2 = WeaponPresentationResolver.resolveVanillaAttack(
                hands, AttackPresentationEvent.ActionKind.VANILLA_SWING,
                stage1.selection().state(), 112L);
        var stage3 = WeaponPresentationResolver.resolveVanillaAttack(
                hands, AttackPresentationEvent.ActionKind.VANILLA_SWING,
                stage2.selection().state(), 124L);
        assertEquals(1, stage1.event().sequenceStep());
        assertEquals(2, stage2.event().sequenceStep());
        assertEquals(3, stage3.event().sequenceStep());
        for (var r : new WeaponPresentationResolver.Resolved[] {stage1, stage2, stage3}) {
            assertEquals(EquipmentRules.Family.SWORD, r.event().weaponFamily());
            assertEquals(EquipmentRules.Family.AXE, r.event().handContext().offHand().family());
            assertEquals(AttackPresentationEvent.Outcome.LOCAL_PREDICTED,
                    r.event().outcome());
        }
        assertSame(stage3.event().handContext(), stage3.event()
                .withOutcome(AttackPresentationEvent.Outcome.COMPLETED).handContext());
        assertEquals(AttackPresentationEvent.Outcome.LOCAL_PREDICTED, stage3.event().outcome());
        var swapped = WeaponPresentationResolver.resolveVanillaAttack(
                hands("minecraft:iron_axe", "minecraft:iron_sword", AttackPresentationEvent.Arm.LEFT),
                AttackPresentationEvent.ActionKind.VANILLA_SWING,
                stage3.selection().state(), 130L);
        assertEquals(1, swapped.event().sequenceStep());
        assertEquals(EquipmentRules.Family.AXE, swapped.event().weaponFamily());
    }

    @Test void shiftHeavyAndInvalidActionNeverBecomeUnapprovedExtraHit() {
        var hands = hands("minecraft:iron_axe", "minecraft:iron_sword",
                AttackPresentationEvent.Arm.RIGHT);
        var heavy = WeaponPresentationResolver.resolveVanillaAttack(
                hands, AttackPresentationEvent.ActionKind.HEAVY, null, 100L);
        assertEquals(AttackPresentationEvent.ActionKind.HEAVY, heavy.event().actionKind());
        assertEquals(EquipmentRules.Family.AXE, heavy.event().weaponFamily());
        assertTrue(MnaeDemoClipRouting.demoClipFor(heavy.event()).isEmpty());
        assertThrows(IllegalArgumentException.class, () ->
                WeaponPresentationResolver.resolveVanillaAttack(hands,
                        AttackPresentationEvent.ActionKind.SPECIAL_COMBO, null, 100L));
        assertThrows(NullPointerException.class, () ->
                WeaponPresentationResolver.inspect(hands, null));
    }

    @Test void presentationFamilyReusesEquipmentRulesNoOffhandDamageBonuses() {
        var ctx = WeaponPresentationResolver.inspect(
                hands("minecraft:iron_axe", "minecraft:netherite_sword",
                        AttackPresentationEvent.Arm.RIGHT),
                AttackPresentationEvent.Hand.OFF_HAND);
        assertEquals(EquipmentRules.profile("minecraft:netherite_sword",
                EquipmentRules.Slot.MAIN_HAND).family(), ctx.offHand().family());
        assertEquals(EquipmentRules.Family.SWORD, ctx.activeItem().family());
        assertNotEquals(EquipmentRules.profile("minecraft:netherite_sword",
                EquipmentRules.Slot.OFF_HAND).family(), ctx.offHand().family());
    }
}
