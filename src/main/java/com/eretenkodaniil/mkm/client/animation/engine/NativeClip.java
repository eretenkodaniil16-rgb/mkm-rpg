package com.eretenkodaniil.mkm.client.animation.engine;
import java.util.*;
import com.eretenkodaniil.mkm.client.animation.AnimationClip;
/** Bounded, immutable animation data. Events are PRESENTATION ONLY. */
public record NativeClip(String id,int ticks,Map<String,List<Key>> tracks,List<Event> events,
                         AnimationClip legacyC3Source) {
    /** New format-v2 clips have the native SQUAD/Hermite playback policy. */
    public NativeClip(String id,int ticks,Map<String,List<Key>> tracks,List<Event> events) {
        this(id,ticks,tracks,events,null);
    }
    public record Key(double t,Transform value){
        public Key{if(!Double.isFinite(t)||t<0||t>1)throw new IllegalArgumentException("Key time");Objects.requireNonNull(value);}
    }
    public record Event(String id,double time){
        public Event {if(id==null||!id.matches("[a-z_][a-z_0-9]*")||!Double.isFinite(time)||time<0||time>1)
            throw new IllegalArgumentException("Bad marker");}
    }
    public NativeClip {
        if(id==null||!id.matches("mkm:[a-z0-9_./-]+")||ticks<2||ticks>200
                ||tracks==null||tracks.isEmpty()||tracks.size()>48||events==null||events.size()>64)
            throw new IllegalArgumentException("Bad clip");
        Map<String,List<Key>> safe=new HashMap<>();
        for(var e:tracks.entrySet()){
            if(e.getKey()==null||!e.getKey().matches("[a-z_][a-z_0-9]{0,47}"))
                throw new IllegalArgumentException("Track name");
            List<Key> k=List.copyOf(e.getValue());
            if(k.size()<2||k.size()>256||k.getFirst().t()!=0||k.getLast().t()!=1)
                throw new IllegalArgumentException("Key range");
            double prev=-1;
            for(Key key:k){if(key.t()<=prev)throw new IllegalArgumentException("Nonmonotone keys");prev=key.t();}
            safe.put(e.getKey(),k);
        }
        tracks=Map.copyOf(safe);
        events=List.copyOf(events);
        // Legacy format-v1 clips carry their original, immutable interpolation
        // contract; a quaternion SQUAD curve cannot replicate C3 Euler PCHIP.
        if(legacyC3Source!=null &&
                (!legacyC3Source.id().equals(id)
                 ||legacyC3Source.durationTicks()!=ticks
                 ||legacyC3Source.tracks().size()!=tracks.size()
                 ||legacyC3Source.markers().size()!=events.size()))
            throw new IllegalArgumentException("Legacy clip metadata mismatch");
    }
}
