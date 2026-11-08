package com.eretenkodaniil.mkm.client.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GamepadButtonEdgesTest {
    @Test void pressesAreEdgesNotRepeatedHeldPackets() {
        var d = new GamepadButtonEdges();
        assertEquals(GamepadButtonEdges.COMBO, d.rising(GamepadButtonEdges.COMBO));
        assertEquals(0, d.rising(GamepadButtonEdges.COMBO));
        assertEquals(GamepadButtonEdges.DODGE, d.rising(
                GamepadButtonEdges.COMBO | GamepadButtonEdges.DODGE));
        assertEquals(0, d.rising(0));
        assertEquals(GamepadButtonEdges.COMBO, d.rising(GamepadButtonEdges.COMBO));
    }
    @Test void connectOrReopenDoesNotTriggerHeldButtons() {
        var d = new GamepadButtonEdges();
        d.reset(GamepadButtonEdges.COMBO | GamepadButtonEdges.RENEWAL);
        assertEquals(0, d.rising(GamepadButtonEdges.COMBO | GamepadButtonEdges.RENEWAL));
        assertEquals(0, d.rising(0));
        assertEquals(GamepadButtonEdges.RENEWAL, d.rising(GamepadButtonEdges.RENEWAL));
    }
}
