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
        assertTrue(player.sample(111).get("body").rotation().toEuler().y() > .12);
        assertTrue(player.sample(111).get("body").rotation().toEuler().y() < .30);
        assertEquals(-.4,player.sample(120).get("body").rotation().toEuler().y(),1e-6);
        player.setSpeedAt(2,120);
        assertThrows(IllegalArgumentException.class,()->player.speed(0));
        player.stop();assertFalse(player.playing());
        assertEquals(Map.of(),player.sample(900));
    }

    @Test void speedChangeAtTickDoesNotWarpCurrentAnimationPhase(){
        NativeClip action=clip("mkm:test/speed",.8);
        AnimationPlayer player=new AnimationPlayer();
        player.speed(1.5);
        player.play(action,100,0);
        double before=player.sample(103).get("body").rotation().toEuler().y();
        player.setSpeedAt(.5,103);
        double after=player.sample(103).get("body").rotation().toEuler().y();
        assertEquals(before,after,1e-9,"Changing speed must not teleport the pose");
        assertTrue(player.sample(105).get("body").rotation().toEuler().y()>before);
        assertThrows(IllegalStateException.class,()->player.speed(3));
        assertThrows(IllegalArgumentException.class,()->player.setSpeedAt(2,Double.NaN));
    }

    @Test void interruptAndFadeOutRemoveOldUnsharedBonesContinuously(){
        NativeClip arm=new NativeClip("mkm:test/arm",10,
                Map.of("right_arm",List.of(
                        new NativeClip.Key(0,new Transform(Vec3.ZERO,Quat.euler(0,0,.8),Vec3.ONE)),
                        new NativeClip.Key(1,new Transform(Vec3.ZERO,Quat.euler(0,0,.8),Vec3.ONE)))),
                List.of());
        NativeClip leg=new NativeClip("mkm:test/leg",10,
                Map.of("left_leg",List.of(
                        new NativeClip.Key(0,new Transform(Vec3.ZERO,Quat.euler(.4,0,0),Vec3.ONE)),
                        new NativeClip.Key(1,new Transform(Vec3.ZERO,Quat.euler(.4,0,0),Vec3.ONE)))),
                List.of());
        AnimationPlayer player=new AnimationPlayer();
        player.play(arm,50,0);
        player.play(leg,52,4);
        assertTrue(player.sample(54).containsKey("right_arm"),"Old bones still fade during transition");
        assertTrue(Math.abs(player.sample(54).get("right_arm").rotation().toEuler().z())<.8);
        assertEquals(Set.of("left_leg"),player.sample(56).keySet(),
                "After fade the outgoing bone must not linger");
        player.stop(57,4);
        assertTrue(player.playing());
        assertTrue(player.sample(59).containsKey("left_leg"));
        assertEquals(Map.of(),player.sample(61));
        assertFalse(player.playing());
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
