package com.eretenkodaniil.mkm.client.animation;
import com.eretenkodaniil.mkm.client.animation.engine.*;
import com.eretenkodaniil.mkm.combat.ActionTimeline;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import java.util.*;
/** Only one renderer path applies per avatar frame. Cosmetic changes never move hitboxes. */
public final class VanillaAvatarAdapter {
    private VanillaAvatarAdapter(){}
    /**
     * Prototype MNAE is deliberately restricted to unarmed and tagged swords.
     * Axes, shields, mining tools and items use the existing C3 fallback;
     * Equipment Core can later supply stable per-weapon presentation profiles.
     */
    public static String nativeClip(ActionTimeline timeline, ItemStack mainHand) {
        if (timeline == null || mainHand == null
                || timeline.kind() != ActionTimeline.Kind.LIGHT_COMBO
                || timeline.comboStep() != 1) return null;
        if (mainHand.isEmpty()) return NativeDemoLibrary.JAB;
        return mainHand.is(ItemTags.SWORDS) ? NativeDemoLibrary.SWORD : null;
    }

    public static String apply(PlayerModel model,AvatarRenderState state,
                               LocalCombatAnimation.Frame frame, ItemStack mainHand){
        if(frame==null || frame.sampledClip()==null) return null;
        String clipId=nativeClip(frame.timeline(),mainHand);
        if(clipId==null)return null;
        NativeClip clip=clipId.equals(NativeDemoLibrary.JAB)
                ? NativeDemoLibrary.jab() : NativeDemoLibrary.sword();
        if(clip==null)return null;
        // Use EXACT server phase mapping inherited from C3; there is no client
        // authority to change start, hit, recovery or total action duration.
        double time=frame.sampledClip().localTime();
        Map<String,Transform> authored=PoseSampler.sample(clip,time);
        // The C3.8 model remains the single base. Only this MNAE path executes.
        Map<String,Transform> weighted=new HashMap<>();
        for(var e:authored.entrySet()){
            boolean leg=e.getKey().endsWith("_leg");
            double weight=leg?frame.attackLegWeight():1.0;
            weighted.put(e.getKey(),Transform.IDENTITY.mix(e.getValue(),weight));
        }
        Map<String,Transform> pose=PoseMixer.mix(Map.of(),
                List.of(new AnimationLayer(weighted,10,Set.of(),1,AnimationLayer.Mode.ADDITIVE)));
        boolean leftHanded=state.mainArm==HumanoidArm.LEFT;
        // Vanilla's head/arms are *siblings*, not children of a real pelvis.
        // Carry the small authored torso translation to upper pieces to avoid
        // visibly detaching the helmet or shoulders from the chest.
        Vec3 torsoShift=pose.getOrDefault("body",Transform.IDENTITY).translation();
        for(var e:pose.entrySet()){
            String bone=e.getKey();
            String target=leftHanded?swapSide(bone):bone;
            ModelPart part=part(model,target);
            if(part==null)continue;
            Transform t=e.getValue();
            Vec3 delta=t.translation();
            if(bone.equals("head")||bone.endsWith("_arm")) delta=delta.add(torsoShift);
            Vec3 angle=t.rotation().toEuler();
            float mirror=leftHanded?-1F:1F;
            // Vanilla ModelPart transformations use model-pixel translations.
            // Limit cosmetic pivot shifts to avoid armor/head separation.
            part.x+=(float)Math.clamp(delta.x()*mirror,-1.5,1.5);
            part.y+=(float)Math.clamp(delta.y(),-1.5,1.5);
            part.z+=(float)Math.clamp(delta.z(),-1.5,1.5);
            part.xRot+=(float)angle.x();
            part.yRot+=(float)angle.y()*mirror;
            part.zRot+=(float)angle.z()*mirror;
        }
        return clipId;
    }
    private static String swapSide(String id){
        return switch(id){
            case "left_arm"->"right_arm";case "right_arm"->"left_arm";
            case "left_leg"->"right_leg";case "right_leg"->"left_leg";
            default->id;
        };
    }
    private static ModelPart part(PlayerModel model,String id){
        return switch(id){
            case "head"->model.head;case "body"->model.body;
            case "left_arm"->model.leftArm;case "right_arm"->model.rightArm;
            case "left_leg"->model.leftLeg;case "right_leg"->model.rightLeg;
            default->null;
        };
    }
}
