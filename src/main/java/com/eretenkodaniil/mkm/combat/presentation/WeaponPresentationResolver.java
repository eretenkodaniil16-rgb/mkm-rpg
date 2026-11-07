package com.eretenkodaniil.mkm.combat.presentation;

import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import com.eretenkodaniil.mkm.equipment.WeaponCombatProfiles;
import java.util.Objects;

/**
 * Single read-only, platform-independent hand and weapon context for Combat → MNAE.
 *
 * A player's preferred main arm (LEFT/RIGHT) is NOT the inventory MAIN_HAND slot.
 * Only the actual action hand chooses the attacking weapon. A sword held in OFF_HAND
 * must never change a vanilla LMB's MAIN_HAND family.
 *
 * EquipmentRules.profile(id, MAIN_HAND) is the canonical item FAMILY classifier
 * for both slots here. It is used for observation only: OFF_HAND stats are still
 * computed separately by EquipmentRules.profile(id, OFF_HAND).
 */
public final class WeaponPresentationResolver {
    public record Hands(String mainHandItemId, String offHandItemId,
                        AttackPresentationEvent.Arm preferredMainArm) {
        public Hands {
            Objects.requireNonNull(mainHandItemId, "mainHandItemId");
            Objects.requireNonNull(offHandItemId, "offHandItemId");
            Objects.requireNonNull(preferredMainArm, "preferredMainArm");
            if (mainHandItemId.isBlank() || offHandItemId.isBlank()
                    || mainHandItemId.length() > 160 || offHandItemId.length() > 160) {
                throw new IllegalArgumentException("Invalid held item identifiers");
            }
        }
    }

    public record Item(String id, EquipmentRules.Family family) {
        public Item {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(family, "family");
        }
        public boolean empty() {
            return "minecraft:air".equals(id);
        }
    }

    /** Immutable snapshot of BOTH slots, plus the explicitly selected action hand. */
    public record HandContext(Item mainHand, Item offHand,
                              AttackPresentationEvent.Hand actionHand,
                              AttackPresentationEvent.Arm preferredMainArm,
                              AttackPresentationEvent.Arm attackingArm) {
        public HandContext {
            Objects.requireNonNull(mainHand, "mainHand");
            Objects.requireNonNull(offHand, "offHand");
            Objects.requireNonNull(actionHand, "actionHand");
            Objects.requireNonNull(preferredMainArm, "preferredMainArm");
            Objects.requireNonNull(attackingArm, "attackingArm");
            AttackPresentationEvent.Arm expected = actionHand == AttackPresentationEvent.Hand.MAIN_HAND
                    ? preferredMainArm : opposite(preferredMainArm);
            if (attackingArm != expected) {
                throw new IllegalArgumentException("Physical arm does not match action hand");
            }
        }
        public Item activeItem() {
            return actionHand == AttackPresentationEvent.Hand.MAIN_HAND ? mainHand : offHand;
        }
    }

    public record Resolved(HandContext hands, AttackSequenceRules.Selection selection,
                           AttackPresentationEvent event) {
        public Resolved {
            Objects.requireNonNull(hands, "hands");
            Objects.requireNonNull(selection, "selection");
            Objects.requireNonNull(event, "event");
            if (event.handContext() != hands) {
                throw new IllegalArgumentException("Presentation event lost hand context");
            }
        }
    }

    private WeaponPresentationResolver() {}

    public static HandContext inspect(Hands hands, AttackPresentationEvent.Hand actualActionHand) {
        Objects.requireNonNull(hands, "hands");
        Objects.requireNonNull(actualActionHand, "actualActionHand");
        return new HandContext(
                item(hands.mainHandItemId()), item(hands.offHandItemId()), actualActionHand,
                hands.preferredMainArm(),
                actualActionHand == AttackPresentationEvent.Hand.MAIN_HAND
                        ? hands.preferredMainArm() : opposite(hands.preferredMainArm()));
    }

    public static Resolved resolveVanillaAttack(
            Hands actualHands, AttackPresentationEvent.ActionKind kind,
            AttackSequenceRules.State previous, long gameTick) {
        if (kind != AttackPresentationEvent.ActionKind.VANILLA_SWING
                && kind != AttackPresentationEvent.ActionKind.HEAVY) {
            throw new IllegalArgumentException("Only real vanilla mainhand LMB can use this route");
        }
        // Actual vanilla LMB is always MAIN_HAND (regardless of preferred LEFT/RIGHT arm).
        HandContext hands = inspect(actualHands, AttackPresentationEvent.Hand.MAIN_HAND);
        WeaponCombatProfiles.Profile profile = WeaponCombatProfiles.forItem(hands.activeItem().id());
        AttackSequenceRules.Selection selection = AttackSequenceRules.next(
                previous, hands.activeItem().family(), hands.activeItem().id(), gameTick,
                profile.maxSequenceSteps(),
                profile.fastChainWindowTicks(), profile.alternatePauseWindowTicks());
        String animationId = WeaponCombatProfiles.animationFor(profile,
                selection.state().step(), selection.alternatePause(), selection.finisher());
        AttackPresentationEvent event = new AttackPresentationEvent(
                kind, hands.activeItem().family(), hands.actionHand(), hands.attackingArm(),
                selection.state().step(), animationId, gameTick,
                selection.state().sequenceId(),
                AttackPresentationEvent.Outcome.LOCAL_PREDICTED, hands);
        return new Resolved(hands, selection, event);
    }

    private static Item item(String id) {
        // Deliberately MAIN_HAND: presentation family, not gameplay equipment bonuses.
        // Unsupported pickaxe is Family.NONE but remains NON-EMPTY, so it is generic,
        // never unarmed. No second whitelist of item IDs is introduced.
        return new Item(id, EquipmentRules.profile(id, EquipmentRules.Slot.MAIN_HAND).family());
    }

    private static AttackPresentationEvent.Arm opposite(AttackPresentationEvent.Arm arm) {
        return arm == AttackPresentationEvent.Arm.RIGHT
                ? AttackPresentationEvent.Arm.LEFT : AttackPresentationEvent.Arm.RIGHT;
    }
}
