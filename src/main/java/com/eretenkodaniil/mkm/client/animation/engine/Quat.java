package com.eretenkodaniil.mkm.client.animation.engine;
/** Unit quaternion. Euler order is qZ * qY * qX, with Minecraft-local radians. */
public record Quat(double x,double y,double z,double w) {
    public static final Quat IDENTITY = new Quat(0,0,0,1);
    public Quat {
        double n=Math.sqrt(x*x+y*y+z*z+w*w);
        if(!Double.isFinite(n)||n<1e-12)throw new IllegalArgumentException("Invalid quaternion");
        x/=n;y/=n;z/=n;w/=n;
    }
    public static Quat euler(double rx,double ry,double rz) {
        if(!Double.isFinite(rx)||!Double.isFinite(ry)||!Double.isFinite(rz)
                ||Math.max(Math.abs(rx),Math.max(Math.abs(ry),Math.abs(rz)))>Math.PI*2)
            throw new IllegalArgumentException("Invalid Euler angles");
        double a=Math.sin(rx/2),b=Math.cos(rx/2),c=Math.sin(ry/2),d=Math.cos(ry/2),e=Math.sin(rz/2),f=Math.cos(rz/2);
        return new Quat(a*d*f-b*c*e,b*c*f+a*d*e,b*d*e-a*c*f,b*d*f+a*c*e);
    }
    public Quat multiply(Quat b) {
        return new Quat(w*b.x+x*b.w+y*b.z-z*b.y,
                w*b.y-x*b.z+y*b.w+z*b.x,
                w*b.z+x*b.y-y*b.x+z*b.w,
                w*b.w-x*b.x-y*b.y-z*b.z);
    }
    public Vec3 rotate(Vec3 p){
        Quat q=this;
        double vx=2*(q.y*p.z-q.z*p.y),vy=2*(q.z*p.x-q.x*p.z),vz=2*(q.x*p.y-q.y*p.x);
        return new Vec3(p.x+q.w*vx+q.y*vz-q.z*vy,
                p.y+q.w*vy+q.z*vx-q.x*vz,
                p.z+q.w*vz+q.x*vy-q.y*vx);
    }
    public Quat slerp(Quat other,double t) {
        if(!Double.isFinite(t))throw new IllegalArgumentException("Nonfinite blend");
        t=Math.clamp(t,0,1);
        double dot=x*other.x+y*other.y+z*other.z+w*other.w;
        double sign=dot<0?-1:1;dot=Math.abs(dot);
        if(dot>.9995)return new Quat(x+t*(other.x*sign-x),y+t*(other.y*sign-y),
                z+t*(other.z*sign-z),w+t*(other.w*sign-w));
        double theta=Math.acos(Math.clamp(dot,-1,1)),denom=Math.sin(theta);
        double a=Math.sin((1-t)*theta)/denom,b=Math.sin(t*theta)/denom*sign;
        return new Quat(x*a+other.x*b,y*a+other.y*b,z*a+other.z*b,w*a+other.w*b);
    }
    public Vec3 toEuler(){
        double rx=Math.atan2(2*(w*x+y*z),1-2*(x*x+y*y));
        double ry=Math.asin(Math.clamp(2*(w*y-z*x),-1,1));
        double rz=Math.atan2(2*(w*z+x*y),1-2*(y*y+z*z));
        return new Vec3(rx,ry,rz);
    }
}
