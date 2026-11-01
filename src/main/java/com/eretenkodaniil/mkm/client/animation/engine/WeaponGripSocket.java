package com.eretenkodaniil.mkm.client.animation.engine;

import java.util.Map;
import java.util.Objects;

/**
 * Model-space hand/socket diagnostics, units = model pixels (1/16 block).
 * Coordinates are relative to the six-piece avatar rig and do not assert
 * how Vanilla's separate ItemInHandLayer ultimately displays an item.
 * No client or Minecraft dependencies; good for Blender-to-engine golden tests.
 */
public final class WeaponGripSocket {
    private WeaponGripSocket(){}

    public record Grip(Vec3 handEnd, Vec3 handle, Vec3 bladeTip) {}

    public record Spec(Vec3 handOffset,Vec3 gripOffset,Vec3 tipOffset){
        public Spec {
            Objects.requireNonNull(handOffset);
            Objects.requireNonNull(gripOffset);
            Objects.requireNonNull(tipOffset);
        }
    }

    // Neutral side-handed sword proxy: hand endpoint on the lower arm and
    // forward blade axis. Must be calibrated against the real held-item layer.
    public static final Spec TEST_SWORD = new Spec(
            new Vec3(0,10,0),new Vec3(0,0,0),new Vec3(0,-14,0));

    public static Grip sample(Skeleton skeleton,Map<String,Transform> pose,
                              String armBone,Spec specification){
        Objects.requireNonNull(skeleton);
        Objects.requireNonNull(pose);
        Objects.requireNonNull(specification);
        if(!"right_arm".equals(armBone) && !"left_arm".equals(armBone))
            throw new IllegalArgumentException("A sword grip must belong to a physical arm");
        Transform arm=skeleton.world(armBone,pose);
        Transform hand=arm.compose(new Transform(specification.handOffset(),
                Quat.IDENTITY,Vec3.ONE));
        Transform handle=hand.compose(new Transform(specification.gripOffset(),
                Quat.IDENTITY,Vec3.ONE));
        Transform tip=handle.compose(new Transform(specification.tipOffset(),
                Quat.IDENTITY,Vec3.ONE));
        return new Grip(hand.translation(),handle.translation(),tip.translation());
    }

    /** Distance from a point to a finite grip-tip segment (model pixels). */
    public static double pointToBlade(Vec3 point,Grip grip){
        Objects.requireNonNull(point);
        Vec3 vector=grip.bladeTip().subtract(grip.handle());
        Vec3 offset=point.subtract(grip.handle());
        double denominator=vector.x()*vector.x()+vector.y()*vector.y()+vector.z()*vector.z();
        if(denominator<1e-12)return Math.sqrt(
                offset.x()*offset.x()+offset.y()*offset.y()+offset.z()*offset.z());
        double t=Math.clamp((offset.x()*vector.x()+offset.y()*vector.y()
                +offset.z()*vector.z())/denominator,0D,1D);
        Vec3 projected=grip.handle().add(vector.times(t));
        Vec3 delta=point.subtract(projected);
        return Math.sqrt(delta.x()*delta.x()+delta.y()*delta.y()+delta.z()*delta.z());
    }
}
