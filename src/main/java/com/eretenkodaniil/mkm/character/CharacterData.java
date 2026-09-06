package com.eretenkodaniil.mkm.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Persistent RPG state owned by one player.
 *
 * <p>The record is intentionally immutable. Replacing the attachment through setData makes
 * persistence and synchronization explicit and avoids mutable-state dirty tracking bugs.</p>
 */
public record CharacterData(
        int schemaVersion,
        long experience,
        int strength,
        int dexterity,
        int vitality) {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int DEFAULT_ATTRIBUTE = 10;
    public static final int MIN_ATTRIBUTE = 1;
    public static final int MAX_ATTRIBUTE = 100;

    public static final MapCodec<CharacterData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(CharacterData::schemaVersion),
            Codec.LONG.optionalFieldOf("experience", 0L).forGetter(CharacterData::experience),
            Codec.INT.optionalFieldOf("strength", DEFAULT_ATTRIBUTE).forGetter(CharacterData::strength),
            Codec.INT.optionalFieldOf("dexterity", DEFAULT_ATTRIBUTE).forGetter(CharacterData::dexterity),
            Codec.INT.optionalFieldOf("vitality", DEFAULT_ATTRIBUTE).forGetter(CharacterData::vitality)
    ).apply(instance, CharacterData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CharacterData decode(RegistryFriendlyByteBuf buffer) {
            return new CharacterData(
                    buffer.readVarInt(),
                    buffer.readVarLong(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, CharacterData value) {
            buffer.writeVarInt(value.schemaVersion());
            buffer.writeVarLong(value.experience());
            buffer.writeVarInt(value.strength());
            buffer.writeVarInt(value.dexterity());
            buffer.writeVarInt(value.vitality());
        }
    };

    public CharacterData {
        schemaVersion = Math.max(1, schemaVersion);
        experience = Math.max(0L, experience);
        strength = clampAttribute(strength);
        dexterity = clampAttribute(dexterity);
        vitality = clampAttribute(vitality);
    }

    public CharacterData() {
        this(CURRENT_SCHEMA_VERSION, 0L, DEFAULT_ATTRIBUTE, DEFAULT_ATTRIBUTE, DEFAULT_ATTRIBUTE);
    }

    /**
     * Level is derived rather than persisted so it cannot drift out of sync with total experience.
     */
    public int level() {
        return ProgressionRules.levelForExperience(experience);
    }

    public CharacterData withExperience(long newExperience) {
        return new CharacterData(CURRENT_SCHEMA_VERSION, newExperience, strength, dexterity, vitality);
    }

    private static int clampAttribute(int value) {
        return Math.max(MIN_ATTRIBUTE, Math.min(MAX_ATTRIBUTE, value));
    }
}
