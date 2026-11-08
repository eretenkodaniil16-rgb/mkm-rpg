package com.eretenkodaniil.mkm.mixin.client;

import com.eretenkodaniil.mkm.client.animation.LocalCombatAnimation;
import com.eretenkodaniil.mkm.client.animation.AnimationClip;
import com.eretenkodaniil.mkm.client.animation.AnimationController;
import com.eretenkodaniil.mkm.client.animation.AnimationRenderProbe;
import com.eretenkodaniil.mkm.client.animation.ArticulatedMode;
import com.eretenkodaniil.mkm.client.animation.SixPartR07Projection;
import com.eretenkodaniil.mkm.client.animation.NativeRenderDiagnostics;
import com.eretenkodaniil.mkm.client.animation.engine.ArticulatedClipV3;
import com.eretenkodaniil.mkm.client.animation.engine.Quat;
import com.eretenkodaniil.mkm.client.animation.engine.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import net.minecraft.client.Minecraft;
import com.eretenkodaniil.mkm.combat.ActionTimeline;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Small, additive model-part animations. Vanilla gait and head tracking are
 * calculated first; we only offset active local-player combat actions.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelAnimationMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("TAIL"))
    private void mkm$animateLocalCombat(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel model = (PlayerModel) (Object) this;
        // Reused PlayerModel instances must not permanently inherit one
        // experimental no-armor frame's hidden-geometry flags.
        model.head.skipDraw=false; model.hat.skipDraw=false;
        model.body.skipDraw=false; model.jacket.skipDraw=false;
        model.leftArm.skipDraw=false; model.rightArm.skipDraw=false;
        model.leftSleeve.skipDraw=false; model.rightSleeve.skipDraw=false;
        model.leftLeg.skipDraw=false; model.rightLeg.skipDraw=false;
        model.leftPants.skipDraw=false; model.rightPants.skipDraw=false;
        LocalCombatAnimation.Frame frame = LocalCombatAnimation.frame(state);
        if (frame == null || frame.weight() <= 0.0001F) return;

        ArticulatedMode.Mode mode=ArticulatedMode.mode(state,frame);
        if(mode==ArticulatedMode.Mode.R07_17){
            // Exactly one geometry owner this frame. Only render layer draws.
            model.head.skipDraw=true; model.hat.skipDraw=true;
            model.body.skipDraw=true; model.jacket.skipDraw=true;
            model.leftArm.skipDraw=true; model.rightArm.skipDraw=true;
            model.leftSleeve.skipDraw=true; model.rightSleeve.skipDraw=true;
            model.leftLeg.skipDraw=true; model.rightLeg.skipDraw=true;
            model.leftPants.skipDraw=true; model.rightPants.skipDraw=true;
            return;
        }
        if(mode==ArticulatedMode.Mode.SIX_PART_DERIVED){
            SixPartR07Projection.apply(model,state,ArticulatedMode.clip(),
                    frame.sampledClip().localTime());
            copyAngles(model.rightSleeve,model.rightArm);
            copyAngles(model.leftSleeve,model.leftArm);
            copyAngles(model.rightPants,model.rightLeg);
            copyAngles(model.leftPants,model.leftLeg);
            copyAngles(model.jacket,model.body);
            if(Minecraft.getInstance().level!=null)
                NativeRenderDiagnostics.mark(Minecraft.getInstance().level.getGameTime(),
                        "R07_6_MODEL_SETUP","mkm:r07/mid_guard_sword_cut",
                        "SIX_PART_APPROXIMATION_NOT_OLD_MNAE6");
            return;
        }
        ActionTimeline action = frame.timeline();
        float w = frame.weight();

        if (frame.sampledClip() != null) {
            // C3's validated immutable data is the primary animation system.
            // Its rotations are additive; vanilla locomotion/head tracking remain.
            applyClip(model, state, frame.sampledClip(), frame.attackLegWeight());
            if (Minecraft.getInstance().level != null) {
                AnimationRenderProbe.mark(Minecraft.getInstance().level.getGameTime(),
                        frame.sampledClip().clipId());
            }
        } else if (action.phase() == ActionTimeline.Phase.DODGING) {
            // Tuck the body into a crouched roll, then release naturally.
            model.body.xRot -= 0.70F * w;
            model.head.xRot += 0.45F * w;
            model.rightArm.xRot -= 1.30F * w;
            model.leftArm.xRot -= 1.30F * w;
            model.rightArm.zRot -= 0.55F * w;
            model.leftArm.zRot += 0.55F * w;
            model.rightLeg.xRot += 0.95F * w;
            model.leftLeg.xRot += 0.95F * w;
        } else if (action.kind() == ActionTimeline.Kind.LIGHT_COMBO
                && AnimationController.profileForHands(action,
                    Minecraft.getInstance().player.getMainHandItem(),
                    Minecraft.getInstance().player.getOffhandItem()) != null) {
            boolean leftHanded = state.mainArm == HumanoidArm.LEFT;
            ModelPart mainArm = leftHanded ? model.leftArm : model.rightArm;
            ModelPart offArm = leftHanded ? model.rightArm : model.leftArm;
            float mirror = leftHanded ? -1F : 1F;

            switch (action.comboStep()) {
                case 1 -> {
                    // Diagonal descending cut.
                    mainArm.xRot -= 1.95F * w;
                    mainArm.yRot += 0.75F * w * mirror;
                    mainArm.zRot += 0.20F * w * mirror;
                    model.body.yRot -= 0.22F * w * mirror;
                    offArm.xRot -= 0.20F * w;
                }
                case 2 -> {
                    // Crossing horizontal cut in the other direction.
                    mainArm.xRot -= 1.15F * w;
                    mainArm.yRot -= 1.25F * w * mirror;
                    mainArm.zRot -= 0.45F * w * mirror;
                    model.body.yRot += 0.35F * w * mirror;
                    offArm.xRot -= 0.45F * w;
                }
                case 3 -> {
                    // Heavier two-arm overhead finishing swing.
                    mainArm.xRot -= 2.55F * w;
                    offArm.xRot -= 1.70F * w;
                    mainArm.yRot += 0.15F * w * mirror;
                    model.body.xRot += 0.32F * w;
                    model.rightLeg.xRot -= 0.25F * w;
                    model.leftLeg.xRot += 0.25F * w;
                }
                default -> { }
            }
        }

        // The jacket and sleeves are separate cubes in vanilla's player model.
        // Keep their rotations aligned with modified skeleton parts.
        copyAngles(model.rightSleeve, model.rightArm);
        copyAngles(model.leftSleeve, model.leftArm);
        copyAngles(model.rightPants, model.rightLeg);
        copyAngles(model.leftPants, model.leftLeg);
        copyAngles(model.jacket, model.body);
    }

    /**
     * Redirect the vanilla hand transform only during R07's eligible
     * mainhand sword. Offhand and first-person retain the vanilla item layer.
     * Presentation marker never issues a gameplay hit.
     */
    @Inject(method = "translateToHand(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("HEAD"), cancellable = true)
    private void mkm$r07Socket(AvatarRenderState state, HumanoidArm arm,
                               PoseStack stack, CallbackInfo ci) {
        LocalCombatAnimation.Frame frame=LocalCombatAnimation.frame(state);
        if(!ArticulatedMode.active(state,frame)||arm!=state.mainArm)return;
        ArticulatedClipV3 clip=ArticulatedMode.clip();
        var pose=clip.sample(frame.sampledClip().localTime());
        ArticulatedClipV3.Sample hand=pose.get(clip.socket().hand());
        if(hand==null)return;
        Quat rot=hand.rotation();
        Vec3 point=clip.socketPoint(pose,false);
        if(arm==HumanoidArm.LEFT){
            point=new Vec3(-point.x(),point.y(),point.z());
            rot=new Quat(rot.x(),-rot.y(),-rot.z(),rot.w());
        }
        ((PlayerModel)(Object)this).root().translateAndRotate(stack);
        stack.translate(point.x()/16D,point.y()/16D,point.z()/16D);
        stack.rotate(new Quaternionf((float)rot.x(),(float)rot.y(),
                (float)rot.z(),(float)rot.w()));
        ci.cancel();
    }

    private static void applyClip(
            PlayerModel model, AvatarRenderState state,
            com.eretenkodaniil.mkm.client.animation.AnimationController.Frame clip,
            float attackLegWeight) {
        boolean left = state.mainArm == HumanoidArm.LEFT;
        boolean attackClip = clip.clipId().startsWith("mkm:sword/light_")
                || clip.clipId().startsWith("mkm:unarmed/");
        for (var entry : clip.rotations().entrySet()) {
            AnimationClip.Bone bone = entry.getKey();
            AnimationClip.Rotation rotation = entry.getValue();
            ModelPart part = switch (bone) {
                case HEAD -> model.head;
                case BODY -> model.body;
                case RIGHT_ARM -> left ? model.leftArm : model.rightArm;
                case LEFT_ARM -> left ? model.rightArm : model.leftArm;
                case RIGHT_LEG -> left && attackClip ? model.leftLeg : model.rightLeg;
                case LEFT_LEG -> left && attackClip ? model.rightLeg : model.leftLeg;
            };
            // Data is authored for right-hand dominance; mirror the transverse
            // axes for left-handed player animations without changing skeleton.
            boolean leg = bone == AnimationClip.Bone.LEFT_LEG
                    || bone == AnimationClip.Bone.RIGHT_LEG;
            float mirror = left && (!leg || attackClip) ? -1F : 1F;
            // C3.4 attack stance is deliberately a subtle additive lower-body
            // offset, never a replacement of vanilla run/jump leg animation.
            // Preserve all C3.1 directional dodge leg choreography untouched.
            float intensity = leg && attackClip ? attackLegWeight : 1F;
            part.xRot += (float) rotation.x() * intensity;
            part.yRot += (float) rotation.y() * mirror * intensity;
            part.zRot += (float) rotation.z() * mirror * intensity;
        }
    }

    private static void copyAngles(ModelPart overlay, ModelPart source) {
        overlay.xRot = source.xRot;
        overlay.yRot = source.yRot;
        overlay.zRot = source.zRot;
    }
}
