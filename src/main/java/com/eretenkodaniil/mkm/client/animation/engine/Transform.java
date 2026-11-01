package com.eretenkodaniil.mkm.client.animation.engine;
import java.util.Objects;
/** Translation in MODEL PIXELS (1 pixel = 1/16 block), local quaternion and scale. */
public record Transform(Vec3 translation,Quat rotation,Vec3 scale) {
    public static final Transform IDENTITY = new Transform(Vec3.ZERO,Quat.IDENTITY,Vec3.ONE);
    public Transform {
        Objects.requireNonNull(translation);Objects.requireNonNull(rotation);Objects.requireNonNull(scale);
        if(scale.x()<.05||scale.y()<.05||scale.z()<.05||scale.x()>8||scale.y()>8||scale.z()>8)
            throw new IllegalArgumentException("Scale out of range");
    }
    public Transform mix(Transform b,double t){return new Transform(translation.lerp(b.translation,t),
            rotation.slerp(b.rotation,t),scale.lerp(b.scale,t));}
    public Transform compose(Transform local){return new Transform(
            translation.add(rotation.rotate(scale.multiply(local.translation))),
            rotation.multiply(local.rotation),scale.multiply(local.scale));}
}
