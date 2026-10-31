package com.eretenkodaniil.mkm.mixin.client;

import com.eretenkodaniil.mkm.client.animation.LocalCombatAnimation;
import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.eretenkodaniil.mkm.combat.CombatAnimationMath;
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

        float axisX = CombatAnimationMath.rollAxisX(frame.timeline().dodgeDirection());
        float axisZ = CombatAnimationMath.rollAxisZ(frame.timeline().dodgeDirection());
        float norm = (float) Math.hypot(axisX, axisZ);
        if (norm < 0.001F) return;

        // Rotate about the avatar's waist rather than the feet to avoid a
        // huge visual arc. Model animation tucks arms, legs and head in.
        stack.translate(0F, 0.75F * entityScale, 0F);
        stack.rotate(new Quaternionf().rotationAxis(
                CombatAnimationMath.rollRadians(p), axisX / norm, 0F, axisZ / norm));
        stack.translate(0F, -0.75F * entityScale, 0F);
    }
}
