package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.client.animation.engine.ArticulatedClipV3;
import com.eretenkodaniil.mkm.client.animation.engine.Quat;
import com.eretenkodaniil.mkm.client.animation.engine.Vec3;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.world.entity.HumanoidArm;
import java.util.Map;

/**
 * Diagnostic MNAE six-part reduction of the EXACT R07 timeline.
 * This is deliberately NOT described as the historical MNAE M1 clips:
 * elbows, knees, wrist sockets disappear in this comparator.
 */
public final class R07SixPartProxy {
    private R07SixPartProxy(){}
    public static void apply(PlayerModel model,HumanoidArm mainArm,
                             ArticulatedClipV3 clip,double time){
        Map<String,ArticulatedClipV3.Sample> joints=clip.sample(time);
        boolean mirror=mainArm==HumanoidArm.LEFT;
        add(model.body,joints.get("spine").rotation(),mirror,.65F);
        add(model.head,joints.get("head").rotation(),mirror,.35F);
        add(model.rightArm,joints.get(mirror?"upper_arm.L":"upper_arm.R").rotation(),mirror,.75F);
        add(model.leftArm,joints.get(mirror?"upper_arm.R":"upper_arm.L").rotation(),mirror,.75F);
        add(model.rightLeg,joints.get(mirror?"thigh.L":"thigh.R").rotation(),mirror,.40F);
        add(model.leftLeg,joints.get(mirror?"thigh.R":"thigh.L").rotation(),mirror,.40F);
        copy(model.rightSleeve,model.rightArm);
        copy(model.leftSleeve,model.leftArm);
        copy(model.rightPants,model.rightLeg);
        copy(model.leftPants,model.leftLeg);
        copy(model.jacket,model.body);
    }
    private static void add(ModelPart p,Quat q,boolean mirrored,float strength){
        if(mirrored)q=new Quat(q.x(),-q.y(),-q.z(),q.w());
        Vec3 e=q.toEuler();
        p.xRot+=(float)Math.clamp(e.x(),-1.6,1.6)*strength;
        p.yRot+=(float)Math.clamp(e.y(),-1.6,1.6)*strength;
        p.zRot+=(float)Math.clamp(e.z(),-1.6,1.6)*strength;
    }
    private static void copy(ModelPart overlay,ModelPart source){
        overlay.x=source.x;overlay.y=source.y;overlay.z=source.z;
        overlay.xRot=source.xRot;overlay.yRot=source.yRot;overlay.zRot=source.zRot;
    }
}
