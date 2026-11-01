package com.eretenkodaniil.mkm.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Server-level persistent encounter reservation. Each player owns one authored
 * Ford encounter, even while the raider's chunk is unloaded. A missing loaded
 * entity is NOT proof of destruction and never authorizes another spawn.
 */
public record FordEncounterRegistry(Map<String, Entry> byOwner) {
    public static final int MAX_ACTIVE = 4096;
    public record Entry(String raiderUuid, String wardenUuid, int x, int y, int z) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("raider_uuid").forGetter(Entry::raiderUuid),
                Codec.STRING.fieldOf("warden_uuid").forGetter(Entry::wardenUuid),
                Codec.INT.fieldOf("x").forGetter(Entry::x),
                Codec.INT.fieldOf("y").forGetter(Entry::y),
                Codec.INT.fieldOf("z").forGetter(Entry::z)
        ).apply(i, Entry::new));
        public Entry {
            if (raiderUuid == null || raiderUuid.isBlank()
                    || wardenUuid == null || wardenUuid.isBlank()) {
                throw new IllegalArgumentException("Invalid encounter reservation");
            }
        }
    }
    public static final Codec<FordEncounterRegistry> CODEC =
            Codec.unboundedMap(Codec.STRING, Entry.CODEC)
                    .xmap(FordEncounterRegistry::new, FordEncounterRegistry::byOwner);

    public FordEncounterRegistry {
        byOwner = Map.copyOf(byOwner);
        if (byOwner.size() > MAX_ACTIVE)
            throw new IllegalArgumentException("Encounter reservation limit exceeded");
    }
    public FordEncounterRegistry() { this(Map.of()); }

    public boolean canRegister(String playerId) {
        return !byOwner.containsKey(playerId) && byOwner.size() < MAX_ACTIVE;
    }

    /** Duplicate owner or exhausted registry is a strict no-op. */
    public FordEncounterRegistry register(String playerId, Entry entry) {
        if (!canRegister(playerId)) return this;
        Map<String, Entry> updated = new HashMap<>(byOwner);
        updated.put(playerId, entry);
        return new FordEncounterRegistry(updated);
    }

    /** A death may release only the EXACT enemy belonging to this player. */
    public FordEncounterRegistry removeIfMatch(String playerId, String raiderId) {
        Entry entry = byOwner.get(playerId);
        if (entry == null || !entry.raiderUuid().equals(raiderId)) return this;
        Map<String, Entry> updated = new HashMap<>(byOwner);
        updated.remove(playerId);
        return new FordEncounterRegistry(updated);
    }
}
