package com.eretenkodaniil.mkm.performance;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Opt-in worker utility for pure CPU planning on immutable snapshots; NOT started by M0.
 * No worker may read Entity/World/ChunkAccess or invoke Minecraft mutations.
 * Validation and result application run on the owner thread via dispatchToOwner.
 */
public final class BoundedPureWorkerPool implements AutoCloseable {
    private final ThreadPoolExecutor executor;
    private final AtomicInteger rejected = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();

    public BoundedPureWorkerPool(int workers, int pending) {
        if (workers < 1 || workers > 8 || pending < 1 || pending > 4096) {
            throw new IllegalArgumentException("Invalid bounded worker pool size");
        }
        AtomicInteger nextId = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread t = new Thread(runnable, "MKM-Pure-Worker-" + nextId.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        executor = new ThreadPoolExecutor(workers, workers, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(pending), factory, new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Returns false when queue is full or closed. Inputs must be immutable values and
     * the planner must be pure. A stale result is discarded on the owning thread.
     * NEVER block the server tick waiting for a worker future.
     */
    public <S, R> boolean trySubmit(S snapshot, Function<S, R> purePlan,
            Consumer<Runnable> dispatchToOwner, Predicate<S> isCurrent,
            Consumer<R> applyOnOwner) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(purePlan);
        Objects.requireNonNull(dispatchToOwner);
        Objects.requireNonNull(isCurrent);
        Objects.requireNonNull(applyOnOwner);
        try {
            executor.execute(() -> {
                try {
                    R result = purePlan.apply(snapshot);
                    dispatchToOwner.accept(() -> {
                        if (isCurrent.test(snapshot)) applyOnOwner.accept(result);
                    });
                } catch (RuntimeException ex) {
                    failed.incrementAndGet();
                }
            });
            return true;
        } catch (RejectedExecutionException rejectedExecution) {
            rejected.incrementAndGet();
            return false;
        }
    }

    public int queuedTasks() { return executor.getQueue().size(); }
    public int rejectedTasks() { return rejected.get(); }
    public int failedTasks() { return failed.get(); }

    @Override
    public void close() { executor.shutdownNow(); }
}
