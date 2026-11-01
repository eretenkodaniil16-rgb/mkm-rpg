package com.eretenkodaniil.mkm.equipment;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Pure rules for the first vanilla-item equipment vertical slice.
 * Minecraft already supplies base weapon damage, armor, durability and enchantments;
 * the values below are SMALL additional RPG modifier deltas, never replacements.
 */
public final class EquipmentRules {
    public enum Slot { MAIN_HAND, OFF_HAND, HEAD, CHEST, LEGS, FEET }
    public enum Family { NONE, SWORD, AXE, SPEAR, MACE, BOW, CROSSBOW, SHIELD, ARMOR }
    /** Material-grade approximation, not a stored item-rarity/upgrade component. */
    public enum Quality { STANDARD, REFINED, MASTERWORK }

    public record Profile(
            Family family, PhysicalDamageType damageType, Quality quality,
            Map<PlayerModifierId, Double> deltas) {
        public Profile {
            if (family == null || damageType == null || quality == null || deltas == null) {
                throw new IllegalArgumentException("Equipment profile fields are required");
            }
            deltas = Map.copyOf(deltas);
        }
    }

    private static final Profile EMPTY = new Profile(
            Family.NONE, PhysicalDamageType.BLUNT, Quality.STANDARD, Map.of());

    private EquipmentRules() {}

    public static Profile profile(String id, Slot slot) {
        if (id == null || slot == null || !id.startsWith("minecraft:")) {
            return EMPTY;
        }
        String name = id.substring("minecraft:".length());
        if (slot == Slot.OFF_HAND || slot == Slot.MAIN_HAND) {
            if (name.equals("shield")) {
                return new Profile(Family.SHIELD, PhysicalDamageType.BLUNT,
                        Quality.STANDARD, Map.of(PlayerModifierId.BLOCK_STABILITY, 0.25D));
            }
            if (slot == Slot.OFF_HAND) return EMPTY;
            if (name.equals("mace")) {
                return new Profile(Family.MACE, PhysicalDamageType.BLUNT,
                        Quality.REFINED, Map.of(PlayerModifierId.STAGGER_POWER, 0.22D));
            }
            if (name.equals("trident")) {
                return new Profile(Family.SPEAR, PhysicalDamageType.PIERCING,
                        Quality.REFINED, Map.of(PlayerModifierId.ARMOR_PENETRATION, 0.04D));
            }
            if (name.equals("bow") || name.equals("crossbow")) {
                return new Profile(name.equals("bow") ? Family.BOW : Family.CROSSBOW,
                        PhysicalDamageType.PIERCING, Quality.STANDARD,
                        Map.of(PlayerModifierId.RANGED_DAMAGE, 0.04D));
            }
            for (String suffix : List.of("_sword", "_axe", "_spear")) {
                if (!name.endsWith(suffix)) continue;
                String material = name.substring(0, name.length() - suffix.length());
                if (!weaponMaterial(material)) return EMPTY;
                Quality quality = quality(material);
                double grade = quality == Quality.MASTERWORK ? 1.5D
                        : quality == Quality.REFINED ? 1.25D : 1.0D;
                return switch (suffix) {
                    case "_sword" -> new Profile(Family.SWORD, PhysicalDamageType.SLASHING,
                            quality, Map.of(
                                    PlayerModifierId.MELEE_DAMAGE, 0.04D * grade,
                                    PlayerModifierId.ARMOR_PENETRATION, 0.01D * grade));
                    case "_axe" -> new Profile(Family.AXE, PhysicalDamageType.SLASHING,
                            quality, Map.of(PlayerModifierId.STAGGER_POWER, 0.10D * grade));
                    default -> new Profile(Family.SPEAR, PhysicalDamageType.PIERCING,
                            quality, Map.of(PlayerModifierId.ARMOR_PENETRATION, 0.04D * grade));
                };
            }
            return EMPTY;
        }

        String suffix = switch (slot) {
            case HEAD -> "_helmet";
            case CHEST -> "_chestplate";
            case LEGS -> "_leggings";
            case FEET -> "_boots";
            default -> throw new IllegalStateException("Unexpected slot: " + slot);
        };
        if (!name.endsWith(suffix)) return EMPTY;
        String material = name.substring(0, name.length() - suffix.length());
        double resistance = switch (material) {
            case "chainmail" -> 0.002D;
            case "copper" -> 0.002D;
            case "iron" -> 0.004D;
            case "diamond" -> 0.006D;
            case "netherite" -> 0.008D;
            case "leather", "golden", "turtle" -> 0.0D;
            default -> -1.0D;
        };
        if (resistance < 0.0D || (material.equals("turtle") && slot != Slot.HEAD)) {
            return EMPTY;
        }
        if (material.equals("netherite")) {
            return new Profile(Family.ARMOR, PhysicalDamageType.BLUNT, Quality.MASTERWORK,
                    Map.of(PlayerModifierId.PHYSICAL_RESISTANCE, resistance,
                            PlayerModifierId.STAGGER_RESISTANCE, 0.03D));
        }
        return new Profile(Family.ARMOR, PhysicalDamageType.BLUNT, quality(material),
                resistance == 0.0D ? Map.of()
                        : Map.of(PlayerModifierId.PHYSICAL_RESISTANCE, resistance));
    }

    /** Slot-aware and deterministic; no inventory items count unless actually equipped. */
    public static Map<PlayerModifierId, Double> aggregate(Map<Slot, String> equipped) {
        EnumMap<PlayerModifierId, Double> totals = new EnumMap<>(PlayerModifierId.class);
        for (Slot slot : Slot.values()) {
            Profile profile = profile(equipped.get(slot), slot);
            profile.deltas().forEach((id, value) -> totals.merge(id, value, Double::sum));
        }
        // Holding two shields cannot double the effective block stability.
        if (profile(equipped.get(Slot.MAIN_HAND), Slot.MAIN_HAND).family() == Family.SHIELD
                && profile(equipped.get(Slot.OFF_HAND), Slot.OFF_HAND).family() == Family.SHIELD) {
            totals.put(PlayerModifierId.BLOCK_STABILITY, 0.25D);
        }
        return Map.copyOf(totals);
    }

    private static boolean weaponMaterial(String material) {
        return switch (material) {
            case "wooden", "stone", "copper", "iron", "golden", "diamond", "netherite" -> true;
            default -> false;
        };
    }

    private static Quality quality(String material) {
        return switch (material) {
            case "netherite" -> Quality.MASTERWORK;
            case "diamond" -> Quality.REFINED;
            default -> Quality.STANDARD;
        };
    }
}
