package com.eretenkodaniil.mkm.client.animation.engine;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class NativePlayerTest {
    private static NativeClip clip(String name,double yaw) {
        return new NativeClip(name,10,Map.of("body",List.of(
                new NativeClip.Key(0,Transform.IDENTITY),
                new NativeClip.Key(1,new Transform(Vec3.ZERO,Quat.euler(0,yaw,0),Vec3.ONE)))),
                List.of());
    }
    @Test void transitionsInterruptionsAndSpeedAreControlled(){
        var a=clip("mkm:test/a",.4);
        var b=clip("mkm:test/b",-.4);
        AnimationPlayer player=new AnimationPlayer();
        assertFalse(player.playing());
        player.play(a,100,0);
        assertEquals("mkm:test/a",player.clipId());
        assertEquals(.4,player.sample(110).get("body").rotation().toEuler().y(),1e-6);
        player.play(b,110,2);
        assertEquals(.4,player.sample(110).get("body").rotation().toEuler().y(),1e-6);
        assertEquals(0,player.sample(111).get("body").rotation().toEuler().y(),.01);
        assertEquals(-.4,player.sample(120).get("body").rotation().toEuler().y(),1e-6);
        player.speed(2);
        assertThrows(IllegalArgumentException.class,()->player.speed(0));
        player.stop();assertFalse(player.playing());
        assertEquals(Map.of(),player.sample(900));
    }
    @Test void interpolationClampsAndRefusesBadInput(){
        NativeClip a=clip("mkm:test/clip",.2);
        assertEquals(PoseSampler.sample(a,1),PoseSampler.sample(a,10));
        assertEquals(PoseSampler.sample(a,0),PoseSampler.sample(a,Double.NaN));
        assertThrows(IllegalArgumentException.class,()->new NativeClip(
                "mkm:bad",10,Map.of("body",List.of(
                        new NativeClip.Key(.5,Transform.IDENTITY),
                        new NativeClip.Key(1,Transform.IDENTITY))),List.of()));
    }
}
