package com.eretenkodaniil.mkm.combat.presentation;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import org.junit.jupiter.api.Test;

final class MnaeDemoClipRoutingTest {
    private static AttackPresentationEvent event(AttackPresentationEvent.ActionKind kind,
            EquipmentRules.Family family, int stage, String id,
            AttackPresentationEvent.Outcome status) {
        return new AttackPresentationEvent(kind, family,
                AttackPresentationEvent.Hand.MAIN_HAND,
                AttackPresentationEvent.Arm.RIGHT,
                stage, id, 100L, 1L, status);
    }

    @Test void routesOnlyExistingFirstStrikeDemos() {
        assertEquals(MnaeDemoClipRouting.SWORD_DEMO,
                MnaeDemoClipRouting.demoClipFor(event(
                        AttackPresentationEvent.ActionKind.VANILLA_SWING,
                        EquipmentRules.Family.SWORD, 1, "mkm:attack/sword/light",
                        AttackPresentationEvent.Outcome.LOCAL_PREDICTED)).orElseThrow());
        assertEquals(MnaeDemoClipRouting.UNARMED_DEMO,
                MnaeDemoClipRouting.demoClipFor(event(
                        AttackPresentationEvent.ActionKind.VANILLA_SWING,
                        EquipmentRules.Family.NONE, 1, "mkm:attack/unarmed/light",
                        AttackPresentationEvent.Outcome.LOCAL_PREDICTED)).orElseThrow());
    }

    @Test void missingAssetsOrDifferentActionNeverGetMisrouted() {
        assertTrue(MnaeDemoClipRouting.demoClipFor(event(
                AttackPresentationEvent.ActionKind.VANILLA_SWING,
                EquipmentRules.Family.AXE, 1, "mkm:attack/axe/light",
                AttackPresentationEvent.Outcome.LOCAL_PREDICTED)).isEmpty());
        assertTrue(MnaeDemoClipRouting.demoClipFor(event(
                AttackPresentationEvent.ActionKind.VANILLA_SWING,
                EquipmentRules.Family.SWORD, 2, "mkm:attack/sword/light_2",
                AttackPresentationEvent.Outcome.LOCAL_PREDICTED)).isEmpty());
        assertTrue(MnaeDemoClipRouting.demoClipFor(event(
                AttackPresentationEvent.ActionKind.HEAVY,
                EquipmentRules.Family.SWORD, 1, "mkm:attack/sword/light",
                AttackPresentationEvent.Outcome.LOCAL_PREDICTED)).isEmpty());
        assertTrue(MnaeDemoClipRouting.demoClipFor(event(
                AttackPresentationEvent.ActionKind.VANILLA_SWING,
                EquipmentRules.Family.NONE, 1, "mkm:attack/generic/light",
                AttackPresentationEvent.Outcome.LOCAL_PREDICTED)).isEmpty());
        assertTrue(MnaeDemoClipRouting.demoClipFor(event(
                AttackPresentationEvent.ActionKind.VANILLA_SWING,
                EquipmentRules.Family.SWORD, 1, "mkm:attack/sword/light",
                AttackPresentationEvent.Outcome.SERVER_CONFIRMED_HIT)).isEmpty());
    }
}
