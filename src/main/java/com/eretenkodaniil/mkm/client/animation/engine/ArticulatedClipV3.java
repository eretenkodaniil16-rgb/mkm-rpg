package com.eretenkodaniil.mkm.client.animation.engine;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * MNAE v3: 17 articulated bone WORLD samples baked from the immutable Blender
 * R06-derived R07 scene at 60 FPS. Local samples and bone parentage are also
 * stored for independent hierarchy QA. Does not own gameplay state or damage.
 * Geometry is attached to the 17 actual world-space joints, not downbaked
 * into the six vanilla PlayerModel rotations.
 */
public final class ArticulatedClipV3 {
    public record Bone(String id,String parent,Vec3 restPivot,Quat restRotation) {}
    public record Sample(Vec3 pivot,Quat rotation,Vec3 localTranslation,Quat localRotation,
                         Vec3 scale) {
        public Sample {
            Objects.requireNonNull(pivot);Objects.requireNonNull(rotation);
            Objects.requireNonNull(localTranslation);Objects.requireNonNull(localRotation);
            Objects.requireNonNull(scale);
            // First R07 uses 100% rigid skinned cuboids. Nonunit scale is not
            // silently dropped; future rigs must add explicit matrix support.
            if (Math.abs(scale.x()-1)>1e-6 || Math.abs(scale.y()-1)>1e-6
                    || Math.abs(scale.z()-1)>1e-6)
                throw new IllegalArgumentException("R07 requires unit bone scale");
        }
        public Sample mix(Sample b,double t) {
            return new Sample(pivot.lerp(b.pivot,t),rotation.slerp(b.rotation,t),
                    localTranslation.lerp(b.localTranslation,t),
                    localRotation.slerp(b.localRotation,t),scale.lerp(b.scale,t));
        }
    }
    public record Frame(double time,Map<String,Sample> joints) {}
    public record Mesh(String id,String bone,Vec3 min,Vec3 size,int u,int v) {}
    public record Marker(String id,double normalized,int frame) {}
    public record Socket(String hand,Vec3 gripOffset,Vec3 tipOffset) {}
    private final String id;
    private final int durationTicks;
    private final List<Bone> bones;
    private final Map<String,Bone> byId;
    private final List<Mesh> meshes;
    private final List<Frame> frames;
    private final List<Marker> markers;
    private final Socket socket;

    private ArticulatedClipV3(String id,int duration,List<Bone> bones,List<Mesh> meshes,
                               List<Frame> frames,List<Marker> markers,Socket socket) {
        this.id=id;this.durationTicks=duration;this.bones=List.copyOf(bones);
        this.meshes=List.copyOf(meshes);this.frames=List.copyOf(frames);
        this.markers=List.copyOf(markers);this.socket=socket;
        Map<String,Bone> tmp=new LinkedHashMap<>();
        for(Bone b:bones) {
            if(tmp.putIfAbsent(b.id(),b)!=null)throw new IllegalArgumentException("Duplicate bone");
        }
        this.byId=Map.copyOf(tmp);
        for(Bone b:bones) {
            Set<String> chain=new HashSet<>();
            String parent=b.id();
            while(parent!=null) {
                if(!chain.add(parent)||!tmp.containsKey(parent))
                    throw new IllegalArgumentException("Cycle/missing parent");
                parent=tmp.get(parent).parent();
            }
        }
        for(Mesh m:meshes)if(!tmp.containsKey(m.bone()))
            throw new IllegalArgumentException("Unknown mesh bone");
        for(Frame f:frames)if(!f.joints().keySet().equals(tmp.keySet()))
            throw new IllegalArgumentException("Partial articulated frame");
        if(duration!=14||bones.size()!=17||frames.size()!=43
                ||meshes.size()<15||meshes.size()>70)
            throw new IllegalArgumentException("R07 vertical slice requires 17 bones, 43 samples");
        if(markers.stream().noneMatch(m->m.id().equals("impact")
                && Math.abs(m.normalized()-5D/14D)<1e-6 && m.frame()==16))
            throw new IllegalArgumentException("Server impact timing not preserved");
        if(!tmp.containsKey(socket.hand()))throw new IllegalArgumentException("Grip bone absent");
    }

    public String id(){return id;}
    public int durationTicks(){return durationTicks;}
    public List<Bone> bones(){return bones;}
    public List<Mesh> meshes(){return meshes;}
    public List<Marker> markers(){return markers;}
    public Socket socket(){return socket;}
    public int frameCount(){return frames.size();}

    /** Sample at server-derived t. Pose is never allowed to trigger damage. */
    public Map<String,Sample> sample(double t) {
        if(!Double.isFinite(t))throw new IllegalArgumentException("Sample time not finite");
        t=Math.clamp(t,0D,1D);
        if(t<=0)return frames.getFirst().joints();
        if(t>=1)return frames.getLast().joints();
        double f=t*42D;int before=(int)Math.floor(f);
        if(before>=42)return frames.getLast().joints();
        double frac=f-before;
        Map<String,Sample> out=new HashMap<>();
        var first=frames.get(before).joints();
        var last=frames.get(before+1).joints();
        for(String bone:byId.keySet())
            out.put(bone,first.get(bone).mix(last.get(bone),frac));
        return Map.copyOf(out);
    }

    public Vec3 socketPoint(Map<String,Sample> pose,boolean tip){
        Sample hand=Objects.requireNonNull(pose.get(socket.hand()));
        Vec3 local=tip?socket.tipOffset():socket.gripOffset();
        return hand.pivot().add(hand.rotation().rotate(local));
    }

