package com.eretenkodaniil.mkm.client.animation;

import static org.junit.jupiter.api.Assertions.*;
import com.eretenkodaniil.mkm.combat.DodgeDirection;
import org.junit.jupiter.api.Test;

class DodgeVisualChoreographyTest {
    @Test void threeDodgeFamiliesAreVisuallyDifferentAndNoRigidSpin(){
        var f=DodgeVisualChoreography.sample(DodgeDirection.FORWARD,.5F);
        var side=DodgeVisualChoreography.sample(DodgeDirection.LEFT,.5F);
        var back=DodgeVisualChoreography.sample(DodgeDirection.BACKWARD,.5F);
        assertEquals(DodgeVisualChoreography.Family.FORWARD_TUCK,f.family());
        assertEquals(DodgeVisualChoreography.Family.SIDE_SLIDE,side.family());
        assertEquals(DodgeVisualChoreography.Family.BACK_HOP,back.family());
        assertTrue(f.pitch()<-.9F);
        assertTrue(Math.abs(side.bank())>.4F);
        assertTrue(back.lift()>.10F);
        assertTrue(Math.abs(f.pitch())<2);
        assertNotEquals(f,side);
        assertNotEquals(side,back);
    }
    @Test void allEightFinishExactlyNeutralAndHaveBoundedPose(){
        for(byte d=0;d<=DodgeDirection.LAST;d++){
            for(int i=0;i<=100;i++){
                float t=i/100F;
                var p=DodgeVisualChoreography.sample(d,t);
                assertTrue(Float.isFinite(p.pitch()));
                assertTrue(Float.isFinite(p.yaw()));
                assertTrue(Float.isFinite(p.bank()));
                assertTrue(Float.isFinite(p.lift()));
                assertTrue(Math.abs(p.pitch())<=1.05F);
                assertTrue(Math.abs(p.bank())<=.47F);
            }
            assertEquals(0,DodgeVisualChoreography.sample(d,0).pitch(),1e-6);
            assertEquals(0,DodgeVisualChoreography.sample(d,1).pitch(),1e-6);
            assertEquals(0,DodgeVisualChoreography.sample(d,1).bank(),1e-6);
        }
    }
}
