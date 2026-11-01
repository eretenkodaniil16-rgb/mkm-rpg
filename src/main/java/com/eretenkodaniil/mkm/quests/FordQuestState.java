package com.eretenkodaniil.mkm.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One player-owned quest. V3 adds stable encounter/warden identifiers and a recoverable XP floor.
 * The floor is saved WITH the completion stage; Character Core remains the sole XP owner.
 * Future schemas, unknown stages and invalid identifiers fail closed at the service boundary.
 */
public record FordQuestState(int schemaVersion, String stage, boolean foundTracks, int recoveryCount,
                             String encounterId, String wardenId, long rewardXpFloor) {
    public static final int SCHEMA_VERSION = 3;
    public static final String NOT_STARTED = "not_started";
    public static final String ACTIVE = "active";
    public static final String RETURN_TO_WARDEN = "return_to_warden";
    public static final String REWARDED = "rewarded";

    public static final Codec<FordQuestState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", 1).forGetter(FordQuestState::schemaVersion),
            Codec.STRING.optionalFieldOf("stage", NOT_STARTED).forGetter(FordQuestState::stage),
            Codec.BOOL.optionalFieldOf("found_tracks", false).forGetter(FordQuestState::foundTracks),
            Codec.INT.optionalFieldOf("recovery_count", 0).forGetter(FordQuestState::recoveryCount),
            Codec.STRING.optionalFieldOf("encounter_id", "").forGetter(FordQuestState::encounterId),
            Codec.STRING.optionalFieldOf("warden_id", "").forGetter(FordQuestState::wardenId),
            Codec.LONG.optionalFieldOf("reward_xp_floor", -1L).forGetter(FordQuestState::rewardXpFloor)
    ).apply(instance, FordQuestState::new));

    public FordQuestState {
        if (schemaVersion == 1 || schemaVersion == 2) schemaVersion = SCHEMA_VERSION;
        stage = stage == null ? "invalid" : stage;
        encounterId = encounterId == null ? "" : encounterId;
        wardenId = wardenId == null ? "" : wardenId;
        recoveryCount = recoveryCount < 0 ? Integer.MAX_VALUE : recoveryCount;
        rewardXpFloor = Math.max(-1L, rewardXpFloor);
    }

    public FordQuestState() {
        this(SCHEMA_VERSION, NOT_STARTED, false, 0, "", "", -1L);
    }

    /** Compatibility constructor for unit tests and older creator code. */
    public FordQuestState(int schemaVersion, String stage, boolean tracks, int recoveries) {
        this(schemaVersion, stage, tracks, recoveries, "", "", -1L);
    }

    public FordQuestState withStage(String newStage) {
        return new FordQuestState(schemaVersion, newStage, foundTracks, recoveryCount,
                encounterId, wardenId, rewardXpFloor);
    }

    public FordQuestState withTracks() {
        return new FordQuestState(schemaVersion, stage, true, recoveryCount,
                encounterId, wardenId, rewardXpFloor);
    }

    public FordQuestState withEncounter(String entityUuid, String actorUuid) {
        return new FordQuestState(schemaVersion, stage, foundTracks, recoveryCount,
                entityUuid, actorUuid, rewardXpFloor);
    }

    public FordQuestState withRecoveredRaider() {
        return new FordQuestState(schemaVersion, stage, foundTracks, recoveryCount + 1,
                encounterId, wardenId, rewardXpFloor);
    }

    public FordQuestState withRewardFloor(long floor) {
        return new FordQuestState(schemaVersion, REWARDED, foundTracks, recoveryCount,
                encounterId, wardenId, floor);
    }
}
