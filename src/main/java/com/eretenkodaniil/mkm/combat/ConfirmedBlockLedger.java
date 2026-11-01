package com.eretenkodaniil.mkm.combat;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Server-thread-only receipt ledger for actual Fabric damage attempts.
 *
 * Every ALLOW_DAMAGE invocation mints a receipt, including when Minecraft reuses
 * the SAME DamageSource instance. AFTER_DAMAGE can consume one matching receipt
 * once when the attempted damage was blocked by a vanilla shield.
 *
 * Source identity alone is NOT a hit identity.
 */
public final class ConfirmedBlockLedger<P, S> {
    private static final int MAX_PENDING_PER_PLAYER = 64;
    private static final long MAX_CALLBACK_DELAY_TICKS = 1L;
    private final Map<P, ArrayDeque<Attempt<S>>> pending = new WeakHashMap<>();

    /** Called once per Fabric ALLOW_DAMAGE event, before damage resolution. */
    public void begin(P player, S source, long tick) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(source, "source");
        if (tick < 0L) return;
        ArrayDeque<Attempt<S>> attempts = pending.computeIfAbsent(
                player, ignored -> new ArrayDeque<>());
        purge(attempts, tick);
        if (attempts.size() >= MAX_PENDING_PER_PLAYER) {
            // Bound memory under malicious/cancelled damage spam; keep newest
            // receipts, rather than allowing a stale source to veto new blocks.
            attempts.removeFirst();
        }
        attempts.addLast(new Attempt<>(source, tick));
    }

    /**
     * Consume exactly one pending damage attempt; never consumes a second
     * callback for the same attempt. A new begin() permits a NEW legitimate hit
     * with the same DamageSource at the same tick.
     */
    public boolean consume(P player, S source, long tick) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(source, "source");
        if (tick < 0L) return false;
        ArrayDeque<Attempt<S>> attempts = pending.get(player);
        if (attempts == null) return false;
        purge(attempts, tick);
        for (Iterator<Attempt<S>> it = attempts.descendingIterator(); it.hasNext();) {
            Attempt<S> attempt = it.next();
            if (attempt.source() == source) {
                it.remove();
                if (attempts.isEmpty()) pending.remove(player);
                return true;
            }
        }
        if (attempts.isEmpty()) pending.remove(player);
        return false;
    }

    public void clear(P player) {
        pending.remove(player);
    }

    private static <S> void purge(ArrayDeque<Attempt<S>> attempts, long tick) {
        attempts.removeIf(attempt -> tick < attempt.tick()
                || tick - attempt.tick() > MAX_CALLBACK_DELAY_TICKS);
    }

    private record Attempt<S>(S source, long tick) {}
}
