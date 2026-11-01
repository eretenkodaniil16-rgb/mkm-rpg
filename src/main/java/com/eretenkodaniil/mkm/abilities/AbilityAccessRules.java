package com.eretenkodaniil.mkm.abilities;

import java.util.Set;

/**
 * Pure, future-facing grant checks. Character Core owns learned ability IDs,
 * equipment owns temporary grants. Nothing is persisted in this class.
 * Only Renewal is public in the current playable prototype.
 */
public final class AbilityAccessRules {
    private AbilityAccessRules() {}

    public static boolean hasGrant(
            String abilityId, Set<String> learned, Set<String> equipmentGranted) {
        if (!AbilityIds.valid(abilityId) || AbilityCatalog.get(abilityId) == null
                || learned == null || equipmentGranted == null) return false;
        return AbilityCatalog.RENEWAL.id().equals(abilityId)
                || learned.contains(abilityId) || equipmentGranted.contains(abilityId);
    }

    /** Runtime execution is separately controlled by server handler registration. */
    public static boolean executableNow(String abilityId) {
        return AbilityCatalog.RENEWAL.id().equals(abilityId);
    }
}
