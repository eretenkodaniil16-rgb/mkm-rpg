package com.eretenkodaniil.mkm.client.animation.engine;
import java.util.*;
/** Stateless, deterministic piecewise Hermite + shortest-arc quaternion SQUAD. */
public final class PoseSampler {
    private PoseSampler(){}
    public static Map<String,Transform> sample(NativeClip clip,double normalized) {
        Objects.requireNonNull(clip);
        double t=Double.isFinite(normalized)?Math.clamp(normalized,0,1):0;
        Map<String,Transform> result=new HashMap<>();
        for(var e:clip.tracks().entrySet()){
            List<NativeClip.Key> keys=e.getValue();
            Transform value=keys.getLast().value();
            for(int i=1;i<keys.size();i++){
                NativeClip.Key a=keys.get(i-1),b=keys.get(i);
                if(t<=b.t()){
                    double h=b.t()-a.t();
                    double u=Math.clamp((t-a.t())/h,0,1);
                    NativeClip.Key prev=i>=2?keys.get(i-2):null;
                    NativeClip.Key next=i+1<keys.size()?keys.get(i+1):null;
                    double hPrev=prev==null?h:a.t()-prev.t();
                    double hNext=next==null?h:next.t()-b.t();
                    Quat rotation=Quat.curve(prev==null?null:prev.value().rotation(),
                            a.value().rotation(),b.value().rotation(),
                            next==null?null:next.value().rotation(),
                            hPrev,h,hNext,u);
                    Vec3 translation=curveVec(keys,i-1,u,0);
                    Vec3 scale=curveVec(keys,i-1,u,1);
                    value=new Transform(translation,rotation,scale);
                    break;
                }
            }
            result.put(e.getKey(),value);
        }
        return Map.copyOf(result);
    }
    private static Vec3 curveVec(List<NativeClip.Key> keys,int left,double t,int type){
        return new Vec3(axis(keys,left,t,type,0),axis(keys,left,t,type,1),
                axis(keys,left,t,type,2));
    }
    private static double axis(List<NativeClip.Key> keys,int left,double u,int type,int axis){
        double h=keys.get(left+1).t()-keys.get(left).t();
        double start=component(keys.get(left).value(),type,axis);
        double end=component(keys.get(left+1).value(),type,axis);
        double m0=tangent(keys,left,type,axis);
        double m1=tangent(keys,left+1,type,axis);
        double u2=u*u,u3=u2*u;
        double v=(2*u3-3*u2+1)*start+(u3-2*u2+u)*h*m0
                +(-2*u3+3*u2)*end+(u3-u2)*h*m1;
        // No foot/pivot translation overshoot beyond adjacent authored keys.
        return Math.clamp(v,Math.min(start,end),Math.max(start,end));
    }
    private static double tangent(List<NativeClip.Key> keys,int index,int type,int axis){
        if(index==0||index==keys.size()-1)return 0;
        double h0=keys.get(index).t()-keys.get(index-1).t();
        double h1=keys.get(index+1).t()-keys.get(index).t();
        double d0=(component(keys.get(index).value(),type,axis)
                -component(keys.get(index-1).value(),type,axis))/h0;
        double d1=(component(keys.get(index+1).value(),type,axis)
                -component(keys.get(index).value(),type,axis))/h1;
        if(d0==0||d1==0||Math.signum(d0)!=Math.signum(d1))return 0;
        double w0=2*h1+h0,w1=h1+2*h0;
        return (w0+w1)/(w0/d0+w1/d1);
    }
    private static double component(Transform t,int type,int axis){
        Vec3 v=type==0?t.translation():t.scale();
        return switch(axis){case 0->v.x();case 1->v.y();default->v.z();};
    }
}
