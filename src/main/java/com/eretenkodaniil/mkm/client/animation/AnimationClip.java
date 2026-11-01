package com.eretenkodaniil.mkm.client.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, versioned authoring representation; independent from Minecraft rendering.
 * Values are additive Euler rotations in radians, in native model-part local axes.
 * No pose ever moves collision boxes or issues gameplay events.
 */
public final class AnimationClip {
    public enum Bone {
        HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG
    }

    public enum Layer { UPPER_BODY, FULL_BODY }

    public record Rotation(double x, double y, double z) {
        public Rotation {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("Non-finite rotation");
            }
            if (Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) > Math.PI * 2) {
                throw new IllegalArgumentException("Rotation outside +/-2pi radians");
            }
        }

        public static Rotation interpolate(Rotation a, Rotation b, double t) {
            return new Rotation(a.x + (b.x - a.x) * t,
                    a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
        }
    }

    public record Key(double time, Rotation rotation) {
        public Key {
            if (!Double.isFinite(time) || time < 0D || time > 1D
                    || rotation == null) {
                throw new IllegalArgumentException("Invalid normalized keyframe");
            }
        }
    }

    public record Marker(String name, double time) {
        public Marker {
            if (name == null || !name.matches("[a-z_][a-z_0-9]*")
                    || !Double.isFinite(time) || time < 0D || time > 1D) {
                throw new IllegalArgumentException("Invalid animation event marker");
            }
        }
    }

    private final String id;
    private final int durationTicks;
    private final Layer layer;
    private final Map<Bone, List<Key>> tracks;
    private final List<Marker> markers;

    public AnimationClip(String id, int durationTicks, Layer layer,
                         Map<Bone, List<Key>> tracks, List<Marker> markers) {
        if (id == null || !id.matches("mkm:[a-z0-9_./-]+")
                || durationTicks < 2 || durationTicks > 200 || layer == null
                || tracks == null || tracks.isEmpty() || markers == null) {
            throw new IllegalArgumentException("Invalid animation clip metadata");
        }
        this.id = id;
        this.durationTicks = durationTicks;
        this.layer = layer;
        EnumMap<Bone, List<Key>> copy = new EnumMap<>(Bone.class);
        for (var entry : tracks.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null
                    || entry.getValue().size() < 2 || entry.getValue().size() > 256) {
                throw new IllegalArgumentException("Missing or oversized bone track");
            }
            List<Key> keys = List.copyOf(entry.getValue());
            if (keys.get(0).time() != 0D || keys.get(keys.size() - 1).time() != 1D) {
                throw new IllegalArgumentException("Every track must span [0,1]");
            }
            double previous = -1D;
            for (Key key : keys) {
                if (key.time() <= previous) throw new IllegalArgumentException("Keyframes not strictly ordered");
                previous = key.time();
            }
            if (layer == Layer.UPPER_BODY && (entry.getKey() == Bone.LEFT_LEG
                    || entry.getKey() == Bone.RIGHT_LEG)) {
                throw new IllegalArgumentException("Upper-body clips cannot animate leg bones");
            }
            copy.put(entry.getKey(), keys);
        }
        this.tracks = Collections.unmodifiableMap(copy);
        if (markers.size() > 64) throw new IllegalArgumentException("Too many markers");
        List<Marker> sortedMarkers = new ArrayList<>(markers);
        sortedMarkers.sort((a, b) -> Double.compare(a.time(), b.time()));
        this.markers = List.copyOf(sortedMarkers);
    }

    public String id() { return id; }
    public int durationTicks() { return durationTicks; }
    public Layer layer() { return layer; }
    public Map<Bone, List<Key>> tracks() { return tracks; }
    public List<Marker> markers() { return markers; }

    /**
     * Per-segment smoothstep interpolation; data can contain anticipations,
     * overshoots and settling, rather than one hard-coded pose per phase.
     */
    public Map<Bone, Rotation> sample(double normalizedTime) {
        double t = (!Double.isFinite(normalizedTime)) ? 0D
                : Math.max(0D, Math.min(1D, normalizedTime));
        EnumMap<Bone, Rotation> sampled = new EnumMap<>(Bone.class);
        for (var entry : tracks.entrySet()) {
            List<Key> keys = entry.getValue();
            Rotation value = keys.get(keys.size() - 1).rotation();
            for (int i = 1; i < keys.size(); i++) {
                Key right = keys.get(i);
                if (t <= right.time()) {
                    Key left = keys.get(i - 1);
                    double u = (t - left.time()) / (right.time() - left.time());
                    u = Math.max(0D, Math.min(1D, u));
                    u = u * u * (3D - 2D * u);
                    value = Rotation.interpolate(left.rotation(), right.rotation(), u);
                    break;
                }
            }
            sampled.put(entry.getKey(), value);
        }
        return Collections.unmodifiableMap(sampled);
    }
}
