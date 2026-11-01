package com.eretenkodaniil.mkm.client.animation.engine;

import com.eretenkodaniil.mkm.combat.DirectionalRollChoreography;
import com.eretenkodaniil.mkm.combat.ForwardRollChoreography;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * P0 diagnostic only: an ideal rectangular silhouette staying at y >= 0
 * does not imply planted hands/feet, real terrain contact or safe camera.
 */
class LegacyRollPresentationAuditTest {
    @Test void allEightDirectionalRollsHaveFiniteReferenceFloorProjection(){
        for(byte direction=0;direction<8;direction++) {
            for(int i=0;i<=200;i++){
                float t=i/200F;
                var pose=DirectionalRollChoreography.sample(direction,t);
                assertTrue(Float.isFinite(pose.radians()),"direction="+direction);
                assertTrue(Float.isFinite(pose.visualYOffset()),"direction="+direction);
                assertTrue(Math.abs(pose.radians())<=2*Math.PI+1e-4);
                assertTrue(DirectionalRollChoreography.silhouetteMinHeight(pose)>-.001,
                        "Approximation only, not true world contact");
            }
        }
    }
    @Test void forwardRollIsFullRigidRotationNotArticulatedShoulderContact(){
        double previous=0;
        for(int i=0;i<=200;i++){
            var pose=ForwardRollChoreography.sample(i/200F);
            assertTrue(ForwardRollChoreography.silhouetteMinHeight(pose)>-.001);
            assertTrue(Double.isFinite(pose.radians()));
            assertTrue(pose.radians()+1e-5>=previous);
            previous=pose.radians();
        }
        assertEquals(2*Math.PI,previous,.00001);
        // Full rigid 360° is the known choreography defect. This test
        // characterizes it to prevent unreviewed behavior changes.
    }
}
