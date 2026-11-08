package com.eretenkodaniil.mkm.combat.fencing;

import com.eretenkodaniil.mkm.combat.presentation.AttackPresentationEvent;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure, optional MNAE choreography vocabulary, NOT an animation or attack engine.
 *
 * Does not load clips, alter combat timelines or emit packets. A cue is meaningful
 * only if the original accepted combat presentation event already exists.
 * Unknown cues MUST fall back without posing an unarmed or sword animation.
 */
public final class FencingSchoolMotionCues {
    public record Cue(FencingSchool school, String guardCue, String techniqueCue,
                      int sequenceStep, AttackPresentationEvent.Arm physicalArm) {
        public Cue {
            Objects.requireNonNull(school, "school");
            Objects.requireNonNull(guardCue, "guardCue");
            Objects.requireNonNull(techniqueCue, "techniqueCue");
            Objects.requireNonNull(physicalArm, "physicalArm");
            if (sequenceStep < 1 || sequenceStep > 3) {
                throw new IllegalArgumentException("Invalid fencing sequence stage");
            }
        }
    }

    private static final Map<FencingSchool, List<String>> TECHNIQUES = Map.of(
            FencingSchool.LIECHTENAUER, List.of(
                    "mkm:fencing/liechtenauer/oberhau",
                    "mkm:fencing/liechtenauer/zwerchhau",
                    "mkm:fencing/liechtenauer/unterhau"),
            FencingSchool.FIORE, List.of(
                    "mkm:fencing/fiore/mandritto",
                    "mkm:fencing/fiore/riverso",
                    "mkm:fencing/fiore/punta"),
            FencingSchool.DESTREZA, List.of(
                    "mkm:fencing/destreza/tajo",
                    "mkm:fencing/destreza/reves",
                    "mkm:fencing/destreza/estocada"));

    private FencingSchoolMotionCues() {}

    /**
     * Client owner-local read-only selector. NEVER infer real hit outcome from
     * this cue or turn it into a separate AttackPresentationEvent.
     */
    public static Optional<Cue> forEvent(
            FencingSchoolState schoolState, AttackPresentationEvent event,
            long localGameTick) {
        Objects.requireNonNull(schoolState, "schoolState");
        Objects.requireNonNull(event, "event");
        if (event.hand() != AttackPresentationEvent.Hand.MAIN_HAND
                || event.handContext() == null
                || event.startGameTick() > localGameTick
                || event.sequenceStep() < 1 || event.sequenceStep() > 3
                || (event.actionKind() != AttackPresentationEvent.ActionKind.VANILLA_SWING
                    && event.actionKind() != AttackPresentationEvent.ActionKind.SPECIAL_COMBO)
                || event.outcome() == AttackPresentationEvent.Outcome.CANCELLED
                || event.outcome() == AttackPresentationEvent.Outcome.INTERRUPTED) {
            return Optional.empty();
        }
        // Actual held item determines family. An OFFHAND sword is irrelevant.
        var mainhand = event.handContext().mainHand();
        if (mainhand.empty()) return Optional.empty();
        Optional<FencingSchool> effective = FencingSchoolRules.effectiveSchool(
                schoolState, mainhand.family(), event.startGameTick());
        if (effective.isEmpty()) return Optional.empty();
        FencingSchool school = effective.orElseThrow();
        List<String> techniques = TECHNIQUES.get(school);
        if (techniques == null) return Optional.empty();
        return Optional.of(new Cue(school, school.visualGuardCue(),
                techniques.get(event.sequenceStep() - 1),
                event.sequenceStep(), event.leadingArm()));
    }
}
