package com.eretenkodaniil.mkm.character.modifier;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class PlayerModifierSnapshot {
    private final Map<PlayerModifierId, Double> values;

    PlayerModifierSnapshot(Map<PlayerModifierId, Double> source) {
        EnumMap<PlayerModifierId, Double> copy = new EnumMap<>(PlayerModifierId.class);
        copy.putAll(source);

        for (PlayerModifierId id : PlayerModifierId.values()) {
            if (!copy.containsKey(id)) {
                throw new IllegalArgumentException("Snapshot missing modifier " + id);
            }
        }

        values = Collections.unmodifiableMap(copy);
    }

    public double get(PlayerModifierId id) {
        Double value = values.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Snapshot missing modifier " + id);
        }
        return value;
    }

    public Map<PlayerModifierId, Double> all() {
        return values;
    }

    public String formatted(PlayerModifierId id) {
        return ModifierDefinitions.get(id).format(get(id));
    }
}
