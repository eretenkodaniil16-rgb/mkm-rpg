package com.eretenkodaniil.mkm.abilities;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Code-defined prototype catalog. IDs and descriptive metadata can later be loaded
 * from validated data packs; effect handlers remain explicitly server allowlisted.
 */
public final class AbilityCatalog {
    public static final AbilityDefinition RENEWAL = new AbilityDefinition(
            "mkm:renewal", AbilityDefinition.Kind.ACTIVE, AbilityDefinition.TargetMode.SELF,
            AbilityDefinition.Resource.MANA, AbilityDefinition.Effect.HEAL,
            20.0D, 0, 60, 6.0D);

    public static final AbilityDefinition MAGE_WARD = new AbilityDefinition(
            "mkm:mage_ward", AbilityDefinition.Kind.ACTIVE, AbilityDefinition.TargetMode.SELF,
            AbilityDefinition.Resource.MANA, AbilityDefinition.Effect.BARRIER,
            25.0D, 0, 180, 4.0D);

    private static final Map<String, AbilityDefinition> DEFINITIONS = create();

    private AbilityCatalog() {}

    private static Map<String, AbilityDefinition> create() {
        Map<String, AbilityDefinition> abilities = new LinkedHashMap<>();
        register(abilities, RENEWAL);
        register(abilities, MAGE_WARD);
        // Design-only samples: the server deliberately rejects activation until
        // their target validation, damage/effect integration and cast contracts exist.
        register(abilities, new AbilityDefinition(
                "mkm:arcane_bolt", AbilityDefinition.Kind.ACTIVE, AbilityDefinition.TargetMode.ENTITY_RAY,
                AbilityDefinition.Resource.MANA, AbilityDefinition.Effect.DAMAGE,
                14.0D, 8, 30, 4.0D));
        register(abilities, new AbilityDefinition(
                "mkm:arcane_barrier", AbilityDefinition.Kind.ACTIVE, AbilityDefinition.TargetMode.SELF,
                AbilityDefinition.Resource.MANA, AbilityDefinition.Effect.BARRIER,
                25.0D, 12, 160, 10.0D)); // future channelled barrier, executor disabled
        register(abilities, new AbilityDefinition(
                "mkm:repulsion_wave", AbilityDefinition.Kind.ACTIVE, AbilityDefinition.TargetMode.CONE,
                AbilityDefinition.Resource.MANA, AbilityDefinition.Effect.CONTROL,
                30.0D, 10, 120, 3.0D));
        register(abilities, new AbilityDefinition(
                "mkm:iron_will", AbilityDefinition.Kind.PASSIVE, AbilityDefinition.TargetMode.NONE,
                AbilityDefinition.Resource.NONE, AbilityDefinition.Effect.MODIFIER,
                0.0D, 0, 0, 0.05D));
        return Collections.unmodifiableMap(abilities);
    }

    private static void register(Map<String, AbilityDefinition> map, AbilityDefinition ability) {
        if (map.putIfAbsent(ability.id(), ability) != null) {
            throw new IllegalArgumentException("Duplicate ability: " + ability.id());
        }
    }

    public static AbilityDefinition get(String id) {
        return DEFINITIONS.get(id);
    }

    public static Map<String, AbilityDefinition> all() {
        return DEFINITIONS;
    }
}
