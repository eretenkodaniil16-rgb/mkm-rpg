package com.eretenkodaniil.mkm.combat.fencing;

import com.eretenkodaniil.mkm.combat.mode.CombatModeRules;

/**
 * Pure Fiore defensive response. Only a genuine server AFTER_DAMAGE block
 * can open the window; only confirmed health damage consumes its riposte.
 * No damage, block, packet or client-pose writes.
 */
public final class FencingRiposteRules {
    public static final long RIPOSTE_WINDOW_TICKS = 40L;
    private FencingRiposteRules() {}

    public static long openedUntil(FencingSchool school, boolean combatMode,
                                   boolean vanillaBlocked, boolean actualShield,
                                   long serverTick) {
        if (school != FencingSchool.FIORE || !combatMode || !vanillaBlocked
                || !actualShield || serverTick < 0L) return 0L;
        return Math.min(Long.MAX_VALUE, serverTick + RIPOSTE_WINDOW_TICKS);
    }

    public static boolean riposte(FencingSchool school, CombatModeRules.Strike originalImpact,
                                  boolean confirmedUnblockedDamage, long serverTick,
                                  long expiryTick) {
        return school == FencingSchool.FIORE
                && originalImpact == CombatModeRules.Strike.QUICK
                && confirmedUnblockedDamage
                && expiryTick > 0L && serverTick >= 0L && serverTick <= expiryTick;
    }
}
