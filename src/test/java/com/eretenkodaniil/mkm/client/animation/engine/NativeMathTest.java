package com.eretenkodaniil.mkm.client.animation.engine;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class NativeMathTest {
    @Test void quaternionRoundtripAndShortestSlerp(){
        Quat q=Quat.euler(.5,-.3,.4);
        Vec3 restored=q.toEuler();
        assertEquals(.5,restored.x(),1e-10);
        assertEquals(-.3,restored.y(),1e-10);
        assertEquals(.4,restored.z(),1e-10);
        Quat a=Quat.euler(0,0,0), b=Quat.euler(0,0,Math.PI);
        Vec3 mid=a.slerp(b,.5).rotate(new Vec3(1,0,0));
        assertEquals(0,mid.x(),1e-8);
        assertEquals(1,Math.abs(mid.y()),1e-8);
        Vec3 unit=Quat.IDENTITY.slerp(q,.4).rotate(new Vec3(0,0,1));
        assertEquals(1,Math.sqrt(unit.x()*unit.x()+unit.y()*unit.y()+unit.z()*unit.z()),1e-8);
        assertThrows(IllegalArgumentException.class,()->new Quat(0,0,0,0));
        assertThrows(IllegalArgumentException.class,()->Quat.euler(Double.NaN,0,0));
    }
    @Test void hierarchicalTransformsPreserveScaleAndRotation(){
        Transform parent=new Transform(new Vec3(1,2,3),Quat.euler(0,0,Math.PI/2),new Vec3(2,2,2));
        Transform child=new Transform(new Vec3(1,0,0),Quat.IDENTITY,Vec3.ONE);
        Transform world=parent.compose(child);
        assertEquals(1,world.translation().x(),1e-7);
        assertEquals(4,world.translation().y(),1e-7);
        assertEquals(3,world.translation().z(),1e-7);
        assertThrows(IllegalArgumentException.class,()->new Transform(Vec3.ZERO,Quat.IDENTITY,new Vec3(-1,1,1)));
        assertThrows(IllegalArgumentException.class,()->new Transform(Vec3.ZERO,Quat.IDENTITY,new Vec3(1,2,1)));

    }
    @Test void internalKeysRetainVelocityWithoutTranslationOvershoot() {
        var track=java.util.List.of(
                new NativeClip.Key(0,new Transform(new Vec3(0,0,0),Quat.euler(0,0,0),Vec3.ONE)),
                new NativeClip.Key(.5,new Transform(new Vec3(1,0,0),Quat.euler(0,.3,0),Vec3.ONE)),
                new NativeClip.Key(1,new Transform(new Vec3(2,0,0),Quat.euler(0,.6,0),Vec3.ONE)));
        var clip=new NativeClip("mkm:test/continuous",14,java.util.Map.of("body",track),java.util.List.of());
        double e=.0001;
        var before=PoseSampler.sample(clip,.5-e).get("body");
        var after=PoseSampler.sample(clip,.5+e).get("body");
        assertEquals(1,PoseSampler.sample(clip,.5).get("body").translation().x(),1e-8);
        assertTrue((after.translation().x()-before.translation().x())/(2*e)>1.5);
        assertTrue((after.rotation().toEuler().y()-before.rotation().toEuler().y())/(2*e)>.35);
        for(int i=0;i<=1000;i++){
            double x=PoseSampler.sample(clip,i/1000D).get("body").translation().x();
            assertTrue(x>=0&&x<=2);
        }
    }

}
