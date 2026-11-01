package com.eretenkodaniil.mkm.quests;

/**
 * Server-agnostic quest transitions. All inputs are validated separately by the server service.
 * Repeated and out-of-order events never advance an already rewarded quest.
 */
public final class FordQuestRules {
    public static final int TRACKS_PERCEPTION_THRESHOLD = 12;
    public static final long BASE_XP_REWARD = 75L;
    public static final long TRACKS_XP_BONUS = 25L;
    /** Pilot-only bound: prevents unbounded hostile spawns and M1 kill-XP farming. */
    public static final int MAX_RECOVERIES = 2;

    private FordQuestRules() {}

    public static FordQuestState inspect(FordQuestState state, int perception) {
        if (state.schemaVersion() != FordQuestState.SCHEMA_VERSION
                || state.foundTracks()
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

    public static boolean canRecover(FordQuestState state) {
        return state.schemaVersion() == FordQuestState.SCHEMA_VERSION
                && state.stage().equals(FordQuestState.ACTIVE)
                && state.recoveryCount() < MAX_RECOVERIES;
    }

    /** Must only be called after actual, validated successful server-side entity spawn. */
    public static FordQuestState onRecoveredRaiderSpawned(FordQuestState state) {
        return canRecover(state) ? state.withRecoveredRaider() : state;
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
