package com.eretenkodaniil.mkm.client.animation.engine;
import java.util.*;
/** Per-avatar cosmetic playback. No gameplay authority or wall-clock timers. */
public final class AnimationPlayer {
    private NativeClip current;
    private double startTick,speed=1,fadeStart,fadeTicks;
    private Map<String,Transform> previous=Map.of();
    public void play(NativeClip clip,double now,double fadeTicks){
        Objects.requireNonNull(clip);
        if(!Double.isFinite(now)||!Double.isFinite(fadeTicks)||fadeTicks<0||fadeTicks>20)
            throw new IllegalArgumentException("Play time");
        previous=current==null?Map.of():sample(now);
        current=clip;startTick=now;fadeStart=now;this.fadeTicks=fadeTicks;
    }
    public void speed(double multiplier){
        if(!Double.isFinite(multiplier)||multiplier<=.1||multiplier>4)throw new IllegalArgumentException("Speed");
        speed=multiplier;
    }
    public void stop(){current=null;previous=Map.of();}
    public boolean playing(){return current!=null;}
    public String clipId(){return current==null?null:current.id();}
    public Map<String,Transform> sample(double now){
        if(current==null)return Map.of();
        if(!Double.isFinite(now))throw new IllegalArgumentException("Clock");
        double time=Math.clamp((now-startTick)*speed/current.ticks(),0,1);
        Map<String,Transform> nowPose=PoseSampler.sample(current,time);
        if(fadeTicks<=0||now>=fadeStart+fadeTicks)return nowPose;
        double alpha=Math.clamp((now-fadeStart)/fadeTicks,0,1);
        return PoseMixer.mix(previous,List.of(new AnimationLayer(nowPose,1,Set.of(),
                alpha,AnimationLayer.Mode.OVERRIDE)));
    }
}
