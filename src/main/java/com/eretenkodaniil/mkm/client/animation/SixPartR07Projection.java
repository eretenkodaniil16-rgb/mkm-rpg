package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.client.animation.engine.ArticulatedClipV3;
import com.eretenkodaniil.mkm.client.animation.engine.Quat;
import com.eretenkodaniil.mkm.client.animation.engine.Vec3;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import java.util.Map;

/**
 * Controlled six-part comparator driven by the SAME R07 source/clock as the
 * articulated layer. Deliberately loses elbows, knees and all internal pivots.
 * This is NOT the historical six-part MNAE clip; label it "R07 six projection".
 */
public final class SixPartR07Projection {
    private SixPartR07Projection(){}
    public static void apply(PlayerModel model,AvatarRenderState state,ArticulatedClipV3 clip,double t) {
        Map<String,ArticulatedClipV3.Sample> pose=clip.sample(t);
        boolean left=state.mainArm==HumanoidArm.LEFT;
        add(model.body,pose.get("spine").rotation(),false,false);
        add(model.head,pose.get("head").rotation(),false,left);
        add(left?model.leftArm:model.rightArm,pose.get("upper_arm.R").rotation(),false,left);
        add(left?model.rightArm:model.leftArm,pose.get("upper_arm.L").rotation(),false,left);
        add(left?model.leftLeg:model.rightLeg,pose.get("thigh.R").rotation(),false,left);
        add(left?model.rightLeg:model.leftLeg,pose.get("thigh.L").rotation(),false,left);
    }
    private static void add(ModelPart part,Quat q,boolean ignored,boolean mirror){
        Vec3 angle=q.toEuler();
        part.xRot+=(float)angle.x();
        part.yRot+=(float)(mirror?-angle.y():angle.y());
        part.zRot+=(float)(mirror?-angle.z():angle.z());
    }
}
