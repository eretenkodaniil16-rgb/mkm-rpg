package com.eretenkodaniil.mkm.character.origin;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.character.CharacterData;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Add all existing equipment + class/race deltas exactly once BEFORE the existing final
 * PlayerModifierCalculator clamp. Does not apply direct Minecraft damage, mana, or stamina.
 */
public final class OriginModifierRules {
    private OriginModifierRules() {}

    static Map<PlayerModifierId, Double> checked(Map<PlayerModifierId, Double> deltas) {
        if (deltas == null || deltas.size() > 24) throw new IllegalArgumentException("Invalid deltas");
        for (var e : deltas.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || !Double.isFinite(e.getValue())
                    || Math.abs(e.getValue()) > 8.0D) {
                throw new IllegalArgumentException("Unbounded origin modifier");
            }
        }
        return Map.copyOf(deltas);
    }

    public static Map<PlayerModifierId, Double> combined(
            Map<PlayerModifierId, Double> equipment, CharacterOriginData origin) {
        Objects.requireNonNull(equipment, "equipment");
        EnumMap<PlayerModifierId, Double> all = new EnumMap<>(PlayerModifierId.class);
        add(all, equipment);
        if (origin != null && origin.schemaVersion() == CharacterOriginData.SCHEMA_VERSION
                && origin.selected()) {
            RaceDefinition race = OriginCatalog.race(origin.raceId());
            ClassDefinition clazz = OriginCatalog.clazz(origin.classId());
            if (race != null && clazz != null) {
                add(all, race.modifierDeltas());
                add(all, clazz.modifierDeltas());
            }
        }
        return Map.copyOf(all);
    }

    public static PlayerModifierSnapshot snapshot(CharacterData character,
            Map<PlayerModifierId, Double> equipment, CharacterOriginData origin) {
        return PlayerModifierCalculator.calculate(
                Objects.requireNonNull(character, "character"), combined(equipment, origin));
    }

    private static void add(EnumMap<PlayerModifierId, Double> out, Map<PlayerModifierId, Double> delta) {
        for (var e : delta.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || !Double.isFinite(e.getValue()))
                throw new IllegalArgumentException("Invalid modifier delta");
            double sum = out.getOrDefault(e.getKey(), 0.0D) + e.getValue();
            if (!Double.isFinite(sum)) throw new IllegalArgumentException("Modifier overflow");
            out.put(e.getKey(), sum);
        }
    }
}
