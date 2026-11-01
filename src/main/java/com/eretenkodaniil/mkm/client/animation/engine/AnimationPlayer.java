package com.eretenkodaniil.mkm.client.animation.engine;

import java.util.Map;
import java.util.Objects;

/**
 * Deterministic local animation clock with phase-preserving speed changes and
 * pose-to-pose transitions. No server state, wall clock, damage or movement.
 * Instance belongs to one avatar; callers provide game tick + partial tick.
 */
public final class AnimationPlayer {
    private NativeClip current;
    private double anchorTick;
    private double elapsedAtAnchor;
    private double speed = 1D;
    private double fadeStart, fadeDuration;
    private Map<String, Transform> outgoing = Map.of();
    private boolean fadingOut;

    public void play(NativeClip clip, double now, double fadeTicks) {
        Objects.requireNonNull(clip, "clip");
        checkTick(now);
        checkFade(fadeTicks);
        Map<String, Transform> snapshot = sample(now);
        current = clip;
        anchorTick = now;
        elapsedAtAnchor = 0D;
        fadingOut = false;
        outgoing = snapshot;
        fadeStart = now;
        fadeDuration = fadeTicks;
    }

    /**
     * Configure a future player's speed before starting playback.
     * During playback use setSpeedAt(value, now) so phase cannot jump.
     */
    public void speed(double multiplier) {
        validateSpeed(multiplier);
        if (playing()) throw new IllegalStateException("Use setSpeedAt during playback");
        speed = multiplier;
    }

    public void setSpeedAt(double multiplier, double now) {
        validateSpeed(multiplier);
        checkTick(now);
        if (current != null) {
            elapsedAtAnchor = clipTicks(now);
            anchorTick = now;
        }
        speed = multiplier;
    }

    public double speed() { return speed; }

    public void stop() {
        current = null;
        fadingOut = false;
        outgoing = Map.of();
    }

    /** Fade the last sampled pose to the identity stance without popping bones. */
    public void stop(double now, double fadeTicks) {
        checkTick(now);
        checkFade(fadeTicks);
        Map<String, Transform> snapshot = sample(now);
        current = null;
        outgoing = snapshot;
        fadeStart = now;
        fadeDuration = fadeTicks;
        fadingOut = fadeTicks > 0D && !snapshot.isEmpty();
        if (!fadingOut) outgoing = Map.of();
    }

    public boolean playing() { return current != null || fadingOut; }

    public String clipId() { return current == null ? null : current.id(); }

    public Map<String, Transform> sample(double now) {
        checkTick(now);
        if (current != null) {
            double normalized = Math.clamp(clipTicks(now) / current.ticks(), 0D, 1D);
            Map<String, Transform> pose = PoseSampler.sample(current, normalized);
            if (fadeDuration <= 0D || now >= fadeStart + fadeDuration) return pose;
            return PoseMixer.crossfade(outgoing, pose, smooth((now - fadeStart) / fadeDuration));
        }
        if (fadingOut) {
            if (now >= fadeStart + fadeDuration) {
                fadingOut = false;
                outgoing = Map.of();
                return Map.of();
            }
            return PoseMixer.crossfade(outgoing, Map.of(),
                    smooth((now - fadeStart) / fadeDuration));
        }
        return Map.of();
    }

    private double clipTicks(double now) {
        return Math.max(0D, elapsedAtAnchor + Math.max(0D, now - anchorTick) * speed);
    }

    private static double smooth(double value) {
        double t = Math.clamp(value, 0D, 1D);
        return t * t * (3D - 2D * t);
    }

    private static void validateSpeed(double value) {
        if (!Double.isFinite(value) || value <= .1D || value > 4D)
            throw new IllegalArgumentException("Playback speed outside (0.1,4]");
    }

    private static void checkTick(double value) {
        if (!Double.isFinite(value) || value < 0D)
            throw new IllegalArgumentException("Invalid animation clock");
    }

    private static void checkFade(double value) {
        if (!Double.isFinite(value) || value < 0D || value > 20D)
            throw new IllegalArgumentException("Fade outside [0,20] ticks");
    }
}
