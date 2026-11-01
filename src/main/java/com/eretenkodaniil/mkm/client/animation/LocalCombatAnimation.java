package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.eretenkodaniil.mkm.combat.CombatAnimationMath;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.presentation.WeaponPresentationResolver;
import com.eretenkodaniil.mkm.combat.presentation.AttackPresentationEvent;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Read-only rendering bridge. Only animate the current local avatar: C2.1
 * timeline attachment is owner-synchronized, not broadcast to other players.
 */
public final class LocalCombatAnimation {
    private LocalCombatAnimation() {}

    public record Frame(ActionTimeline timeline, float progress, float weight, float attackLegWeight,
                        AnimationController.Frame sampledClip) {}

    public static Frame frame(AvatarRenderState state) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null
                || state.id != client.player.getId()) {
            return null;
        }
        ActionTimeline timeline = client.player.getAttached(CombatAttachments.TIMELINE);
        if (timeline == null || timeline.idle()) return null;
        float progress = CombatAnimationMath.phaseProgress(
                timeline, client.level.getGameTime(),
                client.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        float weight = CombatAnimationMath.poseWeight(timeline.phase(), progress);
        // Resolve actual item and action hand. Unsupported tools must NEVER
        // inherit the sword-only C3 clip simply because the slot is nonempty.
        WeaponPresentationResolver.HandContext hands = WeaponPresentationResolver.inspect(
                new WeaponPresentationResolver.Hands(
                        BuiltInRegistries.ITEM.getKey(client.player.getMainHandItem().getItem()).toString(),
                        BuiltInRegistries.ITEM.getKey(client.player.getOffhandItem().getItem()).toString(),
                        client.player.getMainArm() == HumanoidArm.LEFT
                                ? AttackPresentationEvent.Arm.LEFT : AttackPresentationEvent.Arm.RIGHT),
                AttackPresentationEvent.Hand.MAIN_HAND);
        String profile = AnimationController.profileForHands(hands, timeline);
        // Do not fall into PlayerModelAnimationMixin's old generic sword-like
        // combo fallback when an item family has no authored animation.
        // Null frame makes vanilla locomotion the neutral pose; CombatService
        // still owns the same legitimate vanilla hit/stamina/timeline.
        if (timeline.kind() == ActionTimeline.Kind.LIGHT_COMBO && profile == null) {
            return null;
        }
        AnimationController.Frame clip = AnimationController.sample(
                AnimationLibrary.embedded(), profile, timeline, progress);
        float attackLegWeight = CombatStanceMath.attackLegWeight(
                client.player.onGround(),
                client.player.getDeltaMovement().horizontalDistanceSqr());
        return new Frame(timeline, progress, weight, attackLegWeight, clip);
    }
}
