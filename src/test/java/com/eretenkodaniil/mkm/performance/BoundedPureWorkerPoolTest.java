package com.eretenkodaniil.mkm.performance;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BoundedPureWorkerPoolTest {
    @Test void dispatchesResultForOwnerThreadAndDiscardsStaleWork() throws Exception {
        try (var workers = new BoundedPureWorkerPool(1, 2)) {
            var submitted = new CountDownLatch(2);
            List<Runnable> pending = java.util.Collections.synchronizedList(new ArrayList<>());
            List<Integer> applied = new ArrayList<>();
            assertTrue(workers.trySubmit(3, value -> value * 2, runnable -> {
                pending.add(runnable);
                submitted.countDown();
            }, value -> value == 3, applied::add));
            assertTrue(workers.trySubmit(5, value -> value * 2, runnable -> {
                pending.add(runnable);
                submitted.countDown();
            }, value -> false, applied::add));
            assertTrue(submitted.await(5, TimeUnit.SECONDS));
            assertTrue(applied.isEmpty(), "No worker may apply game results directly");
            pending.forEach(Runnable::run);
            assertEquals(List.of(6), applied);
        }
    }

    @Test void boundedQueueRejectsExcessWorkWithoutBlockingCaller() throws Exception {
        try (var workers = new BoundedPureWorkerPool(1, 1)) {
            CountDownLatch running = new CountDownLatch(1);
            CountDownLatch unblock = new CountDownLatch(1);
            assertTrue(workers.trySubmit(1, value -> {
                running.countDown();
                try { unblock.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return value;
            }, runnable -> { }, value -> true, value -> { }));
            assertTrue(running.await(5, TimeUnit.SECONDS));
            assertTrue(workers.trySubmit(2, value -> value, runnable -> { }, value -> true, value -> { }));
            assertFalse(workers.trySubmit(3, value -> value, runnable -> { }, value -> true, value -> { }));
            assertEquals(1, workers.rejectedTasks());
            unblock.countDown();
        }
    }
}
