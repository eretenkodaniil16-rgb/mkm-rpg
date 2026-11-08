package com.eretenkodaniil.mkm.showcase;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One player's narrative choices, independent of Ilra's legacy v3 quest and
 * CharacterData v4. No external source can reset a completed episode.
 */
public record ShowcaseProgress(int schemaVersion, String stage, String approach) {
    public static final int SCHEMA_VERSION = 1;
    public static final String UNKNOWN = "unknown", FOUND = "found",
            ACTIVE = "active", COMPLETED = "completed";
    public static final String NONE = "none", DEFEND = "defend", STUDY = "study";

    public static final Codec<ShowcaseProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("schema_version", 1).forGetter(ShowcaseProgress::schemaVersion),
            Codec.STRING.optionalFieldOf("stage", UNKNOWN).forGetter(ShowcaseProgress::stage),
            Codec.STRING.optionalFieldOf("approach", NONE).forGetter(ShowcaseProgress::approach)
    ).apply(i, ShowcaseProgress::new));

    public ShowcaseProgress {
        if (schemaVersion != SCHEMA_VERSION
                || !java.util.List.of(UNKNOWN, FOUND, ACTIVE, COMPLETED).contains(stage)
                || !java.util.List.of(NONE, DEFEND, STUDY).contains(approach)
                || (stage.equals(UNKNOWN) || stage.equals(FOUND)) && !approach.equals(NONE)
                || (stage.equals(ACTIVE) || stage.equals(COMPLETED)) && approach.equals(NONE))
            throw new IllegalArgumentException("Unsupported or corrupt Showcase progress");
    }
    public ShowcaseProgress() { this(SCHEMA_VERSION, UNKNOWN, NONE); }
    public ShowcaseProgress discover() { return stage.equals(UNKNOWN)
            ? new ShowcaseProgress(schemaVersion, FOUND, NONE) : this; }
    public ShowcaseProgress begin(String choice, boolean eligible) {
        if (!stage.equals(FOUND) || !eligible
                || !java.util.List.of(DEFEND, STUDY).contains(choice)) return this;
        return new ShowcaseProgress(schemaVersion, ACTIVE, choice);
    }
    public ShowcaseProgress finish() { return stage.equals(ACTIVE)
            ? new ShowcaseProgress(schemaVersion, COMPLETED, approach) : this; }
}
