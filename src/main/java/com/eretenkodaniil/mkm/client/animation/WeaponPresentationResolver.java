package com.eretenkodaniil.mkm.client.animation;

import java.util.Objects;

/** Pure presentation policy; does not alter vanilla attacks or server timing. */
public final class WeaponPresentationResolver {
    public enum Family { EMPTY, SWORD, AXE, PICKAXE, OTHER_TOOL, BOW, SHIELD, OTHER }
    public enum Hand { MAIN, OFF }
    public enum Arm { LEFT, RIGHT }
    public enum Reason {
        C3_SELECTED, MNAE_DEMO, NOT_FIRST_COMBO, DODGE_LEGACY_C3,
        UNSUPPORTED_WEAPON, OFFHAND_OCCUPIED, OFFHAND_ATTACK_UNSUPPORTED,
        INVALID_STEP
    }
    public record Choice(String c3Profile, String nativeClip, Reason reason, Arm leadingArm) {
        public Choice { Objects.requireNonNull(reason); Objects.requireNonNull(leadingArm); }
        public boolean hasSafeClip() { return c3Profile != null; }
    }
    private WeaponPresentationResolver() {}

    public static Choice resolve(Family main, Family off, Hand active,
                                 boolean leftDominant, int comboStep,
                                 boolean dodge, boolean nativeMode) {
        Objects.requireNonNull(main); Objects.requireNonNull(off); Objects.requireNonNull(active);
        Arm arm = active == Hand.MAIN
                ? (leftDominant ? Arm.LEFT : Arm.RIGHT)
                : (leftDominant ? Arm.RIGHT : Arm.LEFT);
        if (dodge) {
            // Only the legacy directional C3 dodge tracks exist currently.
            return new Choice(AnimationController.FIRST_PROFILE, null,
                    Reason.DODGE_LEGACY_C3, arm);
        }
        if (comboStep < 1 || comboStep > 3)
            return new Choice(null, null, Reason.INVALID_STEP, arm);
        if (active == Hand.OFF)
            return new Choice(null, null, Reason.OFFHAND_ATTACK_UNSUPPORTED, arm);
        // No verified offhand grip/socket mapping exists. Avoid waving an
        // offhand sword or shield near the head during a mainhand-only pose.
        if (off != Family.EMPTY)
            return new Choice(null, null, Reason.OFFHAND_OCCUPIED, arm);
        String profile = switch (main) {
            case EMPTY -> AnimationController.UNARMED_PROFILE;
            case SWORD -> AnimationController.FIRST_PROFILE;
            default -> null;
        };
        if (profile == null)
            return new Choice(null, null, Reason.UNSUPPORTED_WEAPON, arm);
        if (!nativeMode)
            return new Choice(profile, null, Reason.C3_SELECTED, arm);
        if (comboStep != 1)
            return new Choice(profile, null, Reason.NOT_FIRST_COMBO, arm);
        return new Choice(profile,
                main == Family.EMPTY ? "mkm:mnae/straight_punch" : "mkm:mnae/sword_cut",
                Reason.MNAE_DEMO, arm);
    }
}
