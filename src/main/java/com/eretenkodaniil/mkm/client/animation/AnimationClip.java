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
                    value = interpolateContinuous(keys, i - 1, u);
                    break;
                }
            }
            sampled.put(entry.getKey(), value);
        }
        return Collections.unmodifiableMap(sampled);
    }

    /**
     * Shape-preserving cubic Hermite interpolation (PCHIP-style interior slopes).
     * Conventional per-segment smoothstep artificially stops at every key. Here
     * velocity remains continuous through monotone authored keys, while motion
     * eases to rest at a peak, reversal, clip start or clip end.
     *
     * Tangents are in radians / normalized clip time and use nonuniform keys.
     * This is visual-only: no damage, movement or server hit timing is changed.
     */
    private static Rotation interpolateContinuous(List<Key> keys, int left, double u) {
        double h = keys.get(left + 1).time() - keys.get(left).time();
        return new Rotation(
                interpolateAxis(keys, left, u, h, 0),
                interpolateAxis(keys, left, u, h, 1),
                interpolateAxis(keys, left, u, h, 2));
    }

    private static double interpolateAxis(List<Key> keys, int left, double u, double h, int axis) {
        double first = axisValue(keys.get(left).rotation(), axis);
        double last = axisValue(keys.get(left + 1).rotation(), axis);
        double t0 = tangent(keys, left, axis);
        double t1 = tangent(keys, left + 1, axis);
        double u2 = u * u;
        double u3 = u2 * u;
        double result = (2D * u3 - 3D * u2 + 1D) * first
                + (u3 - 2D * u2 + u) * h * t0
                + (-2D * u3 + 3D * u2) * last
                + (u3 - u2) * h * t1;
        // Floating point tolerance must never turn a valid authored rotation
        // into an overshooting pose (or a rejected +/-2pi Rotation).
        return Math.max(Math.min(first, last), Math.min(Math.max(first, last), result));
    }

    private static double tangent(List<Key> keys, int index, int axis) {
        if (index == 0 || index == keys.size() - 1) return 0D;
        Key prev = keys.get(index - 1);
        Key current = keys.get(index);
        Key next = keys.get(index + 1);
        double h0 = current.time() - prev.time();
        double h1 = next.time() - current.time();
        double d0 = (axisValue(current.rotation(), axis) - axisValue(prev.rotation(), axis)) / h0;
        double d1 = (axisValue(next.rotation(), axis) - axisValue(current.rotation(), axis)) / h1;
        if (!Double.isFinite(d0) || !Double.isFinite(d1)
                || d0 == 0D || d1 == 0D || Math.signum(d0) != Math.signum(d1)) {
            return 0D;
        }
        double w0 = 2D * h1 + h0;
        double w1 = h1 + 2D * h0;
        double denominator = w0 / d0 + w1 / d1;
        if (!Double.isFinite(denominator) || denominator == 0D) return 0D;
        double slope = (w0 + w1) / denominator;
        return Double.isFinite(slope) ? slope : 0D;
    }

    private static double axisValue(Rotation rotation, int axis) {
        return switch (axis) {
            case 0 -> rotation.x();
            case 1 -> rotation.y();
            default -> rotation.z();
        };
    }
}
