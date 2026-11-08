package com.eretenkodaniil.mkm.abilities.visual;

/**
 * Pure client-side termination rule for one-shot cosmetics. Not a gameplay
 * ActionTimeline, spell timer, damage callback or server resource ledger.
 */
public final class AbilityVisualLease {
    private AbilityVisualLease() {}

    public static boolean expired(long startClientTick, long currentClientTick,
            int visualDurationTicks) {
        if (startClientTick < 0 || currentClientTick < startClientTick
                || visualDurationTicks < 0
                || visualDurationTicks > AbilityVisualEvent.MAX_VISUAL_TICKS) {
            return true;
        }
        return currentClientTick - startClientTick >= visualDurationTicks;
    }
}
