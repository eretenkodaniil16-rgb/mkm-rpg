package com.eretenkodaniil.mkm.character.attribute;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Influence graph between approved primary attributes and accepted player modifiers.
 *
 * <p>The graph distinguishes primary and secondary influence without fixing numerical
 * coefficients yet. This lets Combat Core introduce tunable formulas later while preserving the
 * intended build roles and multi-attribute dependencies.</p>
 */
public final class PrimaryAttributeInfluence {
    public enum InfluenceTier {
        PRIMARY,
        SECONDARY
    }

    private static final Map<PrimaryAttribute, Map<PlayerModifierId, InfluenceTier>> DIRECT = buildDirectMap();

    private PrimaryAttributeInfluence() {
    }

    public static Set<PlayerModifierId> directModifiers(PrimaryAttribute attribute) {
        return DIRECT.getOrDefault(attribute, Map.of()).keySet();
    }

    public static boolean directlyInfluences(PrimaryAttribute attribute, PlayerModifierId modifier) {
        return DIRECT.getOrDefault(attribute, Map.of()).containsKey(modifier);
    }

    public static InfluenceTier influenceTier(PrimaryAttribute attribute, PlayerModifierId modifier) {
        return DIRECT.getOrDefault(attribute, Map.of()).get(modifier);
    }

    public static Map<PlayerModifierId, InfluenceTier> tieredModifiers(PrimaryAttribute attribute) {
        return DIRECT.getOrDefault(attribute, Map.of());
    }

    public static Map<PrimaryAttribute, Map<PlayerModifierId, InfluenceTier>> allTieredInfluences() {
        return DIRECT;
    }

    private static Map<PrimaryAttribute, Map<PlayerModifierId, InfluenceTier>> buildDirectMap() {
        EnumMap<PrimaryAttribute, Map<PlayerModifierId, InfluenceTier>> map =
                new EnumMap<>(PrimaryAttribute.class);

        map.put(PrimaryAttribute.STRENGTH, influenceMap(
                immutableEnumSet(
                        PlayerModifierId.PHYSICAL_POWER,
                        PlayerModifierId.MELEE_DAMAGE,
                        PlayerModifierId.STAGGER_POWER,
                        PlayerModifierId.KNOCKBACK_POWER),
                immutableEnumSet(
                        PlayerModifierId.ARMOR_PENETRATION,
                        PlayerModifierId.BLOCK_STABILITY)));

        map.put(PrimaryAttribute.DEXTERITY, influenceMap(
                immutableEnumSet(
                        PlayerModifierId.ATTACK_SPEED,
                        PlayerModifierId.RANGED_DAMAGE,
                        PlayerModifierId.BACKSTAB_DAMAGE,
                        PlayerModifierId.DODGE_SPEED,
                        PlayerModifierId.INTERACTION_SPEED),
                immutableEnumSet(
                        PlayerModifierId.MOVEMENT_SPEED,
                        PlayerModifierId.DODGE_DISTANCE)));

        map.put(PrimaryAttribute.VITALITY, influenceMap(
                immutableEnumSet(
                        PlayerModifierId.MAX_HEALTH,
                        PlayerModifierId.HEALING_RECEIVED,
                        PlayerModifierId.PHYSICAL_RESISTANCE,
                        PlayerModifierId.STAGGER_RESISTANCE,
                        PlayerModifierId.KNOCKBACK_RESISTANCE),
                immutableEnumSet(
                        PlayerModifierId.HEALTH_REGENERATION,
                        PlayerModifierId.SLASHING_RESISTANCE,
                        PlayerModifierId.PIERCING_RESISTANCE,
                        PlayerModifierId.BLUNT_RESISTANCE,
                        PlayerModifierId.STATUS_DURATION_RESISTANCE)));

        map.put(PrimaryAttribute.ENDURANCE, influenceMap(
                immutableEnumSet(
                        PlayerModifierId.MAX_STAMINA,
                        PlayerModifierId.STAMINA_REGENERATION,
                        PlayerModifierId.STAMINA_REGEN_DELAY,
                        PlayerModifierId.STAMINA_COST,
                        PlayerModifierId.DODGE_COST,
                        PlayerModifierId.BLOCK_STABILITY),
                immutableEnumSet(
                        PlayerModifierId.SPRINT_SPEED,
                        PlayerModifierId.JUMP_POWER,
                        PlayerModifierId.FALL_DAMAGE_RESISTANCE)));

        map.put(PrimaryAttribute.INTELLIGENCE, influenceMap(
                immutableEnumSet(
                        PlayerModifierId.MAGIC_POWER,
                        PlayerModifierId.SPELL_DAMAGE,
                        PlayerModifierId.MAGIC_PENETRATION,
                        PlayerModifierId.CAST_SPEED,
                        PlayerModifierId.SPELL_RANGE,
                        PlayerModifierId.SPELL_STATUS_POWER),
                immutableEnumSet(
                        PlayerModifierId.SPELL_CRITICAL_DAMAGE,
                        PlayerModifierId.SPELL_AREA,
                        PlayerModifierId.SPELL_PROJECTILE_SPEED,
                        PlayerModifierId.SPELL_DURATION)));

        map.put(PrimaryAttribute.WILLPOWER, influenceMap(
                immutableEnumSet(
                        PlayerModifierId.MAX_MANA,
                        PlayerModifierId.MANA_REGENERATION,
                        PlayerModifierId.MANA_REGEN_DELAY,
                        PlayerModifierId.MANA_COST,
                        PlayerModifierId.MAGIC_RESISTANCE,
                        PlayerModifierId.STATUS_RESISTANCE,
                        PlayerModifierId.CONCENTRATION_STABILITY,
                        PlayerModifierId.HEALING_POWER,
                        PlayerModifierId.BARRIER_POWER),
                immutableEnumSet(
                        PlayerModifierId.ELEMENTAL_RESISTANCE,
                        PlayerModifierId.STATUS_DURATION_RESISTANCE)));

        map.put(PrimaryAttribute.PERCEPTION, influenceMap(
                immutableEnumSet(
                        PlayerModifierId.CRITICAL_DAMAGE,
                        PlayerModifierId.WEAK_POINT_DAMAGE),
                immutableEnumSet(
                        PlayerModifierId.RANGED_DAMAGE,
                        PlayerModifierId.BACKSTAB_DAMAGE,
                        PlayerModifierId.SPELL_RANGE)));

        return Collections.unmodifiableMap(map);
    }

    private static Map<PlayerModifierId, InfluenceTier> influenceMap(
            Set<PlayerModifierId> primary,
            Set<PlayerModifierId> secondary) {
        EnumMap<PlayerModifierId, InfluenceTier> map = new EnumMap<>(PlayerModifierId.class);
        primary.forEach(modifier -> map.put(modifier, InfluenceTier.PRIMARY));
        secondary.forEach(modifier -> map.put(modifier, InfluenceTier.SECONDARY));
        return Collections.unmodifiableMap(map);
    }

    private static Set<PlayerModifierId> immutableEnumSet(
            PlayerModifierId first,
            PlayerModifierId... rest) {
        return Collections.unmodifiableSet(EnumSet.of(first, rest));
    }
}
