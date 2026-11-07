package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Player-only source accounting. This class ONLY composes named additive
 * contributions. It NEVER calculates derived stats or creates a second
 * PlayerModifierSnapshot; PlayerModifierCalculator owns the single final clamp.
 *
 * Race/class/status features may supply server-verified maps in the future.
 * Equipment is ALWAYS read and aggregated exactly once from native slots.
 */
public final class CombatModifierContributions {
    public enum Kind { RACE, CLASS, STATUS }

    public record Contribution(Kind kind, String featureId,
                               Map<PlayerModifierId, Double> deltas) {
        public Contribution {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(featureId, "featureId");
            Objects.requireNonNull(deltas, "deltas");
            if (featureId.length() > 96
                    || !featureId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("Feature IDs must be bounded namespaced keys");
            }
            for (var entry : deltas.entrySet()) {
                Objects.requireNonNull(entry.getKey(), "modifier");
                if (entry.getValue() == null || !Double.isFinite(entry.getValue())) {
                    throw new IllegalArgumentException("Modifier contribution must be finite");
                }
            }
            deltas = Map.copyOf(deltas);
        }
    }

    private CombatModifierContributions() {}

    /** Equipment is one mandatory source; no Contribution can impersonate it. */
    public static Map<PlayerModifierId, Double> combine(
            Map<EquipmentRules.Slot, String> verifiedEquipment,
            List<Contribution> serverFeatures) {
        Objects.requireNonNull(verifiedEquipment, "verifiedEquipment");
        Objects.requireNonNull(serverFeatures, "serverFeatures");
        if (serverFeatures.size() > 64) {
            throw new IllegalArgumentException("Too many passive feature contributions");
        }
        EnumMap<PlayerModifierId, Double> combined = new EnumMap<>(PlayerModifierId.class);
        combined.putAll(EquipmentRules.aggregate(verifiedEquipment));
        Set<String> receipts = new HashSet<>();
        for (Contribution feature : serverFeatures) {
            Objects.requireNonNull(feature, "feature");
            if (!receipts.add(feature.kind() + ":" + feature.featureId())) {
                throw new IllegalArgumentException("Duplicate feature applied: " + feature.featureId());
            }
            for (var entry : feature.deltas().entrySet()) {
                double sum = combined.getOrDefault(entry.getKey(), 0.0D) + entry.getValue();
                if (!Double.isFinite(sum)) {
                    throw new IllegalArgumentException("Modifier aggregate overflow");
                }
                combined.put(entry.getKey(), sum);
            }
        }
        return Map.copyOf(combined);
    }
}
