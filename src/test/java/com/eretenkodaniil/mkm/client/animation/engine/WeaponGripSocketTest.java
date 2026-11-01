package com.eretenkodaniil.mkm.client.animation.engine;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponGripSocketTest {
    private static Skeleton rig(){
        return new Skeleton(List.of(
                new Skeleton.Bone("body",null,Vec3.ZERO,Transform.IDENTITY),
                new Skeleton.Bone("right_arm","body",new Vec3(5,2,0),Transform.IDENTITY),
                new Skeleton.Bone("left_arm","body",new Vec3(-5,2,0),Transform.IDENTITY),
                new Skeleton.Bone("head","body",new Vec3(0,-8,0),Transform.IDENTITY)));
    }
    @Test void neutralRightLeftHandsHaveDistinctGripSockets(){
        var right=WeaponGripSocket.sample(rig(),Map.of(),"right_arm",WeaponGripSocket.TEST_SWORD);
        var left=WeaponGripSocket.sample(rig(),Map.of(),"left_arm",WeaponGripSocket.TEST_SWORD);
        assertEquals(5,right.handle().x(),1e-8);
        assertEquals(-5,left.handle().x(),1e-8);
        assertEquals(12,right.handle().y(),1e-8);
        assertEquals(-2,right.bladeTip().y(),1e-8);
        assertEquals(14,Math.abs(right.handle().y()-right.bladeTip().y()),1e-8);
    }
    @Test void movingTorsoPropagatesToHandleAndBladeTip(){
        var p=Map.of("body",new Transform(Vec3.ZERO,Quat.euler(0,0,Math.PI/2),Vec3.ONE));
        var start=WeaponGripSocket.sample(rig(),Map.of(),"right_arm",WeaponGripSocket.TEST_SWORD);
        var rotate=WeaponGripSocket.sample(rig(),p,"right_arm",WeaponGripSocket.TEST_SWORD);
        assertEquals(-12,rotate.handle().x(),1e-7);
        assertEquals(5,rotate.handle().y(),1e-7);
        assertNotEquals(start.handle().x(),rotate.handle().x());
        assertEquals(14,Math.hypot(rotate.bladeTip().x()-rotate.handle().x(),
                rotate.bladeTip().y()-rotate.handle().y()),1e-7);
    }
    @Test void bladeClearanceComputationIsFiniteAndRejectsBadBones(){
        var g=WeaponGripSocket.sample(rig(),Map.of(),"right_arm",WeaponGripSocket.TEST_SWORD);
        assertEquals(0,WeaponGripSocket.pointToBlade(g.handle(),g),1e-8);
        assertTrue(WeaponGripSocket.pointToBlade(new Vec3(100,100,100),g)>100);
        assertThrows(IllegalArgumentException.class,
                ()->WeaponGripSocket.sample(rig(),Map.of(),"head",WeaponGripSocket.TEST_SWORD));
    }
}
