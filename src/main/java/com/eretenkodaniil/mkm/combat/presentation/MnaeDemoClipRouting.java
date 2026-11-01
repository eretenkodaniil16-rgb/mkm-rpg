package com.eretenkodaniil.mkm.combat.presentation;

import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.Objects;
import java.util.Optional;

/**
 * Narrow compatibility bridge to MNAE's FIRST TWO demo assets from PR #40.
 *
 * Cosmetic ID routing only. It neither loads clips nor plays animations,
 * performs hits or affects gameplay. The MNAE renderer is the only consumer.
 */
public final class MnaeDemoClipRouting {
    public static final String SWORD_DEMO = "mkm:mnae/sword_cut";
    public static final String UNARMED_DEMO = "mkm:mnae/straight_punch";

    private MnaeDemoClipRouting() {}

    /**
     * Only the first local LMB stage is backed by the two existing demos.
     * Unknown families and other stages must retain legacy C3 fallback
     * until authored native clips are available.
     */
    public static Optional<String> demoClipFor(AttackPresentationEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.actionKind() != AttackPresentationEvent.ActionKind.VANILLA_SWING
                || event.outcome() != AttackPresentationEvent.Outcome.LOCAL_PREDICTED
                || event.sequenceStep() != 1) return Optional.empty();
        if (event.weaponFamily() == EquipmentRules.Family.SWORD
                && event.animationId().equals("mkm:attack/sword/light")) {
            return Optional.of(SWORD_DEMO);
        }
        if (event.weaponFamily() == EquipmentRules.Family.NONE
                && event.animationId().equals("mkm:attack/unarmed/light")) {
            return Optional.of(UNARMED_DEMO);
        }
        return Optional.empty();
    }
}
