package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.combat.ActionTimelineRules;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Validated immutable classpath animation library.
 *
 * <p>Blender and authoring tools produce JSON under assets/mkm/animations.
 * No arbitrary code execution, client files, network downloads or Minecraft
 * reconfiguration is required to play a clip.</p>
 */
public final class AnimationLibrary {
    private static final String ROOT = "assets/mkm/animations/";
    private final Map<String, AnimationClip> clips;
    private final Map<String, AnimationProfile> profiles;

    private AnimationLibrary(Map<String, AnimationClip> clips,
                             Map<String, AnimationProfile> profiles) {
        this.clips = Map.copyOf(clips);
        this.profiles = Map.copyOf(profiles);
    }

    private static final class Embedded {
        static final AnimationLibrary INSTANCE = safeLoad();
    }

    public static AnimationLibrary embedded() {
        return Embedded.INSTANCE;
    }

    private static AnimationLibrary safeLoad() {
        try {
            AnimationLibrary lib = loadClasspath();
            MkmMod.LOGGER.info("C3 animation library loaded: {} clips, {} profiles",
                    lib.clips.size(), lib.profiles.size());
            return lib;
        } catch (RuntimeException exception) {
            // A malformed resource must not crash a user's existing C2.1
            // world. The client model Mixins preserve their legacy fallback.
            MkmMod.LOGGER.error("C3 animation library invalid; using legacy C2.1 poses", exception);
            return new AnimationLibrary(Map.of(), Map.of());
        }
    }

    public static AnimationLibrary loadClasspath() {
        return load(name -> AnimationLibrary.class.getClassLoader().getResourceAsStream(name));
    }

    /** Injectable opener permits independent fixture tests without a live game. */
    public static AnimationLibrary load(Function<String, InputStream> opener) {
        JsonObject index = readJson(opener, "index.json");
        if (index.get("format").getAsInt() != 1) {
            throw new IllegalArgumentException("Unsupported animation library version");
        }
        Map<String, AnimationClip> clips = new HashMap<>();
        Map<String, AnimationProfile> profiles = new HashMap<>();
        for (var path : index.getAsJsonArray("clips")) {
            AnimationClip clip = parseClip(readJson(opener, path.getAsString()));
            if (clips.putIfAbsent(clip.id(), clip) != null) {
                throw new IllegalArgumentException("Duplicate clip " + clip.id());
            }
        }
        for (var path : index.getAsJsonArray("profiles")) {
            AnimationProfile profile = parseProfile(readJson(opener, path.getAsString()));
            if (profiles.putIfAbsent(profile.id(), profile) != null) {
                throw new IllegalArgumentException("Duplicate profile " + profile.id());
            }
        }
        if (clips.isEmpty() || profiles.isEmpty() || clips.size() > 200 || profiles.size() > 50) {
            throw new IllegalArgumentException("Missing or excessive animation assets");
        }
        for (AnimationProfile profile : profiles.values()) {
            for (var entry : profile.clips().entrySet()) {
                AnimationClip clip = clips.get(entry.getValue());
                if (clip == null) throw new IllegalArgumentException(
                        "Unresolved animation clip " + entry.getValue());
                if (entry.getKey().startsWith("combo_")) {
                    int stage = Integer.parseInt(entry.getKey().substring(6));
                    int expected = (int) (ActionTimelineRules.windupTicks(stage)
                            + 1 + ActionTimelineRules.recoveryTicks(stage));
                    if (clip.durationTicks() != expected) {
                        throw new IllegalArgumentException("Clip duration does not match server combo " + stage);
                    }
                    double hitTime = ActionTimelineRules.windupTicks(stage) / (double) expected;
                    if (clip.markers().stream().noneMatch(m ->
                            m.name().equals("impact") && Math.abs(m.time() - hitTime) < 0.001D)) {
                        throw new IllegalArgumentException("Impact marker does not align with server hit tick");
                    }
                } else if (entry.getKey().startsWith("dodge_")
                        && clip.durationTicks() != ActionTimelineRules.DODGE_ACTIVE_TICKS) {
                    throw new IllegalArgumentException("Dodge clip duration differs from server motion state");
                }
            }
        }
        return new AnimationLibrary(clips, profiles);
    }

    public AnimationClip clip(String id) {
        return id == null ? null : clips.get(id);
    }

    public AnimationProfile profile(String id) {
        return id == null ? null : profiles.get(id);
    }

    public int clipCount() { return clips.size(); }

    private static JsonObject readJson(Function<String, InputStream> opener, String relative) {
        if (!relative.matches("[a-z0-9_./-]+\\.json") || relative.contains("..")
                || relative.startsWith("/")) {
            throw new IllegalArgumentException("Invalid animation asset path: " + relative);
        }
        try (InputStream input = opener.apply(ROOT + relative)) {
            if (input == null) throw new IllegalArgumentException("Missing animation asset " + relative);
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Cannot load " + relative, exception);
        }
    }

    private static AnimationClip parseClip(JsonObject json) {
        if (json.get("format").getAsInt() != 1) {
            throw new IllegalArgumentException("Unknown clip format");
        }
        String id = json.get("id").getAsString();
        int duration = json.get("duration_ticks").getAsInt();
        AnimationClip.Layer layer = AnimationClip.Layer.valueOf(
                json.get("layer").getAsString().toUpperCase(Locale.ROOT));
        EnumMap<AnimationClip.Bone, List<AnimationClip.Key>> tracks =
                new EnumMap<>(AnimationClip.Bone.class);
        for (var entry : json.getAsJsonObject("tracks").entrySet()) {
            AnimationClip.Bone bone = AnimationClip.Bone.valueOf(
                    entry.getKey().toUpperCase(Locale.ROOT));
            List<AnimationClip.Key> keys = new ArrayList<>();
            for (var value : entry.getValue().getAsJsonArray()) {
                JsonObject key = value.getAsJsonObject();
                JsonArray r = key.getAsJsonArray("r");
                if (r.size() != 3) throw new IllegalArgumentException("Rotation must have three axes");
                keys.add(new AnimationClip.Key(key.get("t").getAsDouble(),
                        new AnimationClip.Rotation(r.get(0).getAsDouble(),
                                r.get(1).getAsDouble(), r.get(2).getAsDouble())));
            }
            tracks.put(bone, keys);
        }
        List<AnimationClip.Marker> markers = new ArrayList<>();
        if (json.has("markers")) {
            for (var value : json.getAsJsonArray("markers")) {
                JsonObject marker = value.getAsJsonObject();
                markers.add(new AnimationClip.Marker(marker.get("name").getAsString(),
                        marker.get("t").getAsDouble()));
            }
        }
        return new AnimationClip(id, duration, layer, tracks, markers);
    }

    private static AnimationProfile parseProfile(JsonObject json) {
        if (json.get("format").getAsInt() != 1) {
            throw new IllegalArgumentException("Unknown profile format");
        }
        Map<String, String> mappings = new HashMap<>();
        for (var entry : json.getAsJsonObject("clips").entrySet()) {
            mappings.put(entry.getKey(), entry.getValue().getAsString());
        }
        return new AnimationProfile(json.get("id").getAsString(), mappings);
    }
}
