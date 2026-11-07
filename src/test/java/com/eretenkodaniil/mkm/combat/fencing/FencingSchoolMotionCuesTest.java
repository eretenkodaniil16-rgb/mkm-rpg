package com.eretenkodaniil.mkm.combat.fencing;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.combat.presentation.AttackPresentationEvent;
import com.eretenkodaniil.mkm.combat.presentation.WeaponPresentationResolver;
import org.junit.jupiter.api.Test;

final class FencingSchoolMotionCuesTest {
    private static AttackPresentationEvent normalAttack(
            String mainhand, String offhand, AttackPresentationEvent.Arm arm,
            int stage, AttackPresentationEvent.Outcome outcome) {
        var hands = WeaponPresentationResolver.inspect(
                new WeaponPresentationResolver.Hands(mainhand, offhand, arm),
                AttackPresentationEvent.Hand.MAIN_HAND);
        return new AttackPresentationEvent(
                AttackPresentationEvent.ActionKind.VANILLA_SWING,
                hands.activeItem().family(), hands.actionHand(), hands.attackingArm(),
                stage, "mkm:attack/generic/light", 200L, 1L, outcome, hands);
    }

    @Test void differentSchoolsExposeDistinctStageOneToThreeChoreographyAndArms() {
        var german = new FencingSchoolState(FencingSchool.LIECHTENAUER, 120L, 180L);
        var italian = new FencingSchoolState(FencingSchool.FIORE, 120L, 180L);
        var spanish = new FencingSchoolState(FencingSchool.DESTREZA, 120L, 180L);
        for (int stage = 1; stage <= 3; stage++) {
            var ev = normalAttack("minecraft:iron_sword", "minecraft:shield",
                    AttackPresentationEvent.Arm.LEFT, stage,
                    AttackPresentationEvent.Outcome.LOCAL_PREDICTED);
            var g = FencingSchoolMotionCues.forEvent(german, ev, 200L).orElseThrow();
            var i = FencingSchoolMotionCues.forEvent(italian, ev, 200L).orElseThrow();
            var s = FencingSchoolMotionCues.forEvent(spanish, ev, 200L).orElseThrow();
            assertNotEquals(g.techniqueCue(), i.techniqueCue());
            assertNotEquals(i.techniqueCue(), s.techniqueCue());
            assertEquals(AttackPresentationEvent.Arm.LEFT, s.physicalArm());
            assertEquals(stage, g.sequenceStep());
            assertEquals("mkm:guard/vom_tag", g.guardCue());
        }
    }

    @Test void offhandOnlySwordAndUnsupportedWeaponNeverBecomeSchoolSwordStrike() {
        var active = new FencingSchoolState(FencingSchool.LIECHTENAUER, 120L, 180L);
        for (String nonSword : new String[]{
                "minecraft:air", "minecraft:diamond_axe", "minecraft:mace",
                "minecraft:diamond_pickaxe", "minecraft:bow", "minecraft:shield"}) {
            var ev = normalAttack(nonSword, "minecraft:netherite_sword",
                    AttackPresentationEvent.Arm.RIGHT, 1,
                    AttackPresentationEvent.Outcome.LOCAL_PREDICTED);
            assertTrue(FencingSchoolMotionCues.forEvent(active, ev, 200L).isEmpty(),
                    nonSword);
        }
    }

    @Test void warmupCancelledAndLegacyContextCannotGenerateFictitiousMotion() {
        var selected = new FencingSchoolState(FencingSchool.FIORE, 220L, 280L);
        var ev = normalAttack("minecraft:iron_sword", "minecraft:air",
                AttackPresentationEvent.Arm.RIGHT, 1,
                AttackPresentationEvent.Outcome.LOCAL_PREDICTED);
        assertTrue(FencingSchoolMotionCues.forEvent(selected, ev, 219L).isEmpty());
        assertTrue(FencingSchoolMotionCues.forEvent(
                selected, ev.withOutcome(AttackPresentationEvent.Outcome.CANCELLED), 250L).isEmpty());
        assertTrue(FencingSchoolMotionCues.forEvent(
                selected, ev.withOutcome(AttackPresentationEvent.Outcome.INTERRUPTED), 250L).isEmpty());
        var oldEvent = new AttackPresentationEvent(
                ev.actionKind(), ev.weaponFamily(), ev.hand(), ev.leadingArm(),
                1, ev.animationId(), 200L, 1L, ev.outcome());
        assertTrue(FencingSchoolMotionCues.forEvent(selected, oldEvent, 250L).isEmpty());
        assertTrue(FencingSchoolMotionCues.forEvent(
                new FencingSchoolState(), ev, 250L).isEmpty());
    }
}
