package com.eretenkodaniil.mkm.abilities;

import java.util.Set;

/**
 * One pure grant policy for classes, races, equipment, learned grants and AI.
 * The policy is not a saved character state or an AI/Combat action timeline.
 */
public final class AbilityGrantPolicy {
    public enum Actor { PLAYER, HOSTILE_AI }

    public record Grants(
            Actor actor, String classId, String raceId,
            Set<String> learned, Set<String> equipment, Set<String> ai) {
        public Grants {
            if (actor == null || learned == null || equipment == null || ai == null)
                throw new IllegalArgumentException("Missing grant source");
            learned = Set.copyOf(learned);
            equipment = Set.copyOf(equipment);
            ai = Set.copyOf(ai);
        }

        public static Grants currentPlayer() {
            // CharacterData v3/v4 does not yet store a chosen class/race/grants.
            // Keep existing public Renewal accessible, do not invent a class.
            return new Grants(Actor.PLAYER, null, null, Set.of(), Set.of(), Set.of());
        }

        public static Grants taggedHostileRenewal() {
            return new Grants(Actor.HOSTILE_AI, null, null,
                    Set.of(), Set.of(), Set.of(AbilityCatalog.RENEWAL.id()));
        }
    }

    private AbilityGrantPolicy() {}

    public static boolean granted(Grants grants, String abilityId) {
        if (grants == null || !AbilityIds.valid(abilityId)
                || AbilityCatalog.get(abilityId) == null) return false;
        if (grants.actor() == Actor.HOSTILE_AI) {
            // AI cannot use a player's class, race, equipment or learned list.
            return grants.ai().contains(abilityId);
        }
        return AbilityCatalog.RENEWAL.id().equals(abilityId)
                || AbilityArchetypes.classGrants(grants.classId()).contains(abilityId)
                || AbilityArchetypes.raceGrants(grants.raceId()).contains(abilityId)
                || grants.learned().contains(abilityId)
                || grants.equipment().contains(abilityId);
    }

    public static boolean executable(Grants grants, String abilityId) {
        AbilityDefinition definition = AbilityCatalog.get(abilityId);
        return granted(grants, abilityId) && definition.kind() == AbilityDefinition.Kind.ACTIVE
                && AbilityAccessRules.executableNow(abilityId)
                && (grants.actor() != Actor.HOSTILE_AI
                    || AbilityCatalog.RENEWAL.id().equals(abilityId));
    }
}
