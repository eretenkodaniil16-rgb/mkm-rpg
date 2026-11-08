package com.eretenkodaniil.mkm.encounter;

/** Pure server-side gates for one world-persisted, quest-linked encounter. */
public final class EncounterAutoRules {
    public static final double MIN_WARDEN_DISTANCE_SQR = 24.0 * 24.0;
    public static final double MAX_WARDEN_DISTANCE_SQR = 96.0 * 96.0;
    public static final int MAX_LEVEL = 30;
    private EncounterAutoRules() {}

    public static boolean canBegin(boolean questActive, boolean hasWarden,
            boolean alreadyReserved, boolean allowedWorldTimeBiome,
            boolean peaceful, boolean aliveAndGrounded, double distanceToWardenSquared) {
        return questActive && hasWarden && !alreadyReserved && allowedWorldTimeBiome
                && !peaceful && aliveAndGrounded
                && Double.isFinite(distanceToWardenSquared)
                && distanceToWardenSquared >= MIN_WARDEN_DISTANCE_SQR
                && distanceToWardenSquared <= MAX_WARDEN_DISTANCE_SQR;
    }

    public static int encounterLevel(int playerLevel) {
        return Math.max(1, Math.min(MAX_LEVEL, playerLevel));
    }
}
