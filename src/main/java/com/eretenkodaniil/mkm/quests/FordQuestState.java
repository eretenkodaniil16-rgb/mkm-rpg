package com.eretenkodaniil.mkm.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Player-scoped state for the first authored quest. The ID and stored stages are save contracts.
 * Unknown future stages/schema versions are retained, not mistaken for a fresh quest.
 */
public record FordQuestState(int schemaVersion, String stage, boolean foundTracks) {
    public static final int SCHEMA_VERSION = 1;
    public static final String NOT_STARTED = "not_started";
    public static final String ACTIVE = "active";
    public static final String RETURN_TO_WARDEN = "return_to_warden";
    public static final String REWARDED = "rewarded";

    public static final Codec<FordQuestState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", SCHEMA_VERSION).forGetter(FordQuestState::schemaVersion),
            Codec.STRING.optionalFieldOf("stage", NOT_STARTED).forGetter(FordQuestState::stage),
            Codec.BOOL.optionalFieldOf("found_tracks", false).forGetter(FordQuestState::foundTracks)
    ).apply(instance, FordQuestState::new));

    public FordQuestState {
        schemaVersion = Math.max(1, schemaVersion);
        stage = stage == null ? "invalid" : stage;
    }

    public FordQuestState() {
        this(SCHEMA_VERSION, NOT_STARTED, false);
    }

    public FordQuestState withStage(String newStage) {
        return new FordQuestState(schemaVersion, newStage, foundTracks);
    }

    public FordQuestState withTracks() {
        return new FordQuestState(schemaVersion, stage, true);
    }
}
