package com.eretenkodaniil.mkm.quests;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;

/**
 * World-scoped, persistent tombstones for authored raiders killed while quest owners are offline.
 * Key is UUID of the raider, value is UUID of its quest owner. No experience is granted here.
 */
public record FordEncounterOutcomes(Map<String, String> deadRaiders) {
    public static final int MAX_PENDING = 4096;
    public static final Codec<FordEncounterOutcomes> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .xmap(FordEncounterOutcomes::new, FordEncounterOutcomes::deadRaiders);

    public FordEncounterOutcomes {
        deadRaiders = Map.copyOf(deadRaiders);
    }

    public FordEncounterOutcomes() {
        this(Map.of());
    }

    public FordEncounterOutcomes recordDeath(String entityId, String ownerId) {
        if (deadRaiders.containsKey(entityId) || deadRaiders.size() >= MAX_PENDING) return this;
        Map<String, String> copy = new HashMap<>(deadRaiders);
        copy.put(entityId, ownerId);
        return new FordEncounterOutcomes(copy);
    }

    public FordEncounterOutcomes remove(String entityId) {
        if (!deadRaiders.containsKey(entityId)) return this;
        Map<String, String> copy = new HashMap<>(deadRaiders);
        copy.remove(entityId);
        return new FordEncounterOutcomes(copy);
    }
}
