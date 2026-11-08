package com.eretenkodaniil.mkm.character.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Independent long-lived class/cultural learning ledger. Never rewrite CharacterData v4 or
 * the race/class v1 choice. Unknown future IDs survive serialization but grant no authority.
 */
public record OriginProgressData(int schemaVersion, List<String> unlockedNodes) {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_NODES = 24;
    public static final String FORD_MENTORSHIP = "mkm:ford_mentorship";
    public static final Codec<OriginProgressData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("schema_version", SCHEMA_VERSION).forGetter(OriginProgressData::schemaVersion),
            Codec.STRING.listOf().optionalFieldOf("unlocked_nodes", List.of()).forGetter(OriginProgressData::unlockedNodes)
    ).apply(i, OriginProgressData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, OriginProgressData> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public OriginProgressData decode(RegistryFriendlyByteBuf b) {
                    int version = b.readVarInt();
                    int count = b.readVarInt();
                    if (count < 0 || count > MAX_NODES) throw new IllegalArgumentException("Origin node count");
                    java.util.ArrayList<String> ids = new java.util.ArrayList<>(count);
                    for (int j = 0; j < count; j++) ids.add(b.readUtf(OriginIds.MAX_ID_LENGTH));
                    return new OriginProgressData(version, ids);
                }
                @Override public void encode(RegistryFriendlyByteBuf b, OriginProgressData data) {
                    b.writeVarInt(data.schemaVersion);
                    b.writeVarInt(data.unlockedNodes.size());
                    for (String id : data.unlockedNodes) b.writeUtf(id, OriginIds.MAX_ID_LENGTH);
                }
            };
    public OriginProgressData {
        if (schemaVersion < 1 || unlockedNodes == null || unlockedNodes.size() > MAX_NODES)
            throw new IllegalArgumentException("Invalid origin progression version/count");
        unlockedNodes = List.copyOf(unlockedNodes);
        if (new HashSet<>(unlockedNodes).size() != unlockedNodes.size())
            throw new IllegalArgumentException("Duplicate origin learning record");
        for (String id : unlockedNodes) OriginIds.require(id);
    }
    public OriginProgressData() { this(SCHEMA_VERSION, List.of()); }
    public boolean has(String id) {
        return schemaVersion == SCHEMA_VERSION && FORD_MENTORSHIP.equals(id)
                && unlockedNodes.contains(id);
    }
    public OriginProgressData unlock(String id) {
        if (schemaVersion != SCHEMA_VERSION || has(id) || unlockedNodes.size() >= MAX_NODES)
            throw new IllegalStateException("Progress ledger locked, duplicate or full");
        OriginIds.require(id);
        if (!FORD_MENTORSHIP.equals(id))
            throw new IllegalArgumentException("Learning node has no registered M5 handler");
        java.util.ArrayList<String> next = new java.util.ArrayList<>(unlockedNodes);
        next.add(id);
        return new OriginProgressData(schemaVersion, next);
    }
}
