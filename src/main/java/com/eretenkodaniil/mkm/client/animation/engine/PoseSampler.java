package com.eretenkodaniil.mkm.client.animation.engine;
import java.util.*;
/** Stateless deterministic sampler in NORMALIZED clip time. */
public final class PoseSampler {
    private PoseSampler(){}
    public static Map<String,Transform> sample(NativeClip clip,double normalized) {
        double t=Double.isFinite(normalized)?Math.clamp(normalized,0,1):0;
        Map<String,Transform> result=new HashMap<>();
        for(var e:clip.tracks().entrySet()){
            List<NativeClip.Key> keys=e.getValue();
            Transform value=keys.getLast().value();
            for(int i=1;i<keys.size();i++){
                NativeClip.Key a=keys.get(i-1),b=keys.get(i);
                if(t<=b.t()){
                    double u=Math.clamp((t-a.t())/(b.t()-a.t()),0,1);
                    u=u*u*(3-2*u);
                    value=a.value().mix(b.value(),u);break;
                }
            }
            result.put(e.getKey(),value);
        }
        return Map.copyOf(result);
    }
}
