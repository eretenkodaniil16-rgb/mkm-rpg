package com.eretenkodaniil.mkm.combat.fencing;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** One composition before OriginModifierRules and the ONE PlayerModifierCalculator clamp. */
public final class FencingSchoolModifierRules {
    private FencingSchoolModifierRules() {}
    public static Map<PlayerModifierId, Double> combine(
            Map<PlayerModifierId, Double> equipment,
            Map<PlayerModifierId, Double> serverSchoolDeltas) {
        Objects.requireNonNull(equipment);
        Objects.requireNonNull(serverSchoolDeltas);
        EnumMap<PlayerModifierId, Double> merged = new EnumMap<>(PlayerModifierId.class);
        merged.putAll(equipment);
        for (var e : serverSchoolDeltas.entrySet()) {
            double delta = Objects.requireNonNull(e.getValue());
            if (!Double.isFinite(delta) || Math.abs(delta) > 1.0D) {
                throw new IllegalArgumentException("Invalid fencing modifier");
            }
            double next = merged.getOrDefault(e.getKey(), 0.0D) + delta;
            if (!Double.isFinite(next)) throw new IllegalArgumentException("Modifier overflow");
            merged.put(e.getKey(), next);
        }
        return Map.copyOf(merged);
    }
}
