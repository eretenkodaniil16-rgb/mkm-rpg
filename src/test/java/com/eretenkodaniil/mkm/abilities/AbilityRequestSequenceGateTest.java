package com.eretenkodaniil.mkm.abilities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityRequestSequenceGateTest {
    @Test void duplicateReorderedAndInvalidRequestsCannotRecastAfterCooldown() {
        var gate = new AbilityRequestSequenceGate();
        assertFalse(gate.accept(0));
        assertTrue(gate.accept(1));
        assertFalse(gate.accept(1));
        assertFalse(gate.accept(-1));
        assertTrue(gate.accept(2));
        assertFalse(gate.accept(1)); // old packet after cooldown still rejected
        assertTrue(gate.accept(Integer.MAX_VALUE));
        assertFalse(gate.accept(Integer.MAX_VALUE));
        gate.reset(); // new network session
        assertTrue(gate.accept(1));
    }

    @Test void independentPlayersHaveIndependentMonotonicSessions() {
        var a = new AbilityRequestSequenceGate();
        var b = new AbilityRequestSequenceGate();
        assertTrue(a.accept(1));
        assertTrue(b.accept(1));
        assertFalse(a.accept(1));
        assertTrue(b.accept(2));
        assertTrue(a.accept(2));
    }
}
