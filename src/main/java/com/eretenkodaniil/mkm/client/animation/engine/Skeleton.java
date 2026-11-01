package com.eretenkodaniil.mkm.client.animation.engine;
import java.util.*;
/** Validated parent DAG; pivot is relative to parent, using model pixels. */
public final class Skeleton {
    public record Bone(String id,String parent,Vec3 pivot,Transform bind){
        public Bone {if(id==null||!id.matches("[a-z_][a-z_0-9]{0,47}"))throw new IllegalArgumentException("Bone id");
            Objects.requireNonNull(pivot);Objects.requireNonNull(bind);}
    }
    private final Map<String,Bone> bones;
    public Skeleton(List<Bone> input){
        if(input==null||input.isEmpty()||input.size()>48)throw new IllegalArgumentException("Bone count");
        Map<String,Bone> map=new LinkedHashMap<>();
        for(Bone b:input)if(map.putIfAbsent(b.id(),b)!=null)throw new IllegalArgumentException("Duplicate bone");
        for(Bone b:map.values()) {
            Set<String> seen=new HashSet<>();
            String current=b.id();
            while(current!=null){
                if(!seen.add(current))throw new IllegalArgumentException("Skeleton cycle");
                Bone next=map.get(current);
                if(next==null)throw new IllegalArgumentException("Missing parent");
                current=next.parent();
            }
        }
        bones=Map.copyOf(map);
    }
    public Set<String> ids(){return bones.keySet();}
    public Bone bone(String id){return bones.get(id);}
    public Transform world(String id,Map<String,Transform> offsets){
        Bone b=bones.get(id);if(b==null)throw new IllegalArgumentException("Missing bone");
        Transform local=new Transform(b.pivot(),Quat.IDENTITY,Vec3.ONE)
                .compose(b.bind()).compose(offsets.getOrDefault(id,Transform.IDENTITY));
        return b.parent()==null?local:world(b.parent(),offsets).compose(local);
    }
    public static Skeleton vanilla(){
        return new Skeleton(List.of(new Bone("body",null,Vec3.ZERO,Transform.IDENTITY),
                new Bone("head","body",new Vec3(0,-8,0),Transform.IDENTITY),
                new Bone("left_arm","body",new Vec3(-5,-2,0),Transform.IDENTITY),
                new Bone("right_arm","body",new Vec3(5,-2,0),Transform.IDENTITY),
                new Bone("left_leg",null,new Vec3(-2,12,0),Transform.IDENTITY),
                new Bone("right_leg",null,new Vec3(2,12,0),Transform.IDENTITY)));
    }
}
