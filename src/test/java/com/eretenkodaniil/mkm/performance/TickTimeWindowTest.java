package com.eretenkodaniil.mkm.performance;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class TickTimeWindowTest {
    @Test void reportsQuantilesAndKeepsBoundedWindow() {
        TickTimeWindow ticks = new TickTimeWindow(16);
        for (int ms = 1; ms <= 20; ms++) {
            ticks.begin(1_000_000_000L);
            ticks.end(1_000_000_000L + ms * 1_000_000L);
        }
        var s = ticks.snapshot();
        assertEquals(16, s.samples());
        assertEquals(12.5, s.meanMs(), 0.001);
        assertEquals(12.0, s.p50Ms(), 0.001);
        assertEquals(20.0, s.p95Ms(), 0.001);
        assertEquals(20.0, s.p99Ms(), 0.001);
    }

    @Test void resetBetweenServerSessions() {
        TickTimeWindow ticks = new TickTimeWindow(16);
        ticks.begin(10); ticks.end(20);
        assertEquals(1, ticks.snapshot().samples());
        ticks.clear();
        assertEquals(0, ticks.snapshot().samples());
    }

    @Test void ignoresUnstartedAndNegativeSamples() {
        TickTimeWindow ticks = new TickTimeWindow(16);
        ticks.end(123);
        ticks.begin(100);
        ticks.end(99);
        assertEquals(0, ticks.snapshot().samples());
        ticks.begin(5);
        ticks.end(5);
        assertEquals(1, ticks.snapshot().samples());
    }
}
