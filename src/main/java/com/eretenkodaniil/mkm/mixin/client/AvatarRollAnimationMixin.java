package com.eretenkodaniil.mkm.mixin.client;

import com.eretenkodaniil.mkm.client.animation.LocalCombatAnimation;
import com.eretenkodaniil.mkm.client.animation.NativeAnimationToggle;
import com.eretenkodaniil.mkm.client.animation.EvasionChoreography;
import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.eretenkodaniil.mkm.combat.DirectionalRollChoreography;
import com.eretenkodaniil.mkm.combat.ForwardRollChoreography;
import com.eretenkodaniil.mkm.combat.DodgeDirection;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third-person full-body dodge roll. Visual transform only: the server's
 * existing directional impulse, collisions and hitbox are not altered.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRollAnimationMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At("TAIL"))
    private void mkm$animateLocalRoll(
            AvatarRenderState state, PoseStack stack, float bodyRot, float entityScale, CallbackInfo ci) {
        LocalCombatAnimation.Frame frame = LocalCombatAnimation.frame(state);
        if (frame == null || frame.timeline().phase() != ActionTimeline.Phase.DODGING) return;
        float p = frame.progress();
        if (p <= 0.0001F || p >= 0.9999F) return;
        if(NativeAnimationToggle.enabled()){
            EvasionChoreography.Pose pose=EvasionChoreography.sample(
                    frame.timeline().dodgeDirection(),p);
            stack.translate(0F,pose.lift()*entityScale,0F);
            stack.rotate(new Quaternionf().rotationXYZ(
                    pose.pitch(),pose.yaw(),pose.roll()));
            // Different tuck / lateral step / retreat, no 2pi rigid-body turn.
            return;
        }

        if (frame.timeline().dodgeDirection() == DodgeDirection.FORWARD) {
            // Do not flip the character backwards while it moves forward.
            // A dynamic pivot + ground offset keeps the rotating torso above
            // the reference ground plane; other directions retain C3 behavior.
            ForwardRollChoreography.Pose pose = ForwardRollChoreography.sample(p);
            stack.translate(0F,
                    ForwardRollChoreography.PIVOT_HEIGHT * entityScale
                            + pose.visualYOffset() * entityScale, 0F);
            stack.rotate(new Quaternionf().rotationAxis(
                    pose.radians(), pose.axisX(), 0F, pose.axisZ()));
            stack.translate(0F, -ForwardRollChoreography.PIVOT_HEIGHT * entityScale, 0F);
            return;
        }

        // Non-forward dodges also need a grounded visual pivot. Model-local
        // forward is -Z, so invert X on forward/backward components to make
        // diagonal rolls tumble in their actual travel direction.
        DirectionalRollChoreography.Pose pose =
                DirectionalRollChoreography.sample(frame.timeline().dodgeDirection(), p);
        stack.translate(0F,
                DirectionalRollChoreography.PIVOT_HEIGHT * entityScale
                        + pose.visualYOffset() * entityScale, 0F);
        stack.rotate(new Quaternionf().rotationAxis(
                pose.radians(), pose.axisX(), 0F, pose.axisZ()));
        stack.translate(0F, -DirectionalRollChoreography.PIVOT_HEIGHT * entityScale, 0F);
    }
}
