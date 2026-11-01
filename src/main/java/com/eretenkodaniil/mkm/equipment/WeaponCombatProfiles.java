package com.eretenkodaniil.mkm.equipment;

import java.util.Objects;

/**
 * Provisional animation/input design metadata, NOT Minecraft attack attributes.
 * No stamina cost, reach, hit timing or damage change is applied from this class.
 */
public final class WeaponCombatProfiles {
    public enum Archetype { UNARMED, SWORD, AXE, MACE, SPEAR, OTHER }
    public record Profile(
            Archetype archetype,
            EquipmentRules.Family family,
            double presentationSpeed,
            double proposedStaminaCostFactor,
            int maxSequenceSteps,
            double proposedPoiseEmphasis,
            String lightAnimationId,
            String alternateAnimationId,
            String finisherAnimationId,
            String specialAnimationId) {
        public Profile {
            Objects.requireNonNull(archetype, "archetype");
            Objects.requireNonNull(family, "family");
            if (!Double.isFinite(presentationSpeed) || presentationSpeed <= 0D
                    || !Double.isFinite(proposedStaminaCostFactor) || proposedStaminaCostFactor <= 0D
                    || !Double.isFinite(proposedPoiseEmphasis) || proposedPoiseEmphasis < 0D
                    || maxSequenceSteps < 1 || maxSequenceSteps > 4) {
                throw new IllegalArgumentException("Invalid weapon archetype profile");
            }
            for (String id : new String[]{lightAnimationId, alternateAnimationId,
                    finisherAnimationId, specialAnimationId}) {
                if (id == null || !id.startsWith("mkm:")) {
                    throw new IllegalArgumentException("Animation IDs must be namespaced");
                }
            }
        }
    }

    private WeaponCombatProfiles() {}

    public static Profile forItem(String itemId) {
        if (itemId == null || itemId.equals("minecraft:air")) {
            return make(Archetype.UNARMED, EquipmentRules.Family.NONE, 1.25D, .70D, 4, 0D, "unarmed");
        }
        EquipmentRules.Family family =
                EquipmentRules.profile(itemId, EquipmentRules.Slot.MAIN_HAND).family();
        return switch (family) {
            case SWORD -> make(Archetype.SWORD, family, 1.0D, 1.0D, 3, .05D, "sword");
            case AXE -> make(Archetype.AXE, family, .72D, 1.35D, 2, .25D, "axe");
            case MACE -> make(Archetype.MACE, family, .65D, 1.45D, 2, .40D, "mace");
            case SPEAR -> make(Archetype.SPEAR, family, .90D, 1.10D, 3, .12D, "spear");
            default -> make(Archetype.OTHER, family, 1.0D, 1.0D, 1, 0D, "generic");
        };
    }

    private static Profile make(Archetype archetype, EquipmentRules.Family family,
                                double speed, double cost, int stages, double poise, String key) {
        String prefix = "mkm:attack/" + key + "/";
        return new Profile(archetype, family, speed, cost, stages, poise,
                prefix + "light", prefix + "alternate", prefix + "finisher",
                "mkm:special/" + key);
    }

    public static String animationFor(Profile profile, int step, boolean alternate, boolean finisher) {
        if (finisher) return profile.finisherAnimationId();
        if (alternate) return profile.alternateAnimationId();
        if (step <= 1) return profile.lightAnimationId();
        // MNAE resolves stage variants from the stable base path; no forced attack.
        return profile.lightAnimationId() + "_" + Math.min(step, profile.maxSequenceSteps());
    }
}
