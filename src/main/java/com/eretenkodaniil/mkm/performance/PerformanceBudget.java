package com.eretenkodaniil.mkm.performance;

/**
 * Pure advisory load classification for future MKM-owned async workloads.
 * M0 never throttles gameplay, enemy AI, world ticking or rendering.
 */
public final class PerformanceBudget {
    private PerformanceBudget() { }

    public enum Pressure { NO_DATA, NORMAL, CAUTION, BUSY }

    public static Pressure classify(TickTimeWindow.Summary s) {
        if (s.samples() < 100) return Pressure.NO_DATA;
        if (s.p95Ms() >= 48.0 || s.p99Ms() >= 55.0) return Pressure.BUSY;
        if (s.p95Ms() >= 40.0 || s.p99Ms() >= 48.0) return Pressure.CAUTION;
        return Pressure.NORMAL;
    }

    /** Initial upper bound for future worker pools, not a reserved core count. */
    public static int maximumWorkers(int logicalCpus) {
        return Math.max(1, Math.min(4, logicalCpus - 2));
    }
}
