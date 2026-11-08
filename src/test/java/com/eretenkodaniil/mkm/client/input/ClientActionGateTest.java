package com.eretenkodaniil.mkm.client.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientActionGateTest {
    @Test void coalescesDuplicateKeysAndPaletteClicksAcrossSameTick() {
        var gate = new ClientActionGate();
        gate.session(new Object(), new Object());
        assertTrue(gate.allow(ClientActionGate.Action.COMBO, 100));
        assertFalse(gate.allow(ClientActionGate.Action.COMBO, 100));
        assertFalse(gate.allow(ClientActionGate.Action.COMBO, 101));
        assertTrue(gate.allow(ClientActionGate.Action.COMBO, 103));
        assertTrue(gate.allow(ClientActionGate.Action.RENEWAL, 103));
    }
    @Test void newWorldOrRespawnResetsTheGate() {
        var gate = new ClientActionGate();
        Object p = new Object(), w = new Object();
        gate.session(p, w);
        assertTrue(gate.allow(ClientActionGate.Action.DODGE, 501));
        gate.session(p, new Object());
        assertTrue(gate.allow(ClientActionGate.Action.DODGE, 1));
        gate.session(null, null);
        assertFalse(gate.allow(ClientActionGate.Action.DODGE, 2));
    }
    @Test void clockRollbackDoesNotLockOutNextSession() {
        var gate = new ClientActionGate();
        gate.session(new Object(), new Object());
        assertTrue(gate.allow(ClientActionGate.Action.RENEWAL, 900));
        assertTrue(gate.allow(ClientActionGate.Action.RENEWAL, 0));
    }
}
