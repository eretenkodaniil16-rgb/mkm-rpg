package com.eretenkodaniil.mkm.client.animation.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.util.*;

/**
 * These tests require the Blender v3 resource generated into src/main/resources
 * by the isolated R07 workflow BEFORE Gradle tests. No client or server starts.
 */
class ArticulatedClipV3Test {
    private ArticulatedClipV3 read(){
        return ArticulatedClipV3.readResource("assets/mkm/mnae/r07_mid_guard_cut_v3.json");
    }

    @Test void unmodifiedR06MidGuardOriginAndServerImpactAreVerifiable(){
        ArticulatedClipV3 clip=read();
        assertEquals("mkm:r07/mid_guard_sword_cut",clip.id());
        assertEquals(14,clip.durationTicks());
        assertEquals(43,clip.frameCount());
        assertEquals(17,clip.bones().size());
        assertTrue(clip.markers().stream().anyMatch(e->e.id().equals("impact")
                && e.frame()==16 && Math.abs(e.normalized()-5D/14D)<1e-6));
    }

    @Test void eachJointAndSocketIsFiniteAcrossWholeAction(){
        ArticulatedClipV3 clip=read();
        Set<String> ids=new HashSet<>();
        for(ArticulatedClipV3.Bone bone:clip.bones())ids.add(bone.id());
        assertTrue(ids.containsAll(Set.of("pelvis","spine","head","hand.R","hand.L",
                "upper_arm.R","forearm.R","upper_arm.L","forearm.L",
                "thigh.R","shin.R","foot.R","thigh.L","shin.L","foot.L")));
        assertTrue(clip.meshes().stream().anyMatch(m->m.bone().equals("forearm.R")));
        assertTrue(clip.meshes().stream().anyMatch(m->m.bone().equals("shin.R")));
        for(int frame=0;frame<=200;frame++){
            var p=clip.sample(frame/200D);
            assertEquals(17,p.size());
            for(var j:p.values()){
                assertTrue(Double.isFinite(j.pivot().x()));
                assertTrue(Double.isFinite(j.rotation().w()));
                assertEquals(1,Math.sqrt(Math.pow(j.rotation().x(),2)
                        +Math.pow(j.rotation().y(),2)+Math.pow(j.rotation().z(),2)
                        +Math.pow(j.rotation().w(),2)),1e-8);
            }
            Vec3 grip=clip.socketPoint(p,false);
            Vec3 tip=clip.socketPoint(p,true);
            assertTrue(Double.isFinite(grip.y()));
            assertTrue(Double.isFinite(tip.y()));
            assertTrue(Math.abs(grip.x()-tip.x())+Math.abs(grip.y()-tip.y())
                    +Math.abs(grip.z()-tip.z()) > 1);
        }
    }

    @Test void actualSeventeenArticulationsAreNotSixPartDownBake(){
        ArticulatedClipV3 clip=read();
        var first=clip.sample(0);
        var hit=clip.sample(5D/14D);
        boolean elbowMoved=false,kneeExists=false;
        for(String side:List.of("R","L")){
            elbowMoved |= !first.get("forearm."+side).pivot().equals(
                    hit.get("forearm."+side).pivot());
            kneeExists |= clip.sample(.5).containsKey("shin."+side);
        }
        assertTrue(elbowMoved,"Real elbow world position must change at impact");
        assertTrue(kneeExists,"Real knee channels may never be merged into upper leg");
    }

    @Test void parserHasFiniteResourceBudget() {
        byte[] oversized=new byte[1024*1024+1];
        Arrays.fill(oversized,(byte)' ');
        assertThrows(IllegalArgumentException.class,()->
             ArticulatedClipV3.read(new ByteArrayInputStream(oversized)));
    }
}
