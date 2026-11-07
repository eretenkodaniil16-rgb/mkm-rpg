package com.eretenkodaniil.mkm.combat.enemy;

import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import com.eretenkodaniil.mkm.equipment.PhysicalDamageType;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;

/**
 * Fabric server adapter. Never infer an RPG race/class/stamina from a Mob.
 * Explicit registration remains empty by default: vanilla world combat is
 * unchanged until AI/World registers an approved server-side special type.
 */
public final class ServerEnemyCombatAdapter {
    private static final EnemyCombatFeatureRegistry FEATURES = new EnemyCombatFeatureRegistry();

    private ServerEnemyCombatAdapter() {}

    /** Register only during mod initialization; never during a damage callback. */
    public static EnemyCombatFeatureRegistry registration() {
        return FEATURES;
    }

    public static void sealAtServerStart() {
        FEATURES.seal();
    }

    public static Optional<EnemyCombatProfile> features(Mob mob) {
        if (mob == null || mob.level().isClientSide()) return Optional.empty();
        return FEATURES.find(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
    }

    /**
     * If the mob is swinging an equipped weapon, use the same canonical melee
     * classifier as a player; otherwise use an explicitly registered natural
     * attack type. Unregistered mobs keep their existing vanilla/MKM fallback.
     */
    public static PhysicalDamageType directMeleeType(Mob attacker) {
        String main = BuiltInRegistries.ITEM.getKey(attacker.getMainHandItem().getItem()).toString();
        return selectDirectMeleeType(main, features(attacker));
    }

    /**
     * Pure selection contract for AI/tests. A registered projectile-only
     * profile must not incorrectly override a REAL direct melee DamageSource.
     */
    public static PhysicalDamageType selectDirectMeleeType(
            String heldMainhandId, Optional<EnemyCombatProfile> registered) {
        java.util.Objects.requireNonNull(registered, "registered");
        EquipmentRules.Family family = EquipmentRules.presentationFamily(heldMainhandId);
        if (family == EquipmentRules.Family.SWORD
                || family == EquipmentRules.Family.AXE
                || family == EquipmentRules.Family.MACE
                || family == EquipmentRules.Family.SPEAR) {
            return EquipmentRules.meleeDamageType(heldMainhandId);
        }
        return registered.filter(profile -> profile.delivery()
                        == EnemyCombatProfile.AttackDelivery.DIRECT_MELEE)
                .map(EnemyCombatProfile::naturalAttackType)
                .orElse(EquipmentRules.meleeDamageType(heldMainhandId));
    }
}
