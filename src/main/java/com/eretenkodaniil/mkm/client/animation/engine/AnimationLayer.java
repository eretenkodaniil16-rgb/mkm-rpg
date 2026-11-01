package com.eretenkodaniil.mkm.client.animation.engine;
import java.util.*;
/** Independent channel masks and ordered priorities. */
public record AnimationLayer(Map<String,Transform> pose,int priority,Set<String> mask,
                             double weight,Mode mode) {
    public enum Mode{ADDITIVE,OVERRIDE}
    public AnimationLayer{
        pose=Map.copyOf(Objects.requireNonNull(pose));mask=Set.copyOf(Objects.requireNonNull(mask));
        if(!Double.isFinite(weight)||weight<0||weight>1||mode==null)
            throw new IllegalArgumentException("Layer weight/mode");
    }
}
