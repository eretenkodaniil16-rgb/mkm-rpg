package com.eretenkodaniil.mkm.encounter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/** Author-controlled encounter data. No executable scripts or arbitrary entity classes. */
public record EncounterDefinition(
        String id, String title, String category, String dimension,
        List<String> allowedBiomes, String timeWindow, int dangerTier,
        int rewardXp, List<Spawn> roster) {
    public record Spawn(String entityId, String role, boolean elite) {
        public static final Codec<Spawn> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("entity").forGetter(Spawn::entityId),
                Codec.STRING.fieldOf("role").forGetter(Spawn::role),
                Codec.BOOL.fieldOf("elite").forGetter(Spawn::elite)
        ).apply(i, Spawn::new));
    }

    public static final Codec<EncounterDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(EncounterDefinition::id),
            Codec.STRING.fieldOf("title").forGetter(EncounterDefinition::title),
            Codec.STRING.fieldOf("category").forGetter(EncounterDefinition::category),
            Codec.STRING.fieldOf("dimension").forGetter(EncounterDefinition::dimension),
            Codec.STRING.listOf().fieldOf("allowed_biomes").forGetter(EncounterDefinition::allowedBiomes),
            Codec.STRING.fieldOf("time").forGetter(EncounterDefinition::timeWindow),
            Codec.INT.fieldOf("danger").forGetter(EncounterDefinition::dangerTier),
            Codec.INT.fieldOf("reward_xp").forGetter(EncounterDefinition::rewardXp),
            Spawn.CODEC.listOf().fieldOf("roster").forGetter(EncounterDefinition::roster)
    ).apply(i, EncounterDefinition::new));

    public EncounterDefinition {
        allowedBiomes = List.copyOf(allowedBiomes);
        roster = List.copyOf(roster);
        if (!"mkm:ford_ambush".equals(id) || title.isBlank()
                || !"hostile".equals(category) || !"minecraft:overworld".equals(dimension)
                || allowedBiomes.isEmpty() || allowedBiomes.size() > 16
                || !List.of("any", "day", "night").contains(timeWindow)
                || dangerTier < 1 || dangerTier > 5
                || rewardXp <= 0 || rewardXp > 1_000
                || roster.size() != 3
                || roster.stream().filter(Spawn::elite).count() != 1
                || roster.stream().filter(Spawn::elite).anyMatch(s ->
                    !s.entityId().equals("minecraft:skeleton") || !s.role().equals("leader"))
                || roster.stream().noneMatch(s -> s.entityId().equals("minecraft:husk"))
                || roster.stream().noneMatch(s -> s.entityId().equals("minecraft:skeleton"))
                || roster.stream().noneMatch(s -> s.entityId().equals("minecraft:spider"))
                || roster.stream().anyMatch(s -> !List.of("minecraft:husk", "minecraft:spider", "minecraft:skeleton").contains(s.entityId())
                    || !List.of("frontline", "ranged", "leader").contains(s.role()))
                || allowedBiomes.stream().anyMatch(s -> !s.startsWith("minecraft:") || s.length() > 80)) {
            throw new IllegalArgumentException("Invalid or oversized Ford ambush encounter configuration");
        }
    }

    public boolean accepts(String biome, long dayTime) {
        if (!allowedBiomes.contains(biome)) return false;
        long clock = Math.floorMod(dayTime, 24000L);
        return timeWindow.equals("any") || timeWindow.equals("night") && clock >= 13000L
                || timeWindow.equals("day") && clock < 13000L;
    }
}
