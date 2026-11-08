package com.eretenkodaniil.mkm.client.input;

import java.util.EnumMap;

/**
 * Client-side request coalescing, not an authority boundary: server owns
 * stamina, mana, cooldowns, target checks and packet replay protection.
 */
public final class ClientActionGate {
    public enum Action { COMBO, DODGE, RENEWAL }
    private final EnumMap<Action, Long> last = new EnumMap<>(Action.class);
    private Object player;
    private Object world;

    public void session(Object activePlayer, Object activeWorld) {
        if (player != activePlayer || world != activeWorld) {
            player = activePlayer;
            world = activeWorld;
            last.clear();
        }
    }
    public boolean allow(Action action, long tick) {
        if (action == null || tick < 0 || player == null || world == null) return false;
        Long previous = last.get(action);
        if (previous != null) {
            if (tick < previous) { last.clear(); }
            else if (tick - previous < 3) { return false; }
        }
        last.put(action, tick);
        return true;
    }
}
