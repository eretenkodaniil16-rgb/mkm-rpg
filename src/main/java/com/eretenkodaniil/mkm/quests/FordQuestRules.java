package com.eretenkodaniil.mkm.quests;

/**
 * Pure quest transition rules. Minecraft entities, client packets, and rewards are intentionally
 * outside this class. A repeated or out-of-order event never advances a completed quest.
 */
public final class FordQuestRules {
    public static final int TRACKS_PERCEPTION_THRESHOLD = 12;
    public static final long BASE_XP_REWARD = 75L;
    public static final long TRACKS_XP_BONUS = 25L;

    private FordQuestRules() {}

    public static FordQuestState inspect(FordQuestState state, int perception) {
        if (state.schemaVersion() != FordQuestState.SCHEMA_VERSION
                || perception < TRACKS_PERCEPTION_THRESHOLD
                || !(state.stage().equals(FordQuestState.NOT_STARTED)
                || state.stage().equals(FordQuestState.ACTIVE))) {
            return state;
        }
        return state.withTracks();
    }

    public static FordQuestState accept(FordQuestState state) {
        if (state.schemaVersion() != FordQuestState.SCHEMA_VERSION
                || !state.stage().equals(FordQuestState.NOT_STARTED)) {
            return state;
        }
        return state.withStage(FordQuestState.ACTIVE);
    }

    public static FordQuestState onOwnedRaiderDefeated(FordQuestState state) {
        if (state.schemaVersion() != FordQuestState.SCHEMA_VERSION
                || !state.stage().equals(FordQuestState.ACTIVE)) {
            return state;
        }
        return state.withStage(FordQuestState.RETURN_TO_WARDEN);
    }

    public static FordQuestState claim(FordQuestState state) {
        if (state.schemaVersion() != FordQuestState.SCHEMA_VERSION
                || !state.stage().equals(FordQuestState.RETURN_TO_WARDEN)) {
            return state;
        }
        return state.withStage(FordQuestState.REWARDED);
    }

    public static long rewardExperience(FordQuestState state) {
        return BASE_XP_REWARD + (state.foundTracks() ? TRACKS_XP_BONUS : 0L);
    }
}
