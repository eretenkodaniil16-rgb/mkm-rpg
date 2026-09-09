package com.eretenkodaniil.mkm.character;

import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
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
        int vitality,
        int endurance,
        int intelligence,
        int willpower,
        int perception) {

    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final int DEFAULT_ATTRIBUTE = 10;
    public static final int MIN_ATTRIBUTE = 1;
    public static final int MAX_ATTRIBUTE = 100;

    /**
     * Schema v2 adds END/INT/WIL/PER. All attribute fields are optional in persistent decoding so
     * schema-v1 saves automatically receive the neutral default for the four new attributes.
     */
    public static final MapCodec<CharacterData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(CharacterData::schemaVersion),
            Codec.LONG.optionalFieldOf("experience", 0L).forGetter(CharacterData::experience),
            Codec.INT.optionalFieldOf("strength", DEFAULT_ATTRIBUTE).forGetter(CharacterData::strength),
            Codec.INT.optionalFieldOf("dexterity", DEFAULT_ATTRIBUTE).forGetter(CharacterData::dexterity),
            Codec.INT.optionalFieldOf("vitality", DEFAULT_ATTRIBUTE).forGetter(CharacterData::vitality),
            Codec.INT.optionalFieldOf("endurance", DEFAULT_ATTRIBUTE).forGetter(CharacterData::endurance),
            Codec.INT.optionalFieldOf("intelligence", DEFAULT_ATTRIBUTE).forGetter(CharacterData::intelligence),
            Codec.INT.optionalFieldOf("willpower", DEFAULT_ATTRIBUTE).forGetter(CharacterData::willpower),
            Codec.INT.optionalFieldOf("perception", DEFAULT_ATTRIBUTE).forGetter(CharacterData::perception)
    ).apply(instance, CharacterData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CharacterData decode(RegistryFriendlyByteBuf buffer) {
            return new CharacterData(
                    buffer.readVarInt(),
                    buffer.readVarLong(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
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
            buffer.writeVarInt(value.endurance());
            buffer.writeVarInt(value.intelligence());
            buffer.writeVarInt(value.willpower());
            buffer.writeVarInt(value.perception());
        }
    };

    public CharacterData {
        // Decoding a schema-v1 value materializes the four new v2 defaults and marks the in-memory
        // value as v2. A future higher schema marker is not silently downgraded.
        schemaVersion = Math.max(CURRENT_SCHEMA_VERSION, schemaVersion);
        experience = Math.max(0L, experience);
        strength = clampAttribute(strength);
        dexterity = clampAttribute(dexterity);
        vitality = clampAttribute(vitality);
        endurance = clampAttribute(endurance);
        intelligence = clampAttribute(intelligence);
        willpower = clampAttribute(willpower);
        perception = clampAttribute(perception);
    }

    public CharacterData() {
        this(
                CURRENT_SCHEMA_VERSION,
                0L,
                DEFAULT_ATTRIBUTE,
                DEFAULT_ATTRIBUTE,
                DEFAULT_ATTRIBUTE,
                DEFAULT_ATTRIBUTE,
                DEFAULT_ATTRIBUTE,
                DEFAULT_ATTRIBUTE,
                DEFAULT_ATTRIBUTE);
    }

    /**
     * Level is derived rather than persisted so it cannot drift out of sync with total experience.
     */
    public int level() {
        return ProgressionRules.levelForExperience(experience);
    }

    public int attribute(PrimaryAttribute attribute) {
        return switch (attribute) {
            case STRENGTH -> strength;
            case DEXTERITY -> dexterity;
            case VITALITY -> vitality;
            case ENDURANCE -> endurance;
            case INTELLIGENCE -> intelligence;
            case WILLPOWER -> willpower;
            case PERCEPTION -> perception;
        };
    }

    public CharacterData withExperience(long newExperience) {
        return new CharacterData(
                CURRENT_SCHEMA_VERSION,
                newExperience,
                strength,
                dexterity,
                vitality,
                endurance,
                intelligence,
                willpower,
                perception);
    }

    public CharacterData withAttribute(PrimaryAttribute attribute, int newValue) {
        return new CharacterData(
                CURRENT_SCHEMA_VERSION,
                experience,
                attribute == PrimaryAttribute.STRENGTH ? newValue : strength,
                attribute == PrimaryAttribute.DEXTERITY ? newValue : dexterity,
                attribute == PrimaryAttribute.VITALITY ? newValue : vitality,
                attribute == PrimaryAttribute.ENDURANCE ? newValue : endurance,
                attribute == PrimaryAttribute.INTELLIGENCE ? newValue : intelligence,
                attribute == PrimaryAttribute.WILLPOWER ? newValue : willpower,
                attribute == PrimaryAttribute.PERCEPTION ? newValue : perception);
    }

    private static int clampAttribute(int value) {
        return Math.max(MIN_ATTRIBUTE, Math.min(MAX_ATTRIBUTE, value));
    }
}
