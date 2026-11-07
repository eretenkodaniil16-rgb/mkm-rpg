package com.eretenkodaniil.mkm.combat;

/** Safe, bounded presentation policy for server-confirmed hit effects. */
public final class HitFeedbackRules {
    private HitFeedbackRules() {}

    public static boolean validConfirmedHit(int targetEntityId, float damageTaken) {
        return targetEntityId >= 0 && Float.isFinite(damageTaken) && damageTaken > 0F;
    }

    /** Client predictions must never be treated as a verified damage callback. */
    public enum Evidence { CLIENT_PREDICTION, SERVER_AFTER_DAMAGE }

    public static boolean validServerConfirmedHit(
            Evidence evidence, boolean blocked, int targetEntityId, float damageTaken) {
        return evidence == Evidence.SERVER_AFTER_DAMAGE
                && !blocked && validConfirmedHit(targetEntityId, damageTaken);
    }

    /** Small finite burst; stagger is a little more visible, never unlimited. */
    public static int particleCount(float damageTaken, boolean staggered) {
        if (!validConfirmedHit(0, damageTaken)) return 0;
        int severity = (int) Math.min(4D, Math.ceil(damageTaken / 4D));
        return Math.min(12, 4 + severity + (staggered ? 4 : 0));
    }
}
