package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.client.animation.engine.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import java.util.*;

/**
 * Draws actual articulated 17-bone R07 geometry, NOT six-part rotations.
 * The static baked mesh is built ONCE per renderer. Every frame updates
 * independent root-space bone world transforms exported by Blender after IK.
 *
 * This is an experimental no-armor, local-player layer: it is not allowed to
 * duplicate normal armor or offer unvalidated multiplayer replication.
 */
public final class ArticulatedAvatarLayer
        extends RenderLayer<AvatarRenderState,PlayerModel> {
    private final ModelPart rigRoot;
    private final Map<String,ModelPart> joints;

    public ArticulatedAvatarLayer(RenderLayerParent<AvatarRenderState,PlayerModel> parent) {
        super(parent);
        ArticulatedClipV3 clip=ArticulatedMode.clip();
        if(clip==null){rigRoot=null;joints=Map.of();return;}
        MeshDefinition mesh=new MeshDefinition();
        Map<String,CubeListBuilder> parts=new LinkedHashMap<>();
        for(ArticulatedClipV3.Bone bone:clip.bones())
            parts.put(bone.id(),CubeListBuilder.create());
        for(ArticulatedClipV3.Mesh box:clip.meshes()){
            Vec3 min=box.min(),s=box.size();
            parts.get(box.bone()).texOffs(box.u(),box.v())
                .addBox((float)min.x(),(float)min.y(),(float)min.z(),
                        (float)s.x(),(float)s.y(),(float)s.z());
        }
        for(var e:parts.entrySet())
            mesh.getRoot().addOrReplaceChild(e.getKey(),e.getValue(),PartPose.ZERO);
        rigRoot=LayerDefinition.create(mesh,64,64).bakeRoot();
        Map<String,ModelPart> entries=new HashMap<>();
        for(String bone:parts.keySet())entries.put(bone,rigRoot.getChild(bone));
        joints=Map.copyOf(entries);
    }

    @Override
    public void submit(PoseStack matrix,SubmitNodeCollector collector,int light,
                       AvatarRenderState state,float yaw,float pitch){
        LocalCombatAnimation.Frame combat=LocalCombatAnimation.frame(state);
        if(!ArticulatedMode.active(state,combat)||rigRoot==null)return;
        ArticulatedClipV3 clip=ArticulatedMode.clip();
        double phase=combat.sampledClip().localTime();
        var pose=clip.sample(phase);
        boolean left=state.mainArm==HumanoidArm.LEFT;
        for(var e:joints.entrySet()){
            String source=left?swap(e.getKey()):e.getKey();
            ArticulatedClipV3.Sample animated=pose.get(source);
            if(animated==null)continue;
            Vec3 p=animated.pivot();Quat q=animated.rotation();
            ModelPart model=e.getValue();
            model.x=(float)(left?-p.x():p.x());
            model.y=(float)p.y();
            model.z=(float)p.z();
            // Mirror across avatar model x=0, preserving proper rotation.
            if(left)q=new Quat(q.x(),-q.y(),-q.z(),q.w());
            Vec3 angles=q.toEuler();
            model.xRot=(float)angles.x();
            model.yRot=(float)angles.y();
            model.zRot=(float)angles.z();
        }
        collector.submitModelPart(rigRoot,matrix,
                RenderTypes.entityTranslucent(state.skin.body().texturePath()),
                light,OverlayTexture.NO_OVERLAY,null);
    }

    private static String swap(String id){
        if(id.endsWith(".R"))return id.substring(0,id.length()-2)+".L";
        if(id.endsWith(".L"))return id.substring(0,id.length()-2)+".R";
        return id;
    }
}
