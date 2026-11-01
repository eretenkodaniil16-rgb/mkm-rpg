package com.eretenkodaniil.mkm.mixin.client;

import com.eretenkodaniil.mkm.client.animation.LocalCombatAnimation;
import com.eretenkodaniil.mkm.client.animation.AnimationClip;
import com.eretenkodaniil.mkm.client.animation.AnimationRenderProbe;
import com.eretenkodaniil.mkm.client.animation.NativeAnimationToggle;
import com.eretenkodaniil.mkm.client.animation.VanillaAvatarAdapter;
import com.eretenkodaniil.mkm.client.animation.WeaponPresentationResolver;
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
        LocalCombatAnimation.Frame frame = LocalCombatAnimation.frame(state);
        if (frame == null || frame.weight() <= 0.0001F) return;

        PlayerModel model = (PlayerModel) (Object) this;
        ActionTimeline action = frame.timeline();
        float w = frame.weight();

        WeaponPresentationResolver.Selection selection=frame.selection();
        float[] vanillaAngles=snapshotAngles(model);
        String applied=null;
        String renderer="VANILLA";
        String reason=selection.reason();
        if (frame.sampledClip() != null) {
            if (selection.renderer() == WeaponPresentationResolver.Renderer.MNAE) {
                // One path ONLY. Never apply C3 and MNAE in the same frame.
                applied=VanillaAvatarAdapter.apply(model,state,frame);
                if(applied!=null)renderer="MNAE";
                else reason="NATIVE_ASSET_MISSING_C3_FALLBACK";
            }
            if(applied==null) {
                applyClip(model,state,frame.sampledClip(),frame.attackLegWeight(),
                        selection);
                applied=frame.sampledClip().clipId();
                renderer="C3";
            }
        } else if (selection.renderer() == WeaponPresentationResolver.Renderer.C3
                && action.phase() == ActionTimeline.Phase.DODGING) {
            // Tuck the body into a crouched roll, then release naturally.
            model.body.xRot -= 0.70F * w;
            model.head.xRot += 0.45F * w;
            model.rightArm.xRot -= 1.30F * w;
            model.leftArm.xRot -= 1.30F * w;
            model.rightArm.zRot -= 0.55F * w;
            model.leftArm.zRot += 0.55F * w;
            model.rightLeg.xRot += 0.95F * w;
            model.leftLeg.xRot += 0.95F * w;
        } else if (selection.renderer() == WeaponPresentationResolver.Renderer.C3
                && action.kind() == ActionTimeline.Kind.LIGHT_COMBO) {
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

        if (frame.sampledClip() == null
                && selection.renderer() == WeaponPresentationResolver.Renderer.C3
                && (action.kind() == ActionTimeline.Kind.LIGHT_COMBO
                        || action.phase() == ActionTimeline.Phase.DODGING)) {
            renderer="C2_FALLBACK";
            applied="mkm:c2/legacy_pose";
            reason="C3_ASSET_MISSING";
        }
        if(Minecraft.getInstance().level!=null){
            double changed=AnimationRenderProbe.maxAngleDelta(
                    vanillaAngles,snapshotAngles(model));
            AnimationRenderProbe.markDecision(
                    Minecraft.getInstance().level.getGameTime(),applied,changed,
                    renderer,selection,reason);
        }

        // The jacket and sleeves are separate cubes in vanilla's player model.
        // Keep their rotations aligned with modified skeleton parts.
        copyAngles(model.rightSleeve, model.rightArm);
        copyAngles(model.leftSleeve, model.leftArm);
        copyAngles(model.rightPants, model.rightLeg);
        copyAngles(model.leftPants, model.leftLeg);
        copyAngles(model.jacket, model.body);
    }

    private static void applyClip(
            PlayerModel model, AvatarRenderState state,
            com.eretenkodaniil.mkm.client.animation.AnimationController.Frame clip,
            float attackLegWeight,WeaponPresentationResolver.Selection selection) {
        boolean left = state.mainArm == HumanoidArm.LEFT;
        boolean attackClip = clip.clipId().startsWith("mkm:sword/light_")
                || clip.clipId().startsWith("mkm:unarmed/");
        boolean guardOffhand=selection.offFamily()!=WeaponPresentationResolver.Family.EMPTY;
        for (var entry : clip.rotations().entrySet()) {
            AnimationClip.Bone bone = entry.getKey();
            // Offhand sword/shield remains held: it is not the attack hand.
            if (guardOffhand && attackClip && bone == AnimationClip.Bone.LEFT_ARM) continue;
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

    private static float[] snapshotAngles(PlayerModel model) {
        ModelPart[] parts = {model.head, model.body, model.leftArm, model.rightArm,
                model.leftLeg, model.rightLeg};
        float[] angles = new float[parts.length * 3];
        for (int i = 0; i < parts.length; i++) {
            angles[3 * i] = parts[i].xRot;
            angles[3 * i + 1] = parts[i].yRot;
            angles[3 * i + 2] = parts[i].zRot;
        }
        return angles;
    }

    private static void copyAngles(ModelPart overlay, ModelPart source) {
        overlay.x = source.x;
        overlay.y = source.y;
        overlay.z = source.z;
        overlay.xRot = source.xRot;
        overlay.yRot = source.yRot;
        overlay.zRot = source.zRot;
    }
}
