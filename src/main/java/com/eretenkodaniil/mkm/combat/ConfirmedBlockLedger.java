package com.eretenkodaniil.mkm.combat;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Transient server-thread deduplication for Fabric AFTER_DAMAGE block callbacks.
 * Same defender + exact DamageSource object within adjacent ticks is charged
 * only once. Distinct damage sources in the same tick remain separate blocks.
 */
public final class ConfirmedBlockLedger<P, S> {
    private static final int MAX_SOURCES_PER_PLAYER = 64;
    private static final long RETENTION_TICKS = 1L;
    private final Map<P, IdentityHashMap<S, Long>> recent = new WeakHashMap<>();

    public boolean record(P player, S source, long gameTick) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(source, "source");
        if (gameTick < 0L) return false;
        IdentityHashMap<S, Long> seen = recent.computeIfAbsent(
                player, ignored -> new IdentityHashMap<>());
        seen.entrySet().removeIf(entry ->
                gameTick < entry.getValue() || gameTick - entry.getValue() > RETENTION_TICKS);
        if (seen.containsKey(source)) return false;
        if (seen.size() >= MAX_SOURCES_PER_PLAYER) return false; // fail closed
        seen.put(source, gameTick);
        return true;
    }

    public void clear(P player) {
        recent.remove(player);
    }
}