    public static ArticulatedClipV3 readResource(String resource) {
        if(!"assets/mkm/mnae/r07_mid_guard_cut_v3.json".equals(resource))
            throw new IllegalArgumentException("Unknown articulated resource");
        try(InputStream in=ArticulatedClipV3.class.getClassLoader().getResourceAsStream(resource)){
            if(in==null)throw new IllegalArgumentException("MNAE R07 asset absent; fallback to C3");
            return read(in);
        }catch(IOException e){throw new IllegalArgumentException("Cannot read R07",e);}
    }
    public static ArticulatedClipV3 read(InputStream in)throws IOException {
        Objects.requireNonNull(in);
        byte[] bytes=in.readNBytes(1024*1024+1);
        if(bytes.length>1024*1024)throw new IllegalArgumentException("R07 JSON > 1 MiB");
        JsonObject src=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
        if(src.get("format").getAsInt()!=3
                ||!src.get("id").getAsString().equals("mkm:r07/mid_guard_sword_cut"))
            throw new IllegalArgumentException("MNAE 17-bone v3 schema mismatch");
        JsonObject reference=src.getAsJsonObject("source");
        if(!"mkm-guards-r06-approved".equals(reference.get("approved_tag").getAsString())
                || !"MKM_GUARD_MID_02".equals(reference.get("selected_guard").getAsString()))
            throw new IllegalArgumentException("Wrong approved authoring origin");
        JsonObject coords=src.getAsJsonObject("coordinate_space");
        if(!"model_pixels".equals(coords.get("units").getAsString())
                ||!"xyzw".equals(coords.get("quaternion_order").getAsString()))
            throw new IllegalArgumentException("Different units or rotation order");
        List<Bone> bones=new ArrayList<>();
        for(JsonElement e:src.getAsJsonArray("bones")) {
            JsonObject b=e.getAsJsonObject();
            bones.add(new Bone(b.get("id").getAsString(),
                b.get("parent").isJsonNull()?null:b.get("parent").getAsString(),
                vec(b.getAsJsonArray("rest_pivot")),quat(b.getAsJsonArray("rest_quaternion"))));
        }
        List<Mesh> meshes=new ArrayList<>();
        for(JsonElement e:src.getAsJsonArray("mesh_bindings")){
            JsonObject m=e.getAsJsonObject();JsonArray box=m.getAsJsonArray("box");
            Vec3 min=new Vec3(box.get(0).getAsDouble(),box.get(1).getAsDouble(),box.get(2).getAsDouble());
            Vec3 size=new Vec3(box.get(3).getAsDouble(),box.get(4).getAsDouble(),box.get(5).getAsDouble());
            if(size.x()<=0||size.y()<=0||size.z()<=0||Math.max(size.x(),Math.max(size.y(),size.z()))>32)
                throw new IllegalArgumentException("Invalid mesh dimensions");
            JsonArray uv=m.getAsJsonArray("uv");int u=uv.get(0).getAsInt(),v=uv.get(1).getAsInt();
            if(u<0||u>63||v<0||v>63)throw new IllegalArgumentException("UV out of atlas");
            meshes.add(new Mesh(m.get("id").getAsString(),m.get("bone").getAsString(),min,size,u,v));
        }
        List<Frame> frames=new ArrayList<>();
        for(JsonElement e:src.getAsJsonArray("frames")){
            JsonObject frame=e.getAsJsonObject();
            Map<String,Sample> joints=new LinkedHashMap<>();
            for(var bone:frame.getAsJsonObject("bones").entrySet()) {
                JsonObject b=bone.getValue().getAsJsonObject();
                joints.put(bone.getKey(),new Sample(vec(b.getAsJsonArray("pivot")),
                    quat(b.getAsJsonArray("quaternion")),
                    vec(b.getAsJsonArray("local_translation")),
                    quat(b.getAsJsonArray("local_quaternion")),
                    vec(b.getAsJsonArray("scale"))));
            }
            frames.add(new Frame(frame.get("t").getAsDouble(),Map.copyOf(joints)));
        }
        if(frames.size()!=43)throw new IllegalArgumentException("Frame count mismatch");
        for(int i=0;i<frames.size();i++){
            if(Math.abs(frames.get(i).time()-i/42D)>2e-6)
                throw new IllegalArgumentException("Nonuniform sampled frame "+i);
        }
        List<Marker> markers=new ArrayList<>();
        for(JsonElement e:src.getAsJsonArray("markers")){
            JsonObject m=e.getAsJsonObject();
            markers.add(new Marker(m.get("name").getAsString(),m.get("t").getAsDouble(),
                    m.get("frame").getAsInt()));
        }
        JsonObject s=src.getAsJsonObject("weapon_socket");
        Socket socket=new Socket(s.get("hand_bone").getAsString(),
                vec(s.getAsJsonArray("local_handle")),vec(s.getAsJsonArray("local_tip")));
        return new ArticulatedClipV3(src.get("id").getAsString(),
                src.get("duration_ticks").getAsInt(),bones,meshes,frames,markers,socket);
    }
    private static Vec3 vec(JsonArray a){
        if(a==null||a.size()!=3)throw new IllegalArgumentException("Vector 3 required");
        return new Vec3(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble());
    }
    private static Quat quat(JsonArray a){
        if(a==null||a.size()!=4)throw new IllegalArgumentException("Quaternion xyzw required");
        return new Quat(a.get(0).getAsDouble(),a.get(1).getAsDouble(),
                a.get(2).getAsDouble(),a.get(3).getAsDouble());
    }
}
