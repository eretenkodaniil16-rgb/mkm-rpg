package com.eretenkodaniil.mkm.performance;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class PerformanceBudgetTest {
    @Test void classifiesOnlySufficientWindows() {
        assertEquals(PerformanceBudget.Pressure.NO_DATA,
                PerformanceBudget.classify(new TickTimeWindow.Summary(60, 10, 10, 10, 10, 12)));
        assertEquals(PerformanceBudget.Pressure.NORMAL,
                PerformanceBudget.classify(new TickTimeWindow.Summary(100, 10, 10, 15, 20, 21)));
        assertEquals(PerformanceBudget.Pressure.CAUTION,
                PerformanceBudget.classify(new TickTimeWindow.Summary(100, 21, 20, 45, 48, 50)));
        assertEquals(PerformanceBudget.Pressure.BUSY,
                PerformanceBudget.classify(new TickTimeWindow.Summary(100, 25, 20, 49, 55, 67)));
    }

    @Test void boundsParallelismForLowCoreComputers() {
        assertEquals(1, PerformanceBudget.maximumWorkers(1));
        assertEquals(1, PerformanceBudget.maximumWorkers(2));
        assertEquals(2, PerformanceBudget.maximumWorkers(4));
        assertEquals(4, PerformanceBudget.maximumWorkers(16));
    }
}
