package com.eretenkodaniil.mkm.character.attribute;

/**
 * Stable Java vocabulary for player-side derived modifiers.
 *
 * <p>This enum intentionally carries identifiers only. Numerical defaults, caps and aggregation
 * semantics belong to the future modifier-definition layer rather than the primary-attribute
 * vocabulary.</p>
 */
public enum PlayerModifierId {
    MAX_HEALTH("max_health"),
    HEALTH_REGENERATION("health_regeneration"),
    HEALING_RECEIVED("healing_received"),

    PHYSICAL_POWER("physical_power"),
    MELEE_DAMAGE("melee_damage"),
    RANGED_DAMAGE("ranged_damage"),
    CRITICAL_DAMAGE("critical_damage"),
    BACKSTAB_DAMAGE("backstab_damage"),
    WEAK_POINT_DAMAGE("weak_point_damage"),
    ARMOR_PENETRATION("armor_penetration"),
    STAGGER_POWER("stagger_power"),
    KNOCKBACK_POWER("knockback_power"),
    ATTACK_SPEED("attack_speed"),
    INTERACTION_SPEED("interaction_speed"),

    ARMOR("armor"),
    PHYSICAL_RESISTANCE("physical_resistance"),
    SLASHING_RESISTANCE("slashing_resistance"),
    PIERCING_RESISTANCE("piercing_resistance"),
    BLUNT_RESISTANCE("blunt_resistance"),
    CRITICAL_RESISTANCE("critical_resistance"),
    STAGGER_RESISTANCE("stagger_resistance"),
    KNOCKBACK_RESISTANCE("knockback_resistance"),
    BLOCK_EFFICIENCY("block_efficiency"),
    BLOCK_STABILITY("block_stability"),

    MAX_STAMINA("max_stamina"),
    STAMINA_REGENERATION("stamina_regeneration"),
    STAMINA_REGEN_DELAY("stamina_regen_delay"),
    STAMINA_COST("stamina_cost"),
    DODGE_COST("dodge_cost"),
    COOLDOWN_RECOVERY("cooldown_recovery"),

    MOVEMENT_SPEED("movement_speed"),
    SPRINT_SPEED("sprint_speed"),
    JUMP_POWER("jump_power"),
    DODGE_SPEED("dodge_speed"),
    DODGE_DISTANCE("dodge_distance"),
    FALL_DAMAGE_RESISTANCE("fall_damage_resistance"),

    STATUS_RESISTANCE("status_resistance"),
    STATUS_DURATION_RESISTANCE("status_duration_resistance"),
    EXPERIENCE_GAIN("experience_gain"),
    LOOT_BONUS("loot_bonus"),

    MAGIC_POWER("magic_power"),
    SPELL_DAMAGE("spell_damage"),
    MAGIC_PENETRATION("magic_penetration"),
    SPELL_CRITICAL_DAMAGE("spell_critical_damage"),
    CAST_SPEED("cast_speed"),
    SPELL_RANGE("spell_range"),
    SPELL_AREA("spell_area"),
    SPELL_PROJECTILE_SPEED("spell_projectile_speed"),
    SPELL_DURATION("spell_duration"),
    SPELL_STATUS_POWER("spell_status_power"),
    HEALING_POWER("healing_power"),
    BARRIER_POWER("barrier_power"),
    CONCENTRATION_STABILITY("concentration_stability"),

    MAX_MANA("max_mana"),
    MANA_REGENERATION("mana_regeneration"),
    MANA_REGEN_DELAY("mana_regen_delay"),
    MANA_COST("mana_cost"),

    MAGIC_RESISTANCE("magic_resistance"),
    ELEMENTAL_RESISTANCE("elemental_resistance");

    private final String id;

    PlayerModifierId(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
