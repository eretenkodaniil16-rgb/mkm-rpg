package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.equipment.PhysicalDamageType;

/**
 * Extra MKM resistance applied before vanilla armor/shields, exactly once.
 * ARMOR_PENETRATION currently bypasses MKM resistance only; it does not rewrite
 * Minecraft's armor/toughness formula. Do not apply this to spells or projectiles.
 */
public final class PhysicalMitigationRules {
    private PhysicalMitigationRules() {}

    public static float apply(
            float amount, PlayerModifierSnapshot attacker,
            PlayerModifierSnapshot defender, PhysicalDamageType damageType) {
        if (!Float.isFinite(amount) || amount <= 0.0F) return 0.0F;
        PlayerModifierId typeId = switch (damageType) {
            case SLASHING -> PlayerModifierId.SLASHING_RESISTANCE;
            case PIERCING -> PlayerModifierId.PIERCING_RESISTANCE;
            case BLUNT -> PlayerModifierId.BLUNT_RESISTANCE;
        };
        double resistance = Math.max(-0.5D, Math.min(0.8D,
                defender.get(PlayerModifierId.PHYSICAL_RESISTANCE) + defender.get(typeId)));
        double penetration = Math.max(0.0D, Math.min(0.75D,
                attacker.get(PlayerModifierId.ARMOR_PENETRATION)));
        // Penetration removes protection, never turns a vulnerability into a bonus.
        if (resistance > 0.0D) resistance *= 1.0D - penetration;
        return (float) Math.min(1000000.0D, amount * (1.0D - resistance));
    }
}
