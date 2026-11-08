package com.eretenkodaniil.mkm.combat.mode;

/** Pure input isolation: vanilla mining and noncombat LMB always bypass. */
public final class CombatModeInputRules {
    private CombatModeInputRules() {}
    public static boolean intercept(boolean serverAckEnabled, boolean alreadyHolding,
                                    boolean screenOpen, boolean playerEligible,
                                    boolean swordMainhand, boolean entityHit, boolean canSend) {
        if (!serverAckEnabled) return false;
        if (alreadyHolding) return true;
        return !screenOpen && playerEligible && swordMainhand && entityHit && canSend;
    }
}
