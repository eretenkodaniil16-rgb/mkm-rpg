package com.eretenkodaniil.mkm.client.animation.engine;
public record Vec3(double x, double y, double z) {
    public static final Vec3 ZERO = new Vec3(0,0,0);
    public static final Vec3 ONE = new Vec3(1,1,1);
    public Vec3 {
        if (!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)
                || Math.max(Math.abs(x), Math.max(Math.abs(y),Math.abs(z))) > 4096)
            throw new IllegalArgumentException("Invalid vector");
    }
    public Vec3 add(Vec3 v){return new Vec3(x+v.x,y+v.y,z+v.z);}
    public Vec3 subtract(Vec3 v){return new Vec3(x-v.x,y-v.y,z-v.z);}
    public Vec3 multiply(Vec3 v){return new Vec3(x*v.x,y*v.y,z*v.z);}
    public Vec3 times(double s){return new Vec3(x*s,y*s,z*s);}
    public Vec3 lerp(Vec3 b,double t){return new Vec3(x+(b.x-x)*t,y+(b.y-y)*t,z+(b.z-z)*t);}
}
