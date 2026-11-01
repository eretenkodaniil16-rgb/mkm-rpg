package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.eretenkodaniil.mkm.combat.ActionTimelineRules;
import com.eretenkodaniil.mkm.combat.CombatAnimationMath;
import com.eretenkodaniil.mkm.combat.presentation.WeaponPresentationResolver;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.Map;

/**
 * Bridges a server-owned combat timeline to client-only sampled pose data.
 * Damage moments and position are never sourced from marker or pose data.
 */
public final class AnimationController {
    public static final String FIRST_PROFILE = "mkm:one_handed_sword";
    public static final String UNARMED_PROFILE = "mkm:unarmed";

    /** An empty main hand should not trigger sword slashes. */
    public static String profileForEmptyMainHand(boolean emptyHand) {
        return emptyHand ? UNARMED_PROFILE : FIRST_PROFILE;
    }

    /**
     * Only authored sword/unarmed combo clips may drive these action profiles.
     * A neutral unsupported weapon has NO sword combo pose. Dodges share C3's
     * identical generic dodge clips via either profile without changing physics.
     */
    public static String profileForHands(
            WeaponPresentationResolver.HandContext hands, ActionTimeline timeline) {
        if (hands == null || timeline == null) return null;
        if (timeline.kind() == ActionTimeline.Kind.DODGE) return UNARMED_PROFILE;
        if (timeline.kind() != ActionTimeline.Kind.LIGHT_COMBO) return null;
        if (hands.activeItem().empty()) return UNARMED_PROFILE;
        return hands.activeItem().family() == EquipmentRules.Family.SWORD
                ? FIRST_PROFILE : null;
    }

    private AnimationController() {}

    public record Frame(AnimationClip.Layer layer, Map<AnimationClip.Bone, AnimationClip.Rotation> rotations,
                        String clipId, double localTime) {
    }

    public static String actionKey(ActionTimeline timeline) {
        if (timeline == null || timeline.idle()) return null;
        if (timeline.phase() == ActionTimeline.Phase.DODGING) {
            return "dodge_" + timeline.dodgeDirection();
        }
        if (timeline.kind() == ActionTimeline.Kind.LIGHT_COMBO
                && timeline.comboStep() >= 1 && timeline.comboStep() <= 3) {
            return "combo_" + timeline.comboStep();
        }
        return null;
    }

    public static double normalizedClipTime(ActionTimeline timeline, float phaseProgress) {
        if (timeline == null || timeline.idle()) return 0D;
        double p = CombatAnimationMath.clamp01(phaseProgress);
        if (timeline.phase() == ActionTimeline.Phase.DODGING) return p;
        if (timeline.kind() != ActionTimeline.Kind.LIGHT_COMBO
                || timeline.comboStep() < 1 || timeline.comboStep() > 3) return 0D;

        long windup = ActionTimelineRules.windupTicks(timeline.comboStep());
        long active = 1L;
        long recovery = ActionTimelineRules.recoveryTicks(timeline.comboStep());
        long full = windup + active + recovery;
        return switch (timeline.phase()) {
            case WINDUP -> windup * p / full;
            case ACTIVE -> (windup + active * p) / full;
            case RECOVERY -> (windup + active + recovery * p) / full;
            default -> 0D;
        };
    }

    public static Frame sample(AnimationLibrary library, String profileId,
                               ActionTimeline timeline, float phaseProgress) {
        if (library == null || timeline == null) return null;
        AnimationProfile profile = library.profile(profileId);
        String action = actionKey(timeline);
        if (profile == null || action == null) return null;
        AnimationClip clip = library.clip(profile.clipFor(action));
        if (clip == null) return null;
        double t = normalizedClipTime(timeline, phaseProgress);
        return new Frame(clip.layer(), clip.sample(t), clip.id(), t);
    }
}
