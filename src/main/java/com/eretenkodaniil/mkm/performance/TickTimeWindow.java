package com.eretenkodaniil.mkm.performance;

import java.util.Arrays;

/**
 * Bounded, allocation-free-per-tick timing window.
 * Call begin/end on the same owning server thread. This measures elapsed time
 * between Fabric server tick callbacks, not full spark MSPT.
 */
public final class TickTimeWindow {
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;
    private final long[] durations;
    private int writeIndex;
    private int sampleCount;
    private long started;
    private boolean running;

    public TickTimeWindow(int capacity) {
        if (capacity < 16 || capacity > 16_384) {
            throw new IllegalArgumentException("Tick sample capacity must be 16..16384");
        }
        durations = new long[capacity];
    }

    public void begin(long nowNanos) {
        started = nowNanos;
        running = true;
    }

    public void end(long nowNanos) {
        if (!running) return;
        running = false;
        long elapsed = nowNanos - started;
        if (elapsed < 0) return;
        durations[writeIndex] = elapsed;
        writeIndex = (writeIndex + 1) % durations.length;
        if (sampleCount < durations.length) sampleCount++;
    }

    /** Reset samples when a new integrated or dedicated server session begins. */
    public void clear() {
        running = false;
        writeIndex = 0;
        sampleCount = 0;
    }

    /** Snapshot is called infrequently (commands), never every tick. */
    public Summary snapshot() {
        if (sampleCount == 0) return new Summary(0, 0, 0, 0, 0, 0);
        long[] samples = Arrays.copyOf(durations, sampleCount);
        Arrays.sort(samples);
        double total = 0;
        for (long sample : samples) total += sample;
        return new Summary(sampleCount, total / sampleCount / NANOS_PER_MILLISECOND,
                percentile(samples, 50), percentile(samples, 95),
                percentile(samples, 99), samples[samples.length - 1] / NANOS_PER_MILLISECOND);
    }

    private static double percentile(long[] sorted, int percentage) {
        int index = Math.max(0, (int) Math.ceil(sorted.length * percentage / 100.0) - 1);
        return sorted[index] / NANOS_PER_MILLISECOND;
    }

    public record Summary(int samples, double meanMs, double p50Ms, double p95Ms,
                          double p99Ms, double maxMs) { }
}
