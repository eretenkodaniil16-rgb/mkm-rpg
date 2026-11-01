package com.eretenkodaniil.mkm.combat.presentation;

import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.Objects;

/**
 * Read-only combat-to-MNAE presentation signal. NEVER executes gameplay.
 * Local vanilla-LMB events are predictions, not proof of a server hit.
 */
public record AttackPresentationEvent(
        ActionKind actionKind,
        EquipmentRules.Family weaponFamily,
        Hand hand,
        Arm leadingArm,
        int sequenceStep,
        String animationId,
        long startGameTick,
        long sequenceId,
        Outcome outcome) {
    public enum ActionKind {
        VANILLA_SWING, SPECIAL_COMBO, HEAVY, CHARGED, DODGE, RENEWAL
    }
    public enum Hand { MAIN_HAND, OFF_HAND }
    public enum Arm { RIGHT, LEFT }
    public enum Outcome {
        LOCAL_PREDICTED, SERVER_CONFIRMED_HIT, SERVER_CONFIRMED_BLOCK,
        MISSED, CANCELLED, INTERRUPTED, COMPLETED
    }
    public AttackPresentationEvent {
        Objects.requireNonNull(actionKind, "actionKind");
        Objects.requireNonNull(weaponFamily, "weaponFamily");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(leadingArm, "leadingArm");
        Objects.requireNonNull(animationId, "animationId");
        Objects.requireNonNull(outcome, "outcome");
        if (!animationId.startsWith("mkm:") || animationId.length() > 96
                || sequenceStep < 0 || sequenceStep > 4
                || startGameTick < 0 || sequenceId < 0) {
            throw new IllegalArgumentException("Invalid bounded presentation metadata");
        }
    }
    /** Never infer SERVER_CONFIRMED_HIT from client input or an animation marker. */
    public AttackPresentationEvent withOutcome(Outcome next) {
        return new AttackPresentationEvent(actionKind, weaponFamily, hand,
                leadingArm, sequenceStep, animationId, startGameTick, sequenceId, next);
    }
}
