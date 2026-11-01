package com.eretenkodaniil.mkm.client.animation;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.eretenkodaniil.mkm.combat.presentation.AttackPresentationEvent;
import com.eretenkodaniil.mkm.combat.presentation.WeaponPresentationResolver;
import org.junit.jupiter.api.Test;

/** Regression for #43: C3 fallback must not claim axe/bow/pickaxe are swords. */
final class LegacyCombatProfileRoutingTest {
    private static final ActionTimeline.Kind COMBO = ActionTimeline.Kind.LIGHT_COMBO;

    @Test void comboStagesNeverMapNonSwordMainhandToSwordAssets() {
        String[] forbidden = {
                "minecraft:iron_axe", "minecraft:mace", "minecraft:iron_pickaxe",
                "minecraft:bow", "minecraft:crossbow", "minecraft:shield"
        };
        for (int step=1; step<=3; step++) {
            ActionTimeline timeline = new ActionTimeline(
                    COMBO, ActionTimeline.Phase.ACTIVE, step, 100L, 110L);
            for (String id : forbidden) {
                var context = context(id, "minecraft:iron_sword", AttackPresentationEvent.Arm.RIGHT);
                assertNull(AnimationController.profileForHands(context, timeline),
                        id + " wrong sword fallback in step " + step);
            }
            assertEquals(AnimationController.FIRST_PROFILE,
                    AnimationController.profileForHands(context(
                            "minecraft:iron_sword", "minecraft:air",
                            AttackPresentationEvent.Arm.LEFT), timeline));
            assertEquals(AnimationController.UNARMED_PROFILE,
                    AnimationController.profileForHands(context(
                            "minecraft:air", "minecraft:iron_sword",
                            AttackPresentationEvent.Arm.LEFT), timeline));
        }
    }

    @Test void offhandShieldAndEmptyOffhandDoNotAffectMainhandCombo() {
        ActionTimeline timeline = new ActionTimeline(
                COMBO, ActionTimeline.Phase.WINDUP, 1, 100L, 110L);
        assertEquals(AnimationController.UNARMED_PROFILE,
                AnimationController.profileForHands(context(
                        "minecraft:air", "minecraft:shield", AttackPresentationEvent.Arm.RIGHT), timeline));
        assertEquals(AnimationController.FIRST_PROFILE,
                AnimationController.profileForHands(context(
                        "minecraft:iron_sword", "minecraft:shield",
                        AttackPresentationEvent.Arm.RIGHT), timeline));
    }

    @Test void dodgeKeepsLegacyMovementClipIndependentlyOfWeaponFamily() {
        ActionTimeline dodge = new ActionTimeline(
                ActionTimeline.Kind.DODGE, ActionTimeline.Phase.DODGING, 0, 100L, 110L);
        assertEquals(AnimationController.UNARMED_PROFILE,
                AnimationController.profileForHands(context(
                        "minecraft:iron_pickaxe", "minecraft:shield",
                        AttackPresentationEvent.Arm.RIGHT), dodge));
    }

    private static WeaponPresentationResolver.HandContext context(
            String main, String off, AttackPresentationEvent.Arm arm) {
        return WeaponPresentationResolver.inspect(
                new WeaponPresentationResolver.Hands(main, off, arm),
                AttackPresentationEvent.Hand.MAIN_HAND);
    }
}
