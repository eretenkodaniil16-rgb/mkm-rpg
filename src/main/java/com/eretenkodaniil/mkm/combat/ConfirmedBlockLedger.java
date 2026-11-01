package com.eretenkodaniil.mkm.combat;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * A one-shot, server-thread-only receipt for Fabric damage events.
 *
 * Fabric invokes ALLOW_DAMAGE and AFTER_DAMAGE inside the same synchronous
 * LivingEntity.hurtServer call. A new attempt REPLACES any stale receipt:
 * cancellations, repeated DamageSource instances or nested callbacks cannot
 * leave old receipts that may be incorrectly charged later.
 *
 * If a nested damage attempt interleaves with an outer one, the superseded
 * attempt is deliberately ignored (fail closed) rather than charged twice.
 */
public final class ConfirmedBlockLedger<P, S> {
    private static final long MAX_CALLBACK_DELAY_TICKS = 1L;
    private final Map<P, Attempt<S>> latest = new WeakHashMap<>();

    /** Called once per ALLOW_DAMAGE invocation, whether or not it later blocks. */
    public void begin(P player, S source, long tick) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(source, "source");
        if (tick < 0L) {
            latest.remove(player);
            return;
        }
        latest.put(player, new Attempt<>(source, tick));
    }

    /** The one matching AFTER_DAMAGE callback consumes the receipt permanently. */
    public boolean consume(P player, S source, long tick) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(source, "source");
        Attempt<S> attempt = latest.remove(player);
        return attempt != null && attempt.source() == source
                && tick >= attempt.tick()
                && tick - attempt.tick() <= MAX_CALLBACK_DELAY_TICKS;
    }

    public void clear(P player) {
        latest.remove(player);
    }

    private record Attempt<S>(S source, long tick) {}
}
