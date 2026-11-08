package com.eretenkodaniil.mkm.equipment;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginAttachments;
import com.eretenkodaniil.mkm.character.origin.OriginModifierRules;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import java.util.EnumMap;
import java.util.Map;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Thin platform adapter. CharacterData and ordinary vanilla equipment remain the only state. */
public final class EquipmentSnapshots {
    private EquipmentSnapshots() {}

    public static PlayerModifierSnapshot snapshot(Player player, CharacterData character) {
        return snapshot(player, character, Map.of());
    }

    /** Additional verified AbilityCore mana contribution BEFORE one Character final clamp. */
    public static PlayerModifierSnapshot snapshot(Player player, CharacterData character,
            Map<PlayerModifierId, Double> validatedExtra) {
        EnumMap<EquipmentRules.Slot, String> equipped = new EnumMap<>(EquipmentRules.Slot.class);
        equipped.put(EquipmentRules.Slot.MAIN_HAND, id(player.getMainHandItem()));
        equipped.put(EquipmentRules.Slot.OFF_HAND, id(player.getOffhandItem()));
        equipped.put(EquipmentRules.Slot.HEAD, id(player.getItemBySlot(EquipmentSlot.HEAD)));
        equipped.put(EquipmentRules.Slot.CHEST, id(player.getItemBySlot(EquipmentSlot.CHEST)));
        equipped.put(EquipmentRules.Slot.LEGS, id(player.getItemBySlot(EquipmentSlot.LEGS)));
        equipped.put(EquipmentRules.Slot.FEET, id(player.getItemBySlot(EquipmentSlot.FEET)));
        // Original equip + origin deltas share the Character calculator and one final clamp.
        EnumMap<PlayerModifierId, Double> deltas =
                new EnumMap<>(PlayerModifierId.class);
        deltas.putAll(EquipmentRules.aggregate(equipped));
        for (var entry : validatedExtra.entrySet()) {
            if (entry.getKey() != PlayerModifierId.MAX_MANA
                    || entry.getValue() == null || !Double.isFinite(entry.getValue())
                    || Math.abs(entry.getValue()) > 128.0D) {
                throw new IllegalArgumentException("Unapproved ability modifier delta");
            }
            deltas.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
        return OriginModifierRules.snapshot(character, deltas,
                player.getAttached(CharacterOriginAttachments.PROFILE));
    }

    public static PhysicalDamageType heldPhysicalType(LivingEntity player) {
        return EquipmentRules.profile(id(player.getMainHandItem()), EquipmentRules.Slot.MAIN_HAND).damageType();
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
