package com.eretenkodaniil.mkm.client.animation.engine;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
/** Strict declarative MNAE format v2 loader; no user-supplied executable code. */
public final class NativeClipLoader {
    private NativeClipLoader(){}
    public static NativeClip read(String path) {
        return load(path,p->NativeClipLoader.class.getClassLoader().getResourceAsStream(p));
    }
    public static NativeClip load(String path,Function<String,InputStream> opener){
        if(path==null||!path.matches("assets/mkm/mnae/[a-z0-9_/-]+\\.json")
                ||path.contains(".."))throw new IllegalArgumentException("Resource path");
        try(InputStream in=opener.apply(path)){
            if(in==null)throw new IllegalArgumentException("Missing "+path);
            try(Reader input=new InputStreamReader(in,StandardCharsets.UTF_8)){
                JsonObject j=JsonParser.parseReader(input).getAsJsonObject();
                if(j.get("format").getAsInt()!=2||!"model_pixels".equals(j.get("units").getAsString())
                    ||!"xyz_radians".equals(j.get("rotation").getAsString()))
                    throw new IllegalArgumentException("MNAE schema or unit mismatch");
                Map<String,List<NativeClip.Key>> tracks=new HashMap<>();
                for(var entry:j.getAsJsonObject("tracks").entrySet()){
                    List<NativeClip.Key> keys=new ArrayList<>();
                    for(var value:entry.getValue().getAsJsonArray()){
                        JsonObject key=value.getAsJsonObject();
                        Vec3 pos=vec(key.getAsJsonArray("p"));
                        Vec3 euler=vec(key.getAsJsonArray("r"));
                        Vec3 scale=vec(key.getAsJsonArray("s"));
                        keys.add(new NativeClip.Key(key.get("t").getAsDouble(),
                                new Transform(pos,Quat.euler(euler.x(),euler.y(),euler.z()),scale)));
                    }
                    tracks.put(entry.getKey(),keys);
                }
                List<NativeClip.Event> events=new ArrayList<>();
                for(var event:j.getAsJsonArray("markers")){
                    JsonObject e=event.getAsJsonObject();
                    events.add(new NativeClip.Event(e.get("name").getAsString(),e.get("t").getAsDouble()));
                }
                return new NativeClip(j.get("id").getAsString(),j.get("duration_ticks").getAsInt(),tracks,events);
            }
        }catch(IOException ex){throw new IllegalArgumentException("Cannot load MNAE clip "+path,ex);}
    }
    private static Vec3 vec(JsonArray a){
        if(a==null||a.size()!=3)throw new IllegalArgumentException("Expected 3 channels");
        return new Vec3(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble());
    }
}
