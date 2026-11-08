package com.eretenkodaniil.mkm.mixin.client;

import com.eretenkodaniil.mkm.client.animation.LocalCombatAnimation;
import com.eretenkodaniil.mkm.client.animation.DodgeVisualChoreography;
import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No arbitrary rigid 360-degree tumbling. Three bounded cosmetic silhouettes:
 * forward tuck, side step, backward hop, with diagonal variants. The existing
 * server dodge direction/impulse, collision and timing are fully authoritative.
 * Articulated arm/foot contact remains a separate real F5 acceptance gate.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRollAnimationMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At("TAIL"))
    private void mkm$animateLocalDodge(AvatarRenderState state,PoseStack stack,
                                     float bodyRot,float entityScale,CallbackInfo ci){
        LocalCombatAnimation.Frame frame=LocalCombatAnimation.frame(state);
        if(frame==null||frame.timeline().phase()!=ActionTimeline.Phase.DODGING)return;
        DodgeVisualChoreography.Pose pose=DodgeVisualChoreography.sample(
                frame.timeline().dodgeDirection(),frame.progress());
        // Vertex transforms only: no actual vertical displacement of player.
        float pivot=.90F*entityScale;
        stack.translate(0,pivot+pose.lift()*entityScale,0);
        stack.rotate(new Quaternionf().rotationYXZ(
                pose.yaw(),pose.pitch(),pose.bank()));
        stack.translate(0,-pivot,0);
    }
}
