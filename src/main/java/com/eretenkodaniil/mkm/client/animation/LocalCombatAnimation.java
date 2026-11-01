package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.eretenkodaniil.mkm.combat.CombatAnimationMath;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Read-only rendering bridge. Only animate the current local avatar: C2.1
 * timeline attachment is owner-synchronized, not broadcast to other players.
 */
public final class LocalCombatAnimation {
    private LocalCombatAnimation() {}

    public record Frame(ActionTimeline timeline, float progress, float weight,
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
        String profile = AnimationController.profileForEmptyMainHand(
                client.player.getMainHandItem().isEmpty());
        AnimationController.Frame clip = AnimationController.sample(
                AnimationLibrary.embedded(), profile, timeline, progress);
        return new Frame(timeline, progress, weight, clip);
    }
}
