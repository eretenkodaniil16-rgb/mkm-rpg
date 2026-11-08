package com.eretenkodaniil.mkm.abilities.visual;

import com.eretenkodaniil.mkm.abilities.AbilityCatalog;

/** MNAE semantic actions only. Never authoritative damage or resource state. */
public final class AbilityVisualPolicy {
    public static final String RENEWAL_ACTION_ID = "mkm:renewal_cast";
    public static final int RENEWAL_VISUAL_TICKS = 14;
    public static final String WARD_ACTION_ID = "mkm:arcane_ward_cast";
    public static final int WARD_VISUAL_TICKS = 20;

    private AbilityVisualPolicy() {}

    public static boolean renewalApplied(boolean paid, boolean actorValid, float before, float after) {
        return paid && actorValid && Float.isFinite(before) && Float.isFinite(after)
                && after > before + 0.00001F;
    }

    public static boolean wardApplied(boolean paid, boolean actorValid,
            boolean addedAbsorption) {
        return paid && actorValid && addedAbsorption;
    }

    public static boolean allowedForPlayback(AbilityVisualEvent e) {
        if (e == null || e.phase() != AbilityVisualEvent.Phase.APPLIED
                || e.hand() != AbilityVisualEvent.Hand.MAIN_HAND) return false;
        return (AbilityCatalog.RENEWAL.id().equals(e.abilityId())
                    && RENEWAL_ACTION_ID.equals(e.visualActionId())
                    && e.visualDurationTicks() == RENEWAL_VISUAL_TICKS)
                || ("mkm:mage_ward".equals(e.abilityId())
                    && WARD_ACTION_ID.equals(e.visualActionId())
                    && e.visualDurationTicks() == WARD_VISUAL_TICKS);
    }
}
