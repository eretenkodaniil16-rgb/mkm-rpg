package com.eretenkodaniil.mkm.character.modifier;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttributeInfluence.InfluenceTier;

public record ModifierDefinition(
        PlayerModifierId id,
        ModifierUnit unit,
        double baseValue,
        double minValue,
        double maxValue,
        double primaryPerEffectivePoint,
        double secondaryPerEffectivePoint) {

    public ModifierDefinition {
        if (id == null || unit == null) {
            throw new IllegalArgumentException("Modifier id and unit are required");
        }
        requireFinite("baseValue", baseValue);
        requireFinite("minValue", minValue);
        requireFinite("maxValue", maxValue);
        requireFinite("primaryPerEffectivePoint", primaryPerEffectivePoint);
        requireFinite("secondaryPerEffectivePoint", secondaryPerEffectivePoint);
        if (minValue > maxValue) {
            throw new IllegalArgumentException(id + ": minValue must be <= maxValue");
        }
        if (baseValue < minValue || baseValue > maxValue) {
            throw new IllegalArgumentException(id + ": baseValue must be inside safety bounds");
        }
    }

    public double coefficient(InfluenceTier tier) {
        return switch (tier) {
            case PRIMARY -> primaryPerEffectivePoint;
            case SECONDARY -> secondaryPerEffectivePoint;
        };
    }

    public double clamp(double value) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    public String format(double value) {
        return unit.format(value);
    }

    private static void requireFinite(String field, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
    }
}
