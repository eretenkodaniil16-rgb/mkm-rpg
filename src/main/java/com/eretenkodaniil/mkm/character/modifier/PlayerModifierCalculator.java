package com.eretenkodaniil.mkm.character.modifier;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttributeInfluence;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttributeInfluence.InfluenceTier;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.ToIntFunction;

public final class PlayerModifierCalculator {
    private PlayerModifierCalculator() {
    }

    public static PlayerModifierSnapshot calculate(CharacterData data) {
        return calculate(data::attribute);
    }

    /** Adds already-resolved equipment/buff deltas before the single final safety clamp. */
    public static PlayerModifierSnapshot calculate(
            CharacterData data, Map<PlayerModifierId, Double> additiveDeltas) {
        return calculate(data::attribute, additiveDeltas);
    }

    static PlayerModifierSnapshot calculate(ToIntFunction<PrimaryAttribute> attributeValues) {
        return calculate(attributeValues, Map.of());
    }

    static PlayerModifierSnapshot calculate(
            ToIntFunction<PrimaryAttribute> attributeValues,
            Map<PlayerModifierId, Double> additiveDeltas) {
        EnumMap<PlayerModifierId, Double> values = new EnumMap<>(PlayerModifierId.class);

        for (PlayerModifierId id : PlayerModifierId.values()) {
            values.put(id, ModifierDefinitions.get(id).baseValue());
        }

        for (PrimaryAttribute attribute : PrimaryAttribute.values()) {
            double effectiveDelta = AttributeScaling.effectiveDelta(attributeValues.applyAsInt(attribute));
            if (effectiveDelta == 0.0D) {
                continue;
            }

            for (Map.Entry<PlayerModifierId, InfluenceTier> influence
                    : PrimaryAttributeInfluence.tieredModifiers(attribute).entrySet()) {
                PlayerModifierId id = influence.getKey();
                ModifierDefinition definition = ModifierDefinitions.get(id);
                double contribution = effectiveDelta * definition.coefficient(influence.getValue());
                values.compute(id, (ignored, current) -> current + contribution);
            }
        }

        for (Map.Entry<PlayerModifierId, Double> contribution : additiveDeltas.entrySet()) {
            PlayerModifierId id = contribution.getKey();
            Double delta = contribution.getValue();
            if (id == null || delta == null || !Double.isFinite(delta)) {
                throw new IllegalArgumentException("Modifier contributions must be finite and named");
            }
            double next = values.get(id) + delta;
            if (!Double.isFinite(next)) {
                throw new IllegalArgumentException("Modifier contribution sum overflow: " + id);
            }
            values.put(id, next);
        }

        for (PlayerModifierId id : PlayerModifierId.values()) {
            ModifierDefinition definition = ModifierDefinitions.get(id);
            values.compute(id, (ignored, value) -> definition.clamp(value));
        }

        return new PlayerModifierSnapshot(values);
    }
}
