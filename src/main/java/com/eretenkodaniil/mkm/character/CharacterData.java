package com.eretenkodaniil.mkm.character;

import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Immutable, owner-synchronized player attachment. Build credits are tracked separately from XP. */
public record CharacterData(
        int schemaVersion,
        long experience,
        int strength,
        int dexterity,
        int vitality,
        int endurance,
        int intelligence,
        int willpower,
        int perception,
        int earnedPoints,
        List<Integer> allocatedPoints,
        List<String> claimedRewardIds) {

    public static final int CURRENT_SCHEMA_VERSION = 4;
    public static final int ALLOCATION_SCHEMA_VERSION = 3;
    public static final int DEFAULT_ATTRIBUTE = 10;
    public static final int MIN_ATTRIBUTE = 1;
    public static final int MAX_ATTRIBUTE = 100;
    public static final int ATTRIBUTE_COUNT = PrimaryAttribute.values().length;
    private static final List<Integer> ZERO_ALLOCATIONS = List.of(0, 0, 0, 0, 0, 0, 0);

    /**
     * v1/v2 did not record spending. Their primary attributes remain untouched and become the
     * starting baseline; XP grants unspent level credits on migration. Never infer historic spending
     * from non-default stats (admins could edit them).
     *
     * The old v2 encoder omitted schema_version when it matched its default, so an absent marker
     * must be treated as pre-v3 rather than assumed current.
     */
    public static final MapCodec<CharacterData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", 1).forGetter(CharacterData::schemaVersion),
            Codec.LONG.optionalFieldOf("experience", 0L).forGetter(CharacterData::experience),
            Codec.INT.optionalFieldOf("strength", DEFAULT_ATTRIBUTE).forGetter(CharacterData::strength),
            Codec.INT.optionalFieldOf("dexterity", DEFAULT_ATTRIBUTE).forGetter(CharacterData::dexterity),
            Codec.INT.optionalFieldOf("vitality", DEFAULT_ATTRIBUTE).forGetter(CharacterData::vitality),
            Codec.INT.optionalFieldOf("endurance", DEFAULT_ATTRIBUTE).forGetter(CharacterData::endurance),
            Codec.INT.optionalFieldOf("intelligence", DEFAULT_ATTRIBUTE).forGetter(CharacterData::intelligence),
            Codec.INT.optionalFieldOf("willpower", DEFAULT_ATTRIBUTE).forGetter(CharacterData::willpower),
            Codec.INT.optionalFieldOf("perception", DEFAULT_ATTRIBUTE).forGetter(CharacterData::perception),
            Codec.INT.optionalFieldOf("earned_points", 0).forGetter(CharacterData::earnedPoints),
            Codec.INT.listOf().optionalFieldOf("allocated_points", List.of()).forGetter(CharacterData::allocatedPoints),
            Codec.STRING.listOf().optionalFieldOf("claimed_reward_ids", List.of()).forGetter(CharacterData::claimedRewardIds)
    ).apply(instance, CharacterData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CharacterData decode(RegistryFriendlyByteBuf buf) {
            int schema = buf.readVarInt();
            long xp = buf.readVarLong();
            int str = buf.readVarInt(), dex = buf.readVarInt(), vit = buf.readVarInt();
            int end = buf.readVarInt(), intelligence = buf.readVarInt();
            int wil = buf.readVarInt(), per = buf.readVarInt();
            int earned = buf.readVarInt();
            List<Integer> spent = new ArrayList<>(ATTRIBUTE_COUNT);
            for (int i = 0; i < ATTRIBUTE_COUNT; i++) spent.add(buf.readVarInt());
            // The claim ledger is deliberately SERVER-PRIVATE and never copied into owner sync.
            // Clients still receive full level, XP, allocation and derived-stat inputs.
            return new CharacterData(schema, xp, str, dex, vit, end, intelligence, wil, per,
                    earned, spent, List.of());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, CharacterData data) {
            buf.writeVarInt(data.schemaVersion);
            buf.writeVarLong(data.experience);
            buf.writeVarInt(data.strength);
            buf.writeVarInt(data.dexterity);
            buf.writeVarInt(data.vitality);
            buf.writeVarInt(data.endurance);
            buf.writeVarInt(data.intelligence);
            buf.writeVarInt(data.willpower);
            buf.writeVarInt(data.perception);
            buf.writeVarInt(data.earnedPoints);
            for (int allocation : data.allocatedPoints) buf.writeVarInt(allocation);
        }
    };

    public CharacterData {
        int sourceVersion = schemaVersion;
        schemaVersion = Math.max(CURRENT_SCHEMA_VERSION, schemaVersion);
        experience = Math.max(0L, experience);
        strength = clampAttribute(strength);
        dexterity = clampAttribute(dexterity);
        vitality = clampAttribute(vitality);
        endurance = clampAttribute(endurance);
        intelligence = clampAttribute(intelligence);
        willpower = clampAttribute(willpower);
        perception = clampAttribute(perception);
        if (sourceVersion < ALLOCATION_SCHEMA_VERSION) {
            allocatedPoints = ZERO_ALLOCATIONS;
            earnedPoints = ProgressionRules.earnedPointsForLevel(
                    ProgressionRules.levelForExperience(experience));
        } else {
            allocatedPoints = List.copyOf(allocatedPoints);
            if (allocatedPoints.size() != ATTRIBUTE_COUNT) {
                throw new IllegalArgumentException("Invalid allocation ledger size");
            }
            for (int value : allocatedPoints) {
                if (value < 0 || value > MAX_ATTRIBUTE) {
                    throw new IllegalArgumentException("Invalid allocation ledger entry");
                }
            }
            if (earnedPoints < 0 || earnedPoints > ProgressionRules.earnedPointsForLevel(
                    ProgressionRules.MAX_LEVEL)) {
                throw new IllegalArgumentException("Invalid earned-point ledger");
            }
        }
        if (sourceVersion < CURRENT_SCHEMA_VERSION) {
            // v1/v2/v3 had no keyed receipts. Do not fabricate them from XP or attributes.
            claimedRewardIds = List.of();
        } else {
            claimedRewardIds = List.copyOf(claimedRewardIds);
            if (claimedRewardIds.size() > CharacterRewardRules.MAX_CLAIMED_REWARDS
                    || new HashSet<>(claimedRewardIds).size() != claimedRewardIds.size()) {
                throw new IllegalArgumentException("Oversized or duplicate reward ledger");
            }
            for (String rewardId : claimedRewardIds) {
                if (!CharacterRewardRules.validRewardId(rewardId)) {
                    throw new IllegalArgumentException("Malformed saved reward ID");
                }
            }
        }
    }

    public CharacterData() {
        this(CURRENT_SCHEMA_VERSION, 0L, DEFAULT_ATTRIBUTE, DEFAULT_ATTRIBUTE,
                DEFAULT_ATTRIBUTE, DEFAULT_ATTRIBUTE, DEFAULT_ATTRIBUTE, DEFAULT_ATTRIBUTE,
                DEFAULT_ATTRIBUTE, 0, ZERO_ALLOCATIONS, List.of());
    }

    /**
     * Compatibility constructor for schema-v1/v2/v3 fixtures and callers that do not track
     * persistent reward receipts. Prior v3 allocation balances are preserved on v4 migration.
     */
    public CharacterData(int schemaVersion, long experience,
            int strength, int dexterity, int vitality, int endurance, int intelligence,
            int willpower, int perception, int earnedPoints, List<Integer> allocatedPoints) {
        this(schemaVersion, experience, strength, dexterity, vitality, endurance, intelligence,
                willpower, perception, earnedPoints, allocatedPoints, List.of());
    }

    public boolean hasClaimedReward(String id) {
        return claimedRewardIds.contains(id);
    }

    public int level() {
        return ProgressionRules.levelForExperience(experience);
    }

    public int spentPoints() {
        int spent = 0;
        for (int count : allocatedPoints) spent += count;
        return spent;
    }

    public int availablePoints() {
        return Math.max(0, earnedPoints - spentPoints());
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

    public CharacterData withExperience(long xp) {
        long safeXp = Math.max(0L, xp);
        int credits = Math.max(earnedPoints, ProgressionRules.earnedPointsForLevel(
                ProgressionRules.levelForExperience(safeXp)));
        return new CharacterData(schemaVersion, safeXp, strength, dexterity, vitality, endurance,
                intelligence, willpower, perception, credits, allocatedPoints, claimedRewardIds);
    }

    /** Administrative baseline adjustment only, never used to spend build credits. */
    public CharacterData withAttribute(PrimaryAttribute attribute, int newValue) {
        return new CharacterData(schemaVersion, experience,
                attribute == PrimaryAttribute.STRENGTH ? newValue : strength,
                attribute == PrimaryAttribute.DEXTERITY ? newValue : dexterity,
                attribute == PrimaryAttribute.VITALITY ? newValue : vitality,
                attribute == PrimaryAttribute.ENDURANCE ? newValue : endurance,
                attribute == PrimaryAttribute.INTELLIGENCE ? newValue : intelligence,
                attribute == PrimaryAttribute.WILLPOWER ? newValue : willpower,
                attribute == PrimaryAttribute.PERCEPTION ? newValue : perception,
                earnedPoints, allocatedPoints, claimedRewardIds);
    }

    /** Package-local: allocation rules are the sole runtime caller. */
    CharacterData withAllocatedPoints(List<Integer> newAllocations) {
        return new CharacterData(schemaVersion, experience, strength, dexterity, vitality,
                endurance, intelligence, willpower, perception, earnedPoints, newAllocations,
                claimedRewardIds);
    }

    /** Package-local atomic receipt replacement, called only by CharacterRewardRules. */
    CharacterData withClaimedRewardIds(List<String> rewardIds) {
        return new CharacterData(schemaVersion, experience, strength, dexterity, vitality,
                endurance, intelligence, willpower, perception, earnedPoints, allocatedPoints,
                rewardIds);
    }

    private static int clampAttribute(int value) {
        return Math.max(MIN_ATTRIBUTE, Math.min(MAX_ATTRIBUTE, value));
    }
}
