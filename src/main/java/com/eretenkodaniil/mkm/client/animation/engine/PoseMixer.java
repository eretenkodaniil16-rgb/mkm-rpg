package com.eretenkodaniil.mkm.client.animation.engine;
import java.util.*;
/** A single ordered pass; masked channels cannot affect omitted bones. */
public final class PoseMixer {
    private PoseMixer(){}
    public static Map<String,Transform> mix(Map<String,Transform> base,List<AnimationLayer> layers){
        Map<String,Transform> result=new HashMap<>(base);
        ArrayList<AnimationLayer> ordered=new ArrayList<>(layers);
        ordered.sort(Comparator.comparingInt(AnimationLayer::priority));
        for(AnimationLayer layer:ordered){
            for(var e:layer.pose().entrySet()){
                String id=e.getKey();
                if(!layer.mask().isEmpty()&&!layer.mask().contains(id))continue;
                Transform first=result.getOrDefault(id,Transform.IDENTITY);
                Transform target=e.getValue();
                if(layer.mode()==AnimationLayer.Mode.OVERRIDE)
                    result.put(id,first.mix(target,layer.weight()));
                else {
                    Transform delta=new Transform(target.translation().times(layer.weight()),
                            Quat.IDENTITY.slerp(target.rotation(),layer.weight()),
                            Vec3.ONE.lerp(target.scale(),layer.weight()));
                    result.put(id,first.compose(delta));
                }
            }
        }
        return Map.copyOf(result);
    }
    /**
     * Crossfade over the UNION of bones. Outgoing-only bones must fade to
     * identity instead of remaining until the last tick and then snapping.
     * At endpoints return the exact immutable source/target poses.
     */
    public static Map<String,Transform> crossfade(Map<String,Transform> from,
                                                   Map<String,Transform> to, double alpha) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (!Double.isFinite(alpha)) throw new IllegalArgumentException("Invalid crossfade");
        if (alpha <= 0D) return Map.copyOf(from);
        if (alpha >= 1D) return Map.copyOf(to);
        Map<String,Transform> result = new HashMap<>();
        Set<String> bones = new HashSet<>(from.keySet());
        bones.addAll(to.keySet());
        for (String bone : bones) {
            Transform a = from.getOrDefault(bone, Transform.IDENTITY);
            Transform b = to.getOrDefault(bone, Transform.IDENTITY);
            result.put(bone, a.mix(b, alpha));
        }
        return Map.copyOf(result);
    }

}
