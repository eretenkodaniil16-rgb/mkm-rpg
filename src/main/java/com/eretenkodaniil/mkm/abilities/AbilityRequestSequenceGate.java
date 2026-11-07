package com.eretenkodaniil.mkm.abilities;

/**
 * Session-scoped monotonic server replay gate for client cast intentions.
 * The client chooses only a sequence ID, never an effect/damage outcome.
 * The server still checks grants, Combat state, mana, health and cooldown.
 */
public final class AbilityRequestSequenceGate {
    private int lastAccepted;

    public boolean accept(int sequence) {
        if (sequence <= lastAccepted || sequence <= 0) return false;
        lastAccepted = sequence;
        return true;
    }

    public void reset() {
        lastAccepted = 0;
    }
}
