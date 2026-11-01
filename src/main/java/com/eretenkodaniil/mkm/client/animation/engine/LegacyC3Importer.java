package com.eretenkodaniil.mkm.client.animation.engine;
import com.eretenkodaniil.mkm.client.animation.AnimationClip;
import java.util.*;
/** Exact C3 asset migration into native channels; source data remains immutable. */
public final class LegacyC3Importer {
    private LegacyC3Importer(){}
    public static NativeClip convert(AnimationClip source){
        Objects.requireNonNull(source);
        Map<String,List<NativeClip.Key>> tracks=new HashMap<>();
        for(var entry:source.tracks().entrySet()){
            List<NativeClip.Key> keys=new ArrayList<>();
            for(var key:entry.getValue()){
                var a=key.rotation();
                keys.add(new NativeClip.Key(key.time(),new Transform(Vec3.ZERO,
                        Quat.euler(a.x(),a.y(),a.z()),Vec3.ONE)));
            }
            tracks.put(entry.getKey().name().toLowerCase(Locale.ROOT),keys);
        }
        return new NativeClip(source.id(),source.durationTicks(),tracks,
                source.markers().stream().map(m->new NativeClip.Event(m.name(),m.time())).toList(),source);
    }
}
