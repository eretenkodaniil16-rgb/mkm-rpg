package com.eretenkodaniil.mkm.character.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Separate saved and owner-synced v1 profile; CharacterData v4 stays untouched.
 * No selected race/class is represented by BOTH IDs empty (legacy and fresh players).
 */
public record CharacterOriginData(int schemaVersion, int revision, String raceId, String classId) {
    public static final int SCHEMA_VERSION = 1;

    public static final Codec<CharacterOriginData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("schema_version", SCHEMA_VERSION)
                            .forGetter(CharacterOriginData::schemaVersion),
                    Codec.INT.optionalFieldOf("revision", 0).forGetter(CharacterOriginData::revision),
                    Codec.STRING.optionalFieldOf("race_id", "").forGetter(CharacterOriginData::raceId),
                    Codec.STRING.optionalFieldOf("class_id", "").forGetter(CharacterOriginData::classId)
            ).apply(instance, CharacterOriginData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterOriginData> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public CharacterOriginData decode(RegistryFriendlyByteBuf buf) {
                    return new CharacterOriginData(
                            buf.readVarInt(), buf.readVarInt(), buf.readUtf(OriginIds.MAX_ID_LENGTH),
                            buf.readUtf(OriginIds.MAX_ID_LENGTH));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, CharacterOriginData state) {
                    buf.writeVarInt(state.schemaVersion);
                    buf.writeVarInt(state.revision);
                    buf.writeUtf(state.raceId, OriginIds.MAX_ID_LENGTH);
                    buf.writeUtf(state.classId, OriginIds.MAX_ID_LENGTH);
                }
            };

    public CharacterOriginData {
        if (schemaVersion < 1 || revision < 0 || revision > 1
                || raceId == null || classId == null
                || (raceId.isEmpty() != classId.isEmpty())) {
            throw new IllegalArgumentException("Invalid origin state");
        }
        if (!raceId.isEmpty() && (!OriginIds.valid(raceId) || !OriginIds.valid(classId)))
            throw new IllegalArgumentException("Invalid stored origin IDs");
        if (raceId.isEmpty() && revision != 0)
            throw new IllegalArgumentException("Unselected origin cannot have revision");
        if (!raceId.isEmpty() && revision != 1)
            throw new IllegalArgumentException("Selected origin requires revision one");
        // Unknown future schema and catalog IDs are preserved but cannot be mutated.
    }

    public CharacterOriginData() {
        this(SCHEMA_VERSION, 0, "", "");
    }

    public boolean selected() {
        return !raceId.isEmpty();
    }

    public CharacterOriginData withChoice(String race, String clazz) {
        if (schemaVersion != SCHEMA_VERSION || selected())
            throw new IllegalStateException("Origin is locked or unsupported");
        return new CharacterOriginData(SCHEMA_VERSION, 1, race, clazz);
    }
}
