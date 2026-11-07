package com.eretenkodaniil.mkm.combat.presentation;

import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.Objects;

/**
 * Read-only R-combo renderer selection from the SINGLE canonical Combat
 * WeaponPresentationResolver.HandContext. No second item classifier exists.
 *
 * Client bridge translates real ItemStacks to item registry IDs, then calls
 * Combat WeaponPresentationResolver.inspect(..., MAIN_HAND) once.
 * This policy cannot attack, debit stamina or mutate game state.
 */
public final class MnaeRComboPresentationRoute {
    public enum Renderer { VANILLA, LEGACY_C3, NATIVE_MNAE }
    public enum Reason {
        NO_ACTION, DODGE_STAYS_C3, NOT_MAIN_HAND, UNSUPPORTED_FAMILY,
        OFFHAND_GRIP_UNVERIFIED, FIRST_STAGE_NATIVE, F8_C3, LATER_STAGE_C3
    }
    public record Decision(Renderer renderer, String legacyProfile, String nativeClip,
                           Reason reason, AttackPresentationEvent.Arm attackingArm) {
        public Decision {
            Objects.requireNonNull(renderer);
            Objects.requireNonNull(reason);
            Objects.requireNonNull(attackingArm);
            if (renderer == Renderer.NATIVE_MNAE && nativeClip == null)
                throw new IllegalArgumentException("Native renderer needs a clip");
            if (renderer == Renderer.LEGACY_C3 && legacyProfile == null)
                throw new IllegalArgumentException("Legacy renderer needs a profile");
        }
    }
    private MnaeRComboPresentationRoute() {}

    public static Decision select(WeaponPresentationResolver.HandContext ctx,
                                  int stage, boolean dodging, boolean nativeEnabled) {
        Objects.requireNonNull(ctx);
        AttackPresentationEvent.Arm arm = ctx.attackingArm();
        if (dodging) {
            // Explicitly a legacy motion-only DODGE, not a sword ATTACK.
            // Release gate: later create weapon-agnostic roll resource.
            return new Decision(Renderer.LEGACY_C3,"mkm:one_handed_sword",null,
                    Reason.DODGE_STAYS_C3,arm);
        }
        if (stage < 1 || stage > 3) return vanilla(Reason.NO_ACTION,arm);
        if (ctx.actionHand() != AttackPresentationEvent.Hand.MAIN_HAND)
            return vanilla(Reason.NOT_MAIN_HAND,arm);
        var item=ctx.activeItem();
        if (!item.empty() && item.family() != EquipmentRules.Family.SWORD)
            return vanilla(Reason.UNSUPPORTED_FAMILY,arm);
        var off=ctx.offHand();
        if (!off.empty() && off.family() != EquipmentRules.Family.SHIELD)
            return vanilla(Reason.OFFHAND_GRIP_UNVERIFIED,arm);
        String profile=item.empty()?"mkm:unarmed":"mkm:one_handed_sword";
        if (!nativeEnabled) return new Decision(Renderer.LEGACY_C3,profile,null,
                Reason.F8_C3,arm);
        if (stage > 1) return new Decision(Renderer.LEGACY_C3,profile,null,
                Reason.LATER_STAGE_C3,arm);
        return new Decision(Renderer.NATIVE_MNAE,profile,
                item.empty()?MnaeDemoClipRouting.UNARMED_DEMO:MnaeDemoClipRouting.SWORD_DEMO,
                Reason.FIRST_STAGE_NATIVE,arm);
    }
    private static Decision vanilla(Reason reason,AttackPresentationEvent.Arm arm) {
        return new Decision(Renderer.VANILLA,null,null,reason,arm);
    }
}
