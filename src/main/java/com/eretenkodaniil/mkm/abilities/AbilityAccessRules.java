package com.eretenkodaniil.mkm.abilities;

import java.util.Set;

/**
 * Compatibility facade for the existing M2 grant query. Class, race, equipment
 * and AI all resolve through the SINGLE AbilityGrantPolicy implementation.
 * Effect execution remains separately server allowlisted.
 */
public final class AbilityAccessRules {
    private AbilityAccessRules() {}

    public static boolean hasGrant(
            String abilityId, Set<String> learned, Set<String> equipmentGranted) {
        if (learned == null || equipmentGranted == null) return false;
        return AbilityGrantPolicy.granted(
                new AbilityGrantPolicy.Grants(AbilityGrantPolicy.Actor.PLAYER,
                        null, null, learned, equipmentGranted, Set.of()), abilityId);
    }

    /** Explicitly allowlisted server effects; declarative grants cannot widen it. */
    public static boolean executableNow(String abilityId) {
        return AbilityCatalog.RENEWAL.id().equals(abilityId);
    }
}
