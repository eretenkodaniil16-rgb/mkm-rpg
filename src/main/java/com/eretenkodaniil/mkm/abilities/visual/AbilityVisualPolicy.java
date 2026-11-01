package com.eretenkodaniil.mkm.abilities.visual;

import com.eretenkodaniil.mkm.abilities.AbilityCatalog;

/** MNAE-neutral IDs and application gate. Does not add gameplay cast phases. */
public final class AbilityVisualPolicy {
    public static final String RENEWAL_ACTION_ID = "mkm:renewal_cast";
    public static final int RENEWAL_VISUAL_TICKS = 14;

    private AbilityVisualPolicy() {}

    /** Only actual health gain may launch successful Renewal visual feedback. */
    public static boolean renewalApplied(
            boolean paid, boolean serverActorValid, float before, float after) {
        return paid && serverActorValid && Float.isFinite(before) && Float.isFinite(after)
                && after > before + 0.00001F;
    }

    /** Client-side cosmetic allowlist. Other abilities require reviewed handlers. */
    public static boolean allowedForPlayback(AbilityVisualEvent event) {
        return event != null && AbilityCatalog.RENEWAL.id().equals(event.abilityId())
                && RENEWAL_ACTION_ID.equals(event.visualActionId())
                && event.phase() == AbilityVisualEvent.Phase.APPLIED
                && event.hand() == AbilityVisualEvent.Hand.MAIN_HAND
                && event.visualDurationTicks() == RENEWAL_VISUAL_TICKS;
    }
}
