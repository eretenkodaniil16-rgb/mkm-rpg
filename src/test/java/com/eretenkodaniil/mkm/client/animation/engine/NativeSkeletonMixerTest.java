package com.eretenkodaniil.mkm.client.animation.engine;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class NativeSkeletonMixerTest {
    @Test void skeletonRejectsCyclesAndMissingParents(){
        assertThrows(IllegalArgumentException.class,()->new Skeleton(List.of(
                new Skeleton.Bone("a","b",Vec3.ZERO,Transform.IDENTITY),
                new Skeleton.Bone("b","a",Vec3.ZERO,Transform.IDENTITY))));
        assertThrows(IllegalArgumentException.class,()->new Skeleton(List.of(
                new Skeleton.Bone("a","missing",Vec3.ZERO,Transform.IDENTITY))));
        Skeleton s=Skeleton.vanilla();
        assertEquals(6,s.ids().size());
        assertTrue(s.world("head",Map.of()).translation().y()<0);
        assertThrows(IllegalArgumentException.class,()->s.world("nonexistent",Map.of()));
    }
    @Test void priorityMaskAndAdditiveLayerAreDeterministic(){
        Transform lean=new Transform(new Vec3(1,0,0),Quat.euler(0,.4,0),Vec3.ONE);
        Transform shield=new Transform(Vec3.ZERO,Quat.euler(-.2,0,0),Vec3.ONE);
        var low=new AnimationLayer(Map.of("body",lean,"right_arm",lean),1,
                Set.of("body"),1,AnimationLayer.Mode.OVERRIDE);
        var high=new AnimationLayer(Map.of("body",shield),10,Set.of("body"),1,AnimationLayer.Mode.ADDITIVE);
        Map<String,Transform> a=PoseMixer.mix(Map.of(),List.of(high,low));
        Map<String,Transform> b=PoseMixer.mix(Map.of(),List.of(low,high));
        assertEquals(a,b);
        assertEquals(1,a.size());
        assertEquals(1,a.get("body").translation().x(),1e-8);
        assertEquals(-.2,a.get("body").rotation().toEuler().x(),.02);
        assertThrows(IllegalArgumentException.class,()->new AnimationLayer(
                Map.of(),0,Set.of(),Double.NaN,AnimationLayer.Mode.ADDITIVE));
    }
}
