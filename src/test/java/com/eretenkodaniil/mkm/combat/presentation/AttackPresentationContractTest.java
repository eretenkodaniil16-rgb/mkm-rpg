package com.eretenkodaniil.mkm.combat.presentation;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import com.eretenkodaniil.mkm.equipment.WeaponCombatProfiles;
import org.junit.jupiter.api.Test;

final class AttackPresentationContractTest {
    @Test void validMetadataDoesNotClaimServerHit() {
        var event = new AttackPresentationEvent(
                AttackPresentationEvent.ActionKind.VANILLA_SWING,
                EquipmentRules.Family.SWORD, AttackPresentationEvent.Hand.MAIN_HAND,
                AttackPresentationEvent.Arm.LEFT, 2, "mkm:attack/sword/light_2",
                100L, 7L, AttackPresentationEvent.Outcome.LOCAL_PREDICTED);
        assertEquals(AttackPresentationEvent.Arm.LEFT, event.leadingArm());
        assertEquals(AttackPresentationEvent.Outcome.LOCAL_PREDICTED, event.outcome());
        assertEquals(AttackPresentationEvent.Outcome.SERVER_CONFIRMED_HIT,
                event.withOutcome(AttackPresentationEvent.Outcome.SERVER_CONFIRMED_HIT).outcome());
        assertEquals(AttackPresentationEvent.Outcome.LOCAL_PREDICTED, event.outcome());
        assertThrows(IllegalArgumentException.class, () -> new AttackPresentationEvent(
                event.actionKind(), event.weaponFamily(), event.hand(), event.leadingArm(),
                5, event.animationId(), event.startGameTick(), event.sequenceId(), event.outcome()));
    }

    @Test void fastChainAndFinisherAreOnlyCosmeticSelections() {
        var first = AttackSequenceRules.next(null, EquipmentRules.Family.SWORD, 100, 3);
        var second = AttackSequenceRules.next(first.state(), EquipmentRules.Family.SWORD, 103, 3);
        var third = AttackSequenceRules.next(second.state(), EquipmentRules.Family.SWORD, 106, 3);
        assertEquals(1, first.state().step());
        assertEquals(2, second.state().step());
        assertEquals(3, third.state().step());
        assertTrue(third.finisher());
        assertFalse(AttackSequenceRules.next(third.state(), EquipmentRules.Family.SWORD, 107, 3).finisher());
    }

    @Test void pauseSwitchAndRollbackResetSequence() {
        var start = AttackSequenceRules.next(null, EquipmentRules.Family.AXE, 100, 2);
        var alternate = AttackSequenceRules.next(start.state(), EquipmentRules.Family.AXE, 107, 2);
        assertTrue(alternate.alternatePause());
        assertEquals(1, alternate.state().step());
        var rest = AttackSequenceRules.next(alternate.state(), EquipmentRules.Family.AXE, 130, 2);
        assertFalse(rest.alternatePause());
        assertEquals(1, rest.state().step());
        assertEquals(1, AttackSequenceRules.next(
                rest.state(), EquipmentRules.Family.SWORD, 131, 3).state().step());
        assertEquals(1, AttackSequenceRules.next(
                rest.state(), EquipmentRules.Family.AXE, 10, 2).state().step());
    }

    @Test void miningAndUseAlwaysTakePrecedence() {
        var base = new AttackInputPriority.Context(false, false, false, false, false);
        assertEquals(AttackInputPriority.Decision.VANILLA_SWING,
                AttackInputPriority.classify(AttackInputPriority.Intent.LMB, base));
        assertEquals(AttackInputPriority.Decision.EXISTING_HEAVY,
                AttackInputPriority.classify(AttackInputPriority.Intent.SHIFT_LMB, base));
        assertEquals(AttackInputPriority.Decision.VANILLA_MINING,
                AttackInputPriority.classify(AttackInputPriority.Intent.LMB,
                        new AttackInputPriority.Context(false, false, true, false, false)));
        assertEquals(AttackInputPriority.Decision.VANILLA_MINING,
                AttackInputPriority.classify(AttackInputPriority.Intent.SHIFT_LMB,
                        new AttackInputPriority.Context(false, false, true, false, false)));
        assertEquals(AttackInputPriority.Decision.ITEM_USE_OR_SHIELD,
                AttackInputPriority.classify(AttackInputPriority.Intent.LMB,
                        new AttackInputPriority.Context(false, false, true, true, false)));
        assertEquals(AttackInputPriority.Decision.GUI_OR_SPECTATOR,
                AttackInputPriority.classify(AttackInputPriority.Intent.LMB,
                        new AttackInputPriority.Context(true, false, false, false, false)));
        assertEquals(AttackInputPriority.Decision.NO_ACTION,
                AttackInputPriority.classify(AttackInputPriority.Intent.LMB_RELEASE, base));
    }

    @Test void noNewAttackDuringSpecialTimeline() {
        var busy = new AttackInputPriority.Context(false, false, false, false, true);
        assertEquals(AttackInputPriority.Decision.NO_ACTION,
                AttackInputPriority.classify(AttackInputPriority.Intent.LMB, busy));
        assertEquals(AttackInputPriority.Decision.EXISTING_SPECIAL,
                AttackInputPriority.classify(AttackInputPriority.Intent.R_SPECIAL, busy));
        assertEquals(AttackInputPriority.Decision.EXISTING_DODGE,
                AttackInputPriority.classify(AttackInputPriority.Intent.G_DODGE, busy));
    }

    @Test void eachWeaponHasStableMnaeIdsWithoutAlteringMinecraftAttributes() {
        var sword = WeaponCombatProfiles.forItem("minecraft:netherite_sword");
        var mace = WeaponCombatProfiles.forItem("minecraft:mace");
        var fist = WeaponCombatProfiles.forItem("minecraft:air");
        var spear = WeaponCombatProfiles.forItem("minecraft:trident");
        assertEquals(WeaponCombatProfiles.Archetype.SWORD, sword.archetype());
        assertEquals(WeaponCombatProfiles.Archetype.MACE, mace.archetype());
        assertEquals(WeaponCombatProfiles.Archetype.UNARMED, fist.archetype());
        assertEquals(WeaponCombatProfiles.Archetype.SPEAR, spear.archetype());
        assertTrue(fist.presentationSpeed() > sword.presentationSpeed());
        assertTrue(mace.proposedStaminaCostFactor() > sword.proposedStaminaCostFactor());
        assertTrue(mace.proposedPoiseEmphasis() > sword.proposedPoiseEmphasis());
        assertEquals("mkm:attack/sword/finisher",
                WeaponCombatProfiles.animationFor(sword, 3, false, true));
        assertEquals(WeaponCombatProfiles.Archetype.OTHER,
                WeaponCombatProfiles.forItem("minecraft:pickaxe").archetype());
    }
}
