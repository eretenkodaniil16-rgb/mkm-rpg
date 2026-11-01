package com.eretenkodaniil.mkm.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Player-scoped pilot quest. Schema v2 adds a bounded encounter recovery counter.
 * v1 saves upgrade without resetting their stage or earned investigation bonus.
 * Unknown future schemas/stages are retained and fail closed in FordQuestRules.
 */
public record FordQuestState(int schemaVersion, String stage, boolean foundTracks, int recoveryCount) {
    public static final int SCHEMA_VERSION = 2;
    public static final String NOT_STARTED = "not_started";
    public static final String ACTIVE = "active";
    public static final String RETURN_TO_WARDEN = "return_to_warden";
    public static final String REWARDED = "rewarded";

    public static final Codec<FordQuestState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", SCHEMA_VERSION).forGetter(FordQuestState::schemaVersion),
            Codec.STRING.optionalFieldOf("stage", NOT_STARTED).forGetter(FordQuestState::stage),
            Codec.BOOL.optionalFieldOf("found_tracks", false).forGetter(FordQuestState::foundTracks),
            Codec.INT.optionalFieldOf("recovery_count", 0).forGetter(FordQuestState::recoveryCount)
    ).apply(instance, FordQuestState::new));

    public FordQuestState {
        schemaVersion = Math.max(SCHEMA_VERSION, schemaVersion);
        stage = stage == null ? "invalid" : stage;
        // Negative / corrupt counts cannot produce a free reset. Higher-than-cap values
        // remain stored; recovery rules refuse all further respawns.
        recoveryCount = Math.max(0, recoveryCount);
    }

    public FordQuestState() {
        this(SCHEMA_VERSION, NOT_STARTED, false, 0);
    }

    public FordQuestState withStage(String newStage) {
        return new FordQuestState(schemaVersion, newStage, foundTracks, recoveryCount);
    }

    public FordQuestState withTracks() {
        return new FordQuestState(schemaVersion, stage, true, recoveryCount);
    }

    public FordQuestState withRecoveredRaider() {
        return new FordQuestState(schemaVersion, stage, foundTracks, recoveryCount + 1);
    }
}
