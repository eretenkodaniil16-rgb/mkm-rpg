package com.eretenkodaniil.mkm.combat;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * FUTURE server-only replay guard. IDs are minted by the server per actor,
 * never borrowed from a client animation event or a target entity ID.
 * The checker does not execute attacks or mint IDs.
 */
public final class ServerAttackReceiptGate<A> {
    private final Map<A, Long> highestSeen = new WeakHashMap<>();

    public boolean claim(A actor, long serverAttemptId) {
        Objects.requireNonNull(actor, "actor");
        if (serverAttemptId <= 0L) return false;
        Long prior = highestSeen.get(actor);
        if (prior != null && serverAttemptId <= prior) return false;
        highestSeen.put(actor, serverAttemptId);
        return true;
    }

    /** Invoke on relog/despawn/world unload to avoid retaining actors. */
    public void clear(A actor) {
        highestSeen.remove(actor);
    }
}
