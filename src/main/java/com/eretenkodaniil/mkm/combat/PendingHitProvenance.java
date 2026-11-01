package com.eretenkodaniil.mkm.combat;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Server-thread-only provenance for transient heavy-attack markers.
 * A marker belongs to an exact source event, not merely to a target.
 */
public final class PendingHitProvenance<T, S> {
    private static final long MAX_DELAY_TICKS = 1L;
    private final Map<T, Marker<S>> pending = new WeakHashMap<>();

    /** A new eligible damage attempt invalidates the previous marker for its target. */
    public void begin(T target) {
        pending.remove(Objects.requireNonNull(target, "target"));
    }

    public void record(T target, S source, long tick) {
        pending.put(Objects.requireNonNull(target, "target"),
                new Marker<>(Objects.requireNonNull(source, "source"), tick));
    }

    /** Consumes once, even when source mismatches or callback has expired. */
    public boolean consume(T target, S source, long tick) {
        Marker<S> marker = pending.remove(Objects.requireNonNull(target, "target"));
        return marker != null && marker.source() == source
                && tick >= marker.tick() && tick - marker.tick() <= MAX_DELAY_TICKS;
    }

    public void clear(T target) {
        pending.remove(Objects.requireNonNull(target, "target"));
    }

    private record Marker<S>(S source, long tick) {}
}
