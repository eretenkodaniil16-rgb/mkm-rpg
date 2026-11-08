package com.eretenkodaniil.mkm.environment;

/**
 * Pure preliminary eligibility gate for a player starting Encounter M1.
 * No grant, damage, spawning or quest data is owned here.
 */
public final class ShowcaseAmbushGate {
    public static final double MAX_DISTANCE_SQUARED = 48.0 * 48.0;

    private ShowcaseAmbushGate() {}

    public static boolean allowed(boolean alive, boolean spectator, boolean characterV4,
                                  boolean originSelected, boolean runeReceipt,
                                  boolean confirmedPlacedTowerNearby) {
        return alive && !spectator && characterV4 && originSelected
                && runeReceipt && confirmedPlacedTowerNearby;
    }
}
