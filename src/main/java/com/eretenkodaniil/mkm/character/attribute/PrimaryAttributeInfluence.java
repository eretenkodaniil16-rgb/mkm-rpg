package com.eretenkodaniil.mkm.character.attribute;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Formula-free influence graph between candidate primary attributes and accepted player modifiers.
 *
 * <p>The graph says that an attribute is allowed to contribute to a modifier. It intentionally
 * does not define coefficients, caps or stacking rules.</p>
 */
public final class PrimaryAttributeInfluence {
    private static final Map<PrimaryAttribute, Set<PlayerModifierId>> DIRECT = buildDirectMap();

    private PrimaryAttributeInfluence() {
    }

    public static Set<PlayerModifierId> directModifiers(PrimaryAttribute attribute) {
        return DIRECT.getOrDefault(attribute, Set.of());
    }

    public static boolean directlyInfluences(PrimaryAttribute attribute, PlayerModifierId modifier) {
        return directModifiers(attribute).contains(modifier);
    }

    public static Map<PrimaryAttribute, Set<PlayerModifierId>> allDirectInfluences() {
        return DIRECT;
    }

    private static Map<PrimaryAttribute, Set<PlayerModifierId>> buildDirectMap() {
        EnumMap<PrimaryAttribute, Set<PlayerModifierId>> map = new EnumMap<>(PrimaryAttribute.class);

        map.put(PrimaryAttribute.STRENGTH, immutableEnumSet(
                PlayerModifierId.PHYSICAL_POWER,
                PlayerModifierId.MELEE_DAMAGE,
                PlayerModifierId.ARMOR_PENETRATION,
                PlayerModifierId.STAGGER_POWER,
                PlayerModifierId.KNOCKBACK_POWER,
                PlayerModifierId.BLOCK_STABILITY));

        map.put(PrimaryAttribute.DEXTERITY, immutableEnumSet(
                PlayerModifierId.ATTACK_SPEED,
                PlayerModifierId.RANGED_DAMAGE,
                PlayerModifierId.BACKSTAB_DAMAGE,
                PlayerModifierId.MOVEMENT_SPEED,
                PlayerModifierId.DODGE_SPEED,
                PlayerModifierId.DODGE_DISTANCE,
                PlayerModifierId.INTERACTION_SPEED));

        map.put(PrimaryAttribute.VITALITY, immutableEnumSet(
                PlayerModifierId.MAX_HEALTH,
                PlayerModifierId.HEALTH_REGENERATION,
                PlayerModifierId.HEALING_RECEIVED,
                PlayerModifierId.PHYSICAL_RESISTANCE,
                PlayerModifierId.SLASHING_RESISTANCE,
                PlayerModifierId.PIERCING_RESISTANCE,
                PlayerModifierId.BLUNT_RESISTANCE,
                PlayerModifierId.STAGGER_RESISTANCE,
                PlayerModifierId.KNOCKBACK_RESISTANCE,
                PlayerModifierId.STATUS_DURATION_RESISTANCE));

        map.put(PrimaryAttribute.ENDURANCE, immutableEnumSet(
                PlayerModifierId.MAX_STAMINA,
                PlayerModifierId.STAMINA_REGENERATION,
                PlayerModifierId.STAMINA_REGEN_DELAY,
                PlayerModifierId.STAMINA_COST,
                PlayerModifierId.DODGE_COST,
                PlayerModifierId.SPRINT_SPEED,
                PlayerModifierId.JUMP_POWER,
                PlayerModifierId.FALL_DAMAGE_RESISTANCE,
                PlayerModifierId.BLOCK_STABILITY));

        map.put(PrimaryAttribute.INTELLIGENCE, immutableEnumSet(
                PlayerModifierId.MAGIC_POWER,
                PlayerModifierId.SPELL_DAMAGE,
                PlayerModifierId.MAGIC_PENETRATION,
                PlayerModifierId.SPELL_CRITICAL_DAMAGE,
                PlayerModifierId.CAST_SPEED,
                PlayerModifierId.SPELL_RANGE,
                PlayerModifierId.SPELL_AREA,
                PlayerModifierId.SPELL_PROJECTILE_SPEED,
                PlayerModifierId.SPELL_DURATION,
                PlayerModifierId.SPELL_STATUS_POWER));

        map.put(PrimaryAttribute.WILLPOWER, immutableEnumSet(
                PlayerModifierId.MAX_MANA,
                PlayerModifierId.MANA_REGENERATION,
                PlayerModifierId.MANA_REGEN_DELAY,
                PlayerModifierId.MANA_COST,
                PlayerModifierId.MAGIC_RESISTANCE,
                PlayerModifierId.ELEMENTAL_RESISTANCE,
                PlayerModifierId.STATUS_RESISTANCE,
                PlayerModifierId.STATUS_DURATION_RESISTANCE,
                PlayerModifierId.CONCENTRATION_STABILITY,
                PlayerModifierId.HEALING_POWER,
                PlayerModifierId.BARRIER_POWER));

        map.put(PrimaryAttribute.PERCEPTION, immutableEnumSet(
                PlayerModifierId.RANGED_DAMAGE,
                PlayerModifierId.CRITICAL_DAMAGE,
                PlayerModifierId.WEAK_POINT_DAMAGE,
                PlayerModifierId.BACKSTAB_DAMAGE,
                PlayerModifierId.SPELL_RANGE));

        return Collections.unmodifiableMap(map);
    }

    private static Set<PlayerModifierId> immutableEnumSet(
            PlayerModifierId first,
            PlayerModifierId... rest) {
        return Collections.unmodifiableSet(EnumSet.of(first, rest));
    }
}
