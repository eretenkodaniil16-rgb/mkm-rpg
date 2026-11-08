package com.eretenkodaniil.mkm.environment;

/** Pure rules: no world scan, network state, damage, XP or render calls. */
public final class EnvironmentVfxBudget {
    public static final byte RAIDER_AMBIENT = 1;
    public static final byte STALKER_AMBIENT = 2;
    public static final byte RAIDER_HIT = 3;
    public static final byte STALKER_HIT = 4;
    public static final byte RUNE_PULSE = 5;

    private EnvironmentVfxBudget() {}

    /** One ambient event per marked mob per 40 ticks (2 seconds). */
    public static boolean ambientDue(long tick, int entityId) {
        return Math.floorMod(tick + entityId, 40) == 0;
    }

    public static boolean known(byte cue) {
        return cue >= RAIDER_AMBIENT && cue <= RUNE_PULSE;
    }

    public static int particles(EnvironmentVisualSettings.Profile profile,
                                boolean reduced, byte cue) {
        if (!known(cue)) return 0;
        if (reduced) {
            return cue <= STALKER_AMBIENT ? 0 : 1;
        }
        return switch (profile) {
            case ECONOMY -> cue <= STALKER_AMBIENT ? 0 : 2;
            case STANDARD -> cue <= STALKER_AMBIENT ? 1 : 4;
            case CINEMATIC -> cue <= STALKER_AMBIENT ? 3 : 7;
        };
    }
}
