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
            Set<String> learned, Set<String> equipment, Set<String> ai,
            int actorLevel) {
        public Grants {
            if (actor == null || learned == null || equipment == null || ai == null
                    || actorLevel < 1 || actorLevel > 100)
                throw new IllegalArgumentException("Missing grant source");
            learned = Set.copyOf(learned);
            equipment = Set.copyOf(equipment);
            ai = Set.copyOf(ai);
        }

        /** Compatibility constructor for existing class/AI contract tests. */
        public Grants(Actor actor, String classId, String raceId,
                Set<String> learned, Set<String> equipment, Set<String> ai) {
            this(actor, classId, raceId, learned, equipment, ai, 1);
        }

        public static Grants currentPlayer() {
            return currentPlayer(1);
        }

        public static Grants currentPlayer(int authoritativeLevel) {
            // No selected class or race is persisted in CharacterData v3/v4.
            // Level is server-owned CharacterData.level() or owner-synced HUD level.
            return new Grants(Actor.PLAYER, null, null,
                    Set.of(), Set.of(), Set.of(), authoritativeLevel);
        }

        public static Grants taggedHostileRenewal() {
            // AI level defaults to I; a future AI tier policy must pass a
            // verified level rather than assume player progression.
            return new Grants(Actor.HOSTILE_AI, null, null,
                    Set.of(), Set.of(), Set.of(AbilityCatalog.RENEWAL.id()), 1);
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
        return granted(grants, abilityId)
                && SpellProgression.meetsLevel(abilityId, grants.actorLevel())
                && definition.kind() == AbilityDefinition.Kind.ACTIVE
                && AbilityAccessRules.executableNow(abilityId)
                && (grants.actor() != Actor.HOSTILE_AI
                    || AbilityCatalog.RENEWAL.id().equals(abilityId));
    }
}
