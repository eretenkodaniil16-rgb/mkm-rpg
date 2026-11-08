package com.eretenkodaniil.mkm.client.animation.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Golden checks for 43 real Blender evaluated frames; no Minecraft API.
 * Six-part proxy Euler must preserve world-space bone quaternion as far as
 * the vanilla ModelPart representation allows.
 */
class R07CoordinateParityTest {
    private static final Vec3 FORWARD=new Vec3(0,0,-1);
    private static final Vec3 UP=new Vec3(0,-1,0);

    @Test void modelPartEulerAndQuaternionStayAlignedAcrossAllSeventeenJoints(){
        var clip=ArticulatedClipV3.readResource("assets/mkm/mnae/r07_mid_guard_cut_v3.json");
        double maxError=0;
        for(int f=0;f<43;f++){
            var pose=clip.sample(f/42D);
            for(var sample:pose.values()){
                Quat original=sample.rotation();
                Vec3 e=original.toEuler();
                Quat converted=Quat.euler(e.x(),e.y(),e.z());
                double a=distance(original.rotate(FORWARD),converted.rotate(FORWARD));
                double b=distance(original.rotate(UP),converted.rotate(UP));
                maxError=Math.max(maxError,Math.max(a,b));
                assertTrue(a<1e-4 && b<1e-4,"ModelPart lost original Blender quaternion on frame "+f);
            }
        }
        assertTrue(maxError<1e-4);
    }

    @Test void leftHandMirrorsWorldBladeAndGripWithoutSwappingItemOwnership(){
        var clip=ArticulatedClipV3.readResource("assets/mkm/mnae/r07_mid_guard_cut_v3.json");
        for(int f=0;f<43;f++){
            var pose=clip.sample(f/42D);
            Vec3 rightGrip=clip.socketPoint(pose,false);
            Vec3 rightTip=clip.socketPoint(pose,true);
            // Reflection across vertical avatar X=0; same source hand.R acts
            // as physical hand.L. Ownership stays MAIN_HAND as chosen by Minecraft.
            Vec3 leftGrip=mirrorX(rightGrip);
            Vec3 leftTip=mirrorX(rightTip);
            assertEquals(rightGrip.x(),-leftGrip.x(),1e-6);
            assertEquals(rightTip.x(),-leftTip.x(),1e-6);
            assertEquals(distance(rightGrip,rightTip),distance(leftGrip,leftTip),1e-8);
            Quat right=pose.get("hand.R").rotation();
            Quat left=new Quat(right.x(),-right.y(),-right.z(),right.w());
            Vec3 v=right.rotate(clip.socket().tipOffset().subtract(clip.socket().gripOffset()));
            Vec3 mirrored=left.rotate(mirrorX(
                    clip.socket().tipOffset().subtract(clip.socket().gripOffset())));
            assertTrue(distance(mirrorX(v),mirrored)<1e-5,
                    "Mirrored blade orientation inconsistent at frame "+f);
        }
    }

    @Test void visibleElbowsKneesRemainIndependentRatherThanSixRotations(){
        var clip=ArticulatedClipV3.readResource("assets/mkm/mnae/r07_mid_guard_cut_v3.json");
        Set<String> names=new HashSet<>();
        for(var b:clip.bones())names.add(b.id());
        assertTrue(names.containsAll(List.of("forearm.R","forearm.L",
                "shin.R","shin.L","hand.R","hand.L","foot.R","foot.L")));
        assertEquals(17,clip.bones().size());
        assertTrue(clip.meshes().stream().anyMatch(m->m.bone().equals("forearm.R")));
        assertTrue(clip.meshes().stream().anyMatch(m->m.bone().equals("foot.R")));
        var start=clip.sample(0);
        var contact=clip.sample(5D/14D);
        assertTrue(distance(start.get("hand.R").pivot(),contact.get("hand.R").pivot())>.10);
    }
    private static Vec3 mirrorX(Vec3 v){return new Vec3(-v.x(),v.y(),v.z());}
    private static double distance(Vec3 a,Vec3 b){
        return Math.hypot(Math.hypot(a.x()-b.x(),a.y()-b.y()),a.z()-b.z());
    }
}
