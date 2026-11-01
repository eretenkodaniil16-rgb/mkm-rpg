package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.client.animation.engine.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import java.util.*;

/**
 * Cosmetic MNAE to six vanilla ModelParts. The body is treated as a virtual
 * parent: child pivots and orientations inherit its native motion. Native
 * clips never control server collision or item damage.
 *
 * Minecraft arms/head are actual sibling ModelParts. We bake hierarchy into
 * their local (model-root) transforms before the regular held-item layer runs.
 */
public final class VanillaAvatarAdapter {
    private VanillaAvatarAdapter() {}

    public static String apply(PlayerModel model, AvatarRenderState state,
                               LocalCombatAnimation.Frame frame) {
        if (frame == null || frame.sampledClip() == null
                || frame.selection().renderer() != WeaponPresentationResolver.Renderer.MNAE)
            return null;
        String id=frame.selection().nativeClipId();
        NativeClip clip="mkm:mnae/straight_punch".equals(id)
                ? NativeDemoLibrary.jab()
                : "mkm:mnae/sword_cut".equals(id) ? NativeDemoLibrary.sword() : null;
        if(clip==null)return null;

        // Server's action time is authoritative. No event marker deals damage.
        Map<String,Transform> authored=PoseSampler.sample(clip,frame.sampledClip().localTime());
        Map<String,Transform> faded=new HashMap<>();
        boolean protectOffhand=frame.selection().offFamily()
                != WeaponPresentationResolver.Family.EMPTY;
        for(var e:authored.entrySet()){
            boolean leg=e.getKey().endsWith("_leg");
            if(protectOffhand && e.getKey().equals("left_arm")) continue;
            faded.put(e.getKey(), Transform.IDENTITY.mix(e.getValue(),
                    leg?frame.attackLegWeight():1D));
        }
        Map<String,Transform> pose=PoseMixer.mix(Map.of(),
                List.of(new AnimationLayer(faded,10,Set.of(),1D,AnimationLayer.Mode.ADDITIVE)));
        applyHierarchicalPose(model,state,pose);
        return id;
    }

    /**
     * Shared pure skeleton mathematics are not discarded at the render bridge.
     * Animated body transform is propagated into head + both shoulder pivots
     * BEFORE mapping into vanilla's six independent parts.
     */
    static void applyHierarchicalPose(PlayerModel model,AvatarRenderState state,
                                      Map<String,Transform> pose) {
        Skeleton skeleton=fromVanilla(model);
        boolean left=state.mainArm==HumanoidArm.LEFT;
        for(String id:List.of("body","head","left_arm","right_arm","left_leg","right_leg")){
            if(!pose.containsKey(id) && !(pose.containsKey("body")
                    && (id.equals("head")||id.endsWith("_arm"))))continue;
            Transform world=skeleton.world(id,pose);
            Transform neutral=skeleton.world(id,Map.of());
            Vec3 delta=world.translation().subtract(neutral.translation());
            String target=left?mirrorName(id):id;
            ModelPart part=modelPart(model,target);
            if(part==null)continue;
            double sign=left?-1D:1D;
            part.x+=(float)Math.clamp(delta.x()*sign,-2D,2D);
            part.y+=(float)Math.clamp(delta.y(),-2D,2D);
            part.z+=(float)Math.clamp(delta.z(),-2D,2D);
            // World orientation includes the virtual parent. Compose the
            // cosmetic orientation with vanilla head look / movement pose.
            Vec3 e=world.rotation().toEuler();
            Quat worldMirror=Quat.euler(e.x(),e.y()*sign,e.z()*sign);
            Quat vanilla=Quat.euler(part.xRot,part.yRot,part.zRot);
            Vec3 result=worldMirror.multiply(vanilla).toEuler();
            part.xRot=(float)result.x();
            part.yRot=(float)result.y();
            part.zRot=(float)result.z();
        }
    }

    private static Skeleton fromVanilla(PlayerModel model){
        ModelPart body=model.body;
        Vec3 root=new Vec3(body.x,body.y,body.z);
        List<Skeleton.Bone> bones=new ArrayList<>();
        bones.add(new Skeleton.Bone("body",null,root,Transform.IDENTITY));
        for(String id:List.of("head","left_arm","right_arm")){
            ModelPart part=modelPart(model,id);
            bones.add(new Skeleton.Bone(id,"body",
                    new Vec3(part.x-body.x,part.y-body.y,part.z-body.z),Transform.IDENTITY));
        }
        for(String id:List.of("left_leg","right_leg")){
            ModelPart part=modelPart(model,id);
            bones.add(new Skeleton.Bone(id,null,
                    new Vec3(part.x,part.y,part.z),Transform.IDENTITY));
        }
        return new Skeleton(bones);
    }

    private static String mirrorName(String id){
        return switch(id){
            case "left_arm"->"right_arm";case "right_arm"->"left_arm";
            case "left_leg"->"right_leg";case "right_leg"->"left_leg";
            default->id;
        };
    }

    private static ModelPart modelPart(PlayerModel model,String id){
        return switch(id){
            case "head"->model.head;case "body"->model.body;
            case "left_arm"->model.leftArm;case "right_arm"->model.rightArm;
            case "left_leg"->model.leftLeg;case "right_leg"->model.rightLeg;
            default->null;
        };
    }
}
