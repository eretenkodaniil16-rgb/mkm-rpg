package com.eretenkodaniil.mkm.equipment;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.combat.PhysicalMitigationRules;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable launch-time values for future SERVER-OWNED vanilla projectile integration.
 * This is a pure model: no projectile hook is installed and no ranged damage is changed.
 */
public record RangedShotSnapshot(
        String weaponId,
        PhysicalDamageType damageType,
        double physicalPower,
        double rangedDamage,
        double armorPenetration,
        long firedGameTick) {

    public RangedShotSnapshot {
        Objects.requireNonNull(weaponId, "weaponId");
        Objects.requireNonNull(damageType, "damageType");
        if (weaponId.isBlank() || firedGameTick < 0L
                || !Double.isFinite(physicalPower) || physicalPower < 0.0D || physicalPower > 3.0D
                || !Double.isFinite(rangedDamage) || rangedDamage < 0.0D || rangedDamage > 2.5D
                || !Double.isFinite(armorPenetration)
                || armorPenetration < 0.0D || armorPenetration > 0.75D) {
            throw new IllegalArgumentException("Invalid projectile launch snapshot");
        }
    }

    /**
     * Call only when a vanilla bow/crossbow projectile is ACTUALLY spawned
     * or a thrown trident is committed, not while merely holding or drawing it.
     * Other items and modded weapons are left neutral until allowlisted.
     */
    public static Optional<RangedShotSnapshot> capture(
            String weaponId, PlayerModifierSnapshot attacker, long now) {
        Objects.requireNonNull(attacker, "attacker");
        EquipmentRules.Profile profile =
                EquipmentRules.profile(weaponId, EquipmentRules.Slot.MAIN_HAND);
        boolean isRanged = profile.family() == EquipmentRules.Family.BOW
                || profile.family() == EquipmentRules.Family.CROSSBOW
                || "minecraft:trident".equals(weaponId);
        if (!isRanged) return Optional.empty();
        return Optional.of(new RangedShotSnapshot(
                weaponId, PhysicalDamageType.PIERCING,
                attacker.get(PlayerModifierId.PHYSICAL_POWER),
                attacker.get(PlayerModifierId.RANGED_DAMAGE),
                attacker.get(PlayerModifierId.ARMOR_PENETRATION), now));
    }

    /** Transform exactly once before native armor/shield/enchantment mitigation. */
    public float scaleVanillaProjectile(float vanillaAmount) {
        if (!Float.isFinite(vanillaAmount) || vanillaAmount <= 0.0F) return 0.0F;
        return (float) Math.min(1_000_000.0D,
                vanillaAmount * physicalPower * rangedDamage);
    }

    /**
     * Optional single-pass MKM player defense composition. Future integration
     * MUST replace D3's fallback projectile defense, not call BOTH.
     */
    public float scaleAndMitigate(float vanillaAmount, PlayerModifierSnapshot defender) {
        Objects.requireNonNull(defender, "defender");
        return PhysicalMitigationRules.apply(
                scaleVanillaProjectile(vanillaAmount),
                armorPenetration, defender, damageType);
    }
}
