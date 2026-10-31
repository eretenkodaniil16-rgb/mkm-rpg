package com.eretenkodaniil.mkm.character.modifier;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Central numeric definitions for the first player-modifier snapshot.
 *
 * <p>Values are conservative tuning defaults. Combat Core may tune them later, but callers should
 * consume this registry rather than hard-code independent defaults and caps.</p>
 */
public final class ModifierDefinitions {
    private static final Map<PlayerModifierId, ModifierDefinition> DEFINITIONS = build();

    private ModifierDefinitions() {
    }

    public static ModifierDefinition get(PlayerModifierId id) {
        ModifierDefinition definition = DEFINITIONS.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("No modifier definition for " + id);
        }
        return definition;
    }

    public static Map<PlayerModifierId, ModifierDefinition> all() {
        return DEFINITIONS;
    }

    private static Map<PlayerModifierId, ModifierDefinition> build() {
        EnumMap<PlayerModifierId, ModifierDefinition> map = new EnumMap<>(PlayerModifierId.class);

        put(map, PlayerModifierId.MAX_HEALTH, ModifierUnit.FLAT, 20.0, 1.0, 200.0, 1.20, 0.60);
        put(map, PlayerModifierId.HEALTH_REGENERATION, ModifierUnit.PER_SECOND, 0.0, 0.0, 10.0, 0.03, 0.015);
        put(map, PlayerModifierId.HEALING_RECEIVED, ModifierUnit.MULTIPLIER, 1.0, 0.25, 2.0, 0.010, 0.005);

        put(map, PlayerModifierId.PHYSICAL_POWER, ModifierUnit.MULTIPLIER, 1.0, 0.25, 3.0, 0.025, 0.0125);
        put(map, PlayerModifierId.MELEE_DAMAGE, ModifierUnit.MULTIPLIER, 1.0, 0.50, 2.5, 0.015, 0.0075);
        put(map, PlayerModifierId.RANGED_DAMAGE, ModifierUnit.MULTIPLIER, 1.0, 0.50, 2.5, 0.012, 0.006);
        put(map, PlayerModifierId.CRITICAL_DAMAGE, ModifierUnit.MULTIPLIER, 1.50, 1.0, 3.0, 0.020, 0.010);
        put(map, PlayerModifierId.BACKSTAB_DAMAGE, ModifierUnit.MULTIPLIER, 1.50, 1.0, 3.0, 0.020, 0.010);
        put(map, PlayerModifierId.WEAK_POINT_DAMAGE, ModifierUnit.MULTIPLIER, 1.25, 1.0, 3.0, 0.025, 0.0125);
        put(map, PlayerModifierId.ARMOR_PENETRATION, ModifierUnit.FRACTION, 0.0, 0.0, 0.75, 0.010, 0.005);
        put(map, PlayerModifierId.STAGGER_POWER, ModifierUnit.MULTIPLIER, 1.0, 0.25, 3.0, 0.030, 0.015);
        put(map, PlayerModifierId.KNOCKBACK_POWER, ModifierUnit.MULTIPLIER, 1.0, 0.25, 2.0, 0.020, 0.010);
        put(map, PlayerModifierId.ATTACK_SPEED, ModifierUnit.MULTIPLIER, 1.0, 0.25, 2.5, 0.012, 0.006);
        put(map, PlayerModifierId.INTERACTION_SPEED, ModifierUnit.MULTIPLIER, 1.0, 0.50, 2.0, 0.010, 0.005);

        put(map, PlayerModifierId.ARMOR, ModifierUnit.FLAT, 0.0, 0.0, 100.0, 0.0, 0.0);
        put(map, PlayerModifierId.PHYSICAL_RESISTANCE, ModifierUnit.FRACTION, 0.0, -0.50, 0.80, 0.008, 0.004);
        put(map, PlayerModifierId.SLASHING_RESISTANCE, ModifierUnit.FRACTION, 0.0, -0.50, 0.80, 0.008, 0.004);
        put(map, PlayerModifierId.PIERCING_RESISTANCE, ModifierUnit.FRACTION, 0.0, -0.50, 0.80, 0.008, 0.004);
        put(map, PlayerModifierId.BLUNT_RESISTANCE, ModifierUnit.FRACTION, 0.0, -0.50, 0.80, 0.008, 0.004);
        put(map, PlayerModifierId.CRITICAL_RESISTANCE, ModifierUnit.FRACTION, 0.0, 0.0, 0.75, 0.0, 0.0);
        put(map, PlayerModifierId.STAGGER_RESISTANCE, ModifierUnit.MULTIPLIER, 1.0, 0.25, 3.0, 0.025, 0.0125);
        put(map, PlayerModifierId.KNOCKBACK_RESISTANCE, ModifierUnit.FRACTION, 0.0, 0.0, 0.90, 0.010, 0.005);
        put(map, PlayerModifierId.BLOCK_EFFICIENCY, ModifierUnit.MULTIPLIER, 1.0, 0.25, 2.0, 0.0, 0.0);
        put(map, PlayerModifierId.BLOCK_STABILITY, ModifierUnit.MULTIPLIER, 1.0, 0.25, 3.0, 0.020, 0.010);

        put(map, PlayerModifierId.MAX_STAMINA, ModifierUnit.FLAT, 100.0, 10.0, 300.0, 3.0, 1.5);
        put(map, PlayerModifierId.STAMINA_REGENERATION, ModifierUnit.PER_SECOND, 10.0, 0.0, 50.0, 0.25, 0.125);
        put(map, PlayerModifierId.STAMINA_REGEN_DELAY, ModifierUnit.SECONDS, 1.50, 0.10, 5.0, -0.030, -0.015);
        put(map, PlayerModifierId.STAMINA_COST, ModifierUnit.MULTIPLIER, 1.0, 0.25, 2.0, -0.012, -0.006);
        put(map, PlayerModifierId.DODGE_COST, ModifierUnit.MULTIPLIER, 1.0, 0.25, 2.0, -0.012, -0.006);
        put(map, PlayerModifierId.COOLDOWN_RECOVERY, ModifierUnit.MULTIPLIER, 1.0, 0.50, 2.0, 0.0, 0.0);

        put(map, PlayerModifierId.MOVEMENT_SPEED, ModifierUnit.MULTIPLIER, 1.0, 0.50, 1.50, 0.008, 0.004);
        put(map, PlayerModifierId.SPRINT_SPEED, ModifierUnit.MULTIPLIER, 1.0, 0.50, 1.50, 0.012, 0.006);
        put(map, PlayerModifierId.JUMP_POWER, ModifierUnit.MULTIPLIER, 1.0, 0.50, 1.50, 0.008, 0.004);
        put(map, PlayerModifierId.DODGE_SPEED, ModifierUnit.MULTIPLIER, 1.0, 0.50, 2.0, 0.015, 0.0075);
        put(map, PlayerModifierId.DODGE_DISTANCE, ModifierUnit.MULTIPLIER, 1.0, 0.50, 2.0, 0.016, 0.008);
        put(map, PlayerModifierId.FALL_DAMAGE_RESISTANCE, ModifierUnit.FRACTION, 0.0, 0.0, 0.90, 0.016, 0.008);

        put(map, PlayerModifierId.STATUS_RESISTANCE, ModifierUnit.FRACTION, 0.0, 0.0, 0.80, 0.008, 0.004);
        put(map, PlayerModifierId.STATUS_DURATION_RESISTANCE, ModifierUnit.FRACTION, 0.0, 0.0, 0.75, 0.008, 0.004);
        put(map, PlayerModifierId.EXPERIENCE_GAIN, ModifierUnit.MULTIPLIER, 1.0, 0.10, 3.0, 0.0, 0.0);
        put(map, PlayerModifierId.LOOT_BONUS, ModifierUnit.MULTIPLIER, 1.0, 0.10, 3.0, 0.0, 0.0);

        put(map, PlayerModifierId.MAGIC_POWER, ModifierUnit.MULTIPLIER, 1.0, 0.25, 3.0, 0.025, 0.0125);
        put(map, PlayerModifierId.SPELL_DAMAGE, ModifierUnit.MULTIPLIER, 1.0, 0.50, 2.5, 0.015, 0.0075);
        put(map, PlayerModifierId.MAGIC_PENETRATION, ModifierUnit.FRACTION, 0.0, 0.0, 0.75, 0.010, 0.005);
        put(map, PlayerModifierId.SPELL_CRITICAL_DAMAGE, ModifierUnit.MULTIPLIER, 1.50, 1.0, 3.0, 0.015, 0.0075);
        put(map, PlayerModifierId.CAST_SPEED, ModifierUnit.MULTIPLIER, 1.0, 0.50, 2.5, 0.012, 0.006);
        put(map, PlayerModifierId.SPELL_RANGE, ModifierUnit.MULTIPLIER, 1.0, 0.50, 2.0, 0.010, 0.005);
        put(map, PlayerModifierId.SPELL_AREA, ModifierUnit.MULTIPLIER, 1.0, 0.50, 2.0, 0.012, 0.006);
        put(map, PlayerModifierId.SPELL_PROJECTILE_SPEED, ModifierUnit.MULTIPLIER, 1.0, 0.50, 2.0, 0.012, 0.006);
        put(map, PlayerModifierId.SPELL_DURATION, ModifierUnit.MULTIPLIER, 1.0, 0.50, 2.0, 0.016, 0.008);
        put(map, PlayerModifierId.SPELL_STATUS_POWER, ModifierUnit.MULTIPLIER, 1.0, 0.25, 3.0, 0.020, 0.010);
        put(map, PlayerModifierId.HEALING_POWER, ModifierUnit.MULTIPLIER, 1.0, 0.25, 3.0, 0.020, 0.010);
        put(map, PlayerModifierId.BARRIER_POWER, ModifierUnit.MULTIPLIER, 1.0, 0.25, 3.0, 0.020, 0.010);
        put(map, PlayerModifierId.CONCENTRATION_STABILITY, ModifierUnit.MULTIPLIER, 1.0, 0.25, 3.0, 0.025, 0.0125);

        put(map, PlayerModifierId.MAX_MANA, ModifierUnit.FLAT, 100.0, 0.0, 300.0, 3.0, 1.5);
        put(map, PlayerModifierId.MANA_REGENERATION, ModifierUnit.PER_SECOND, 5.0, 0.0, 30.0, 0.20, 0.10);
        put(map, PlayerModifierId.MANA_REGEN_DELAY, ModifierUnit.SECONDS, 2.0, 0.10, 8.0, -0.040, -0.020);
        put(map, PlayerModifierId.MANA_COST, ModifierUnit.MULTIPLIER, 1.0, 0.25, 2.0, -0.012, -0.006);

        put(map, PlayerModifierId.MAGIC_RESISTANCE, ModifierUnit.FRACTION, 0.0, -0.50, 0.80, 0.008, 0.004);
        put(map, PlayerModifierId.ELEMENTAL_RESISTANCE, ModifierUnit.FRACTION, 0.0, -0.50, 0.80, 0.008, 0.004);

        if (map.size() != PlayerModifierId.values().length) {
            throw new IllegalStateException(
                    "Modifier definition registry incomplete: " + map.size()
                            + "/" + PlayerModifierId.values().length);
        }
        return Collections.unmodifiableMap(map);
    }

    private static void put(
            EnumMap<PlayerModifierId, ModifierDefinition> map,
            PlayerModifierId id,
            ModifierUnit unit,
            double baseValue,
            double minValue,
            double maxValue,
            double primaryPerEffectivePoint,
            double secondaryPerEffectivePoint) {
        ModifierDefinition previous = map.put(
                id,
                new ModifierDefinition(
                        id,
                        unit,
                        baseValue,
                        minValue,
                        maxValue,
                        primaryPerEffectivePoint,
                        secondaryPerEffectivePoint));
        if (previous != null) {
            throw new IllegalStateException("Duplicate modifier definition: " + id);
        }
    }
}
