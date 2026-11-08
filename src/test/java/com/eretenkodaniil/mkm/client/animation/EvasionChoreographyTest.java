package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.DodgeDirection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EvasionChoreographyTest {
    @Test void threeDifferentBoundedChoreographiesNoRigidSpins(){
        var front=EvasionChoreography.sample(DodgeDirection.FORWARD,.5);
        var side=EvasionChoreography.sample(DodgeDirection.LEFT,.5);
        var back=EvasionChoreography.sample(DodgeDirection.BACKWARD,.5);
        assertEquals(EvasionChoreography.Kind.FORWARD_TUCK,front.kind());
        assertEquals(EvasionChoreography.Kind.LATERAL_STEP,side.kind());
        assertEquals(EvasionChoreography.Kind.BACKWARD_HOP,back.kind());
        assertTrue(front.pitch()<-.5F);
        assertTrue(side.roll()<-.20F);
        assertTrue(back.pitch()>.20F);
        for(byte direction=0;direction<=DodgeDirection.LAST;direction++){
            var start=EvasionChoreography.sample(direction,0D);
            var end=EvasionChoreography.sample(direction,1D);
            assertEquals(0,start.pitch(),.00001);
            assertEquals(0,end.roll(),.00001);
            EvasionChoreography.Pose previous=null;
            for(int i=0;i<=200;i++){
                var p=EvasionChoreography.sample(direction,i/200D);
                assertTrue(Math.abs(p.pitch())<1);
                assertTrue(Math.abs(p.roll())<.35);
                assertTrue(Math.abs(p.yaw())<.25);
                assertTrue(Math.abs(p.lift())<.12);
                if(previous!=null)assertTrue(Math.abs(previous.pitch()-p.pitch())<.02);
                previous=p;
            }
        }
    }
}
