package com.eretenkodaniil.mkm.combat.enemy;

import com.eretenkodaniil.mkm.combat.PhysicalMitigationRules;
import com.eretenkodaniil.mkm.equipment.PhysicalDamageType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * An optional, immutable SERVER-OWNED MKM combat overlay for a special Mob.
 * Native Minecraft armor, shields, damage attribution and AI still operate.
 *
 * Signed typed resistance: positive is protection, negative is vulnerability.
 * This is NOT a PlayerModifierSnapshot and grants no stamina/mana/class data.
 */
public record EnemyCombatProfile(
        AttackDelivery delivery,
        PhysicalDamageType naturalAttackType,
        double physicalResistance,
        Map<PhysicalDamageType, Double> typedResistance,
        double poiseResistance,
        double attackStaggerPower) {

    public enum AttackDelivery { DIRECT_MELEE, PROJECTILE, NONE }

    public static final EnemyCombatProfile NEUTRAL = new EnemyCombatProfile(
            AttackDelivery.DIRECT_MELEE, PhysicalDamageType.BLUNT,
            0.0D, Map.of(), 1.0D, 1.0D);

    public EnemyCombatProfile {
        Objects.requireNonNull(delivery, "delivery");
        Objects.requireNonNull(naturalAttackType, "naturalAttackType");
        Objects.requireNonNull(typedResistance, "typedResistance");
        if (!Double.isFinite(physicalResistance)
                || physicalResistance < -0.5D || physicalResistance > 0.8D
                || !Double.isFinite(poiseResistance)
                || poiseResistance < 0.25D || poiseResistance > 3.0D
                || !Double.isFinite(attackStaggerPower)
                || attackStaggerPower < 0.0D || attackStaggerPower > 3.0D) {
            throw new IllegalArgumentException("Enemy combat attribute outside safe range");
        }
        EnumMap<PhysicalDamageType, Double> safe = new EnumMap<>(PhysicalDamageType.class);
        for (var entry : typedResistance.entrySet()) {
            var type = Objects.requireNonNull(entry.getKey(), "damage type");
            var amount = Objects.requireNonNull(entry.getValue(), "type resistance");
            if (!Double.isFinite(amount) || amount < -0.5D || amount > 0.8D) {
                throw new IllegalArgumentException("Enemy type resistance outside safe range");
            }
            safe.put(type, amount);
        }
        typedResistance = Map.copyOf(safe);
    }

    public double resistanceFor(PhysicalDamageType type) {
        return typedResistance.getOrDefault(Objects.requireNonNull(type), 0.0D);
    }

    /**
     * Only additional MKM resistance before vanilla mitigation, never a
     * replacement for native mob armor, enchantment, shield or i-frame rules.
     */
    public float mitigateDirectPhysical(float incoming, double penetration,
                                        PhysicalDamageType physicalType) {
        return PhysicalMitigationRules.apply(incoming, penetration,
                physicalResistance, resistanceFor(physicalType));
    }
}
