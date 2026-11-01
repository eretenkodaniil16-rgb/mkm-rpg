package com.eretenkodaniil.mkm.client.animation.engine;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class GripSpaceMathTest {
    private static void near(Vec3 expected,Vec3 actual) {
        assertEquals(expected.x(),actual.x(),1e-8);
        assertEquals(expected.y(),actual.y(),1e-8);
        assertEquals(expected.z(),actual.z(),1e-8);
    }
    @Test void rotatedTorsoMovesArmPivotAndWeaponSocketTogether() {
        Skeleton rig=Skeleton.vanilla();
        Transform torso=new Transform(Vec3.ZERO,Quat.euler(0,0,Math.PI/2),Vec3.ONE);
        Transform socket=new Transform(new Vec3(0,6,0),Quat.IDENTITY,Vec3.ONE);
        Transform grip=new Transform(new Vec3(0,0,2),Quat.IDENTITY,Vec3.ONE);
        Transform world=new Transform(new Vec3(10,20,30),Quat.IDENTITY,Vec3.ONE);
        Transform result=GripSpaceMath.worldGrip(rig,Map.of("body",torso),
                "right_arm",socket,grip,world);
        // Right shoulder bind pivot=(5,-2,0); after +90° Z rotation => (2,5,0).
        // Socket=(0,6,0) rotates to (-6,0,0), weapon grip=(0,0,2).
        near(new Vec3(6,25,32),result.translation());
    }
    @Test void handednessSwitchChangesWorldGripButNeverMirrorsGameplay() {
        Skeleton rig=Skeleton.vanilla();
        Transform socket=new Transform(new Vec3(0,6,0),Quat.IDENTITY,Vec3.ONE);
        Transform right=GripSpaceMath.worldGrip(rig,Map.of(),"right_arm",
                socket,Transform.IDENTITY,Transform.IDENTITY);
        Transform left=GripSpaceMath.worldGrip(rig,Map.of(),"left_arm",
                socket,Transform.IDENTITY,Transform.IDENTITY);
        near(new Vec3(5,4,0),right.translation());
        near(new Vec3(-5,4,0),left.translation());
        assertEquals(right.translation().y(),left.translation().y(),1e-8);
    }
    @Test void gripRotationComposesRatherThanAddingEulerChannels() {
        Skeleton rig=Skeleton.vanilla();
        Transform arm=new Transform(Vec3.ZERO,Quat.euler(.2,.4,.1),Vec3.ONE);
        Transform socket=new Transform(new Vec3(1,3,0),Quat.euler(0,.3,0),Vec3.ONE);
        Transform item=new Transform(new Vec3(0,0,4),Quat.euler(0,0,-.5),Vec3.ONE);
        Transform actual=GripSpaceMath.worldGrip(rig,Map.of("right_arm",arm),
                "right_arm",socket,item,Transform.IDENTITY);
        Transform expected=rig.world("right_arm",Map.of("right_arm",arm)).compose(socket).compose(item);
        near(expected.translation(),actual.translation());
        Vec3 dir=new Vec3(0,0,1);
        near(expected.rotation().rotate(dir),actual.rotation().rotate(dir));
    }
}
