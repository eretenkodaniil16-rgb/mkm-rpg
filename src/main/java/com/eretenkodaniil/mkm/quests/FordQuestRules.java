package com.eretenkodaniil.mkm.quests;

/** Pure stage, identity and reward constraints; no client-authoritative outcomes. */
public final class FordQuestRules {
    public static final int TRACKS_PERCEPTION_THRESHOLD = 12;
    public static final long BASE_XP_REWARD = 75L;
    public static final long TRACKS_XP_BONUS = 25L;
    public static final int MAX_RECOVERIES = 2;
    private FordQuestRules() {}

    public static FordQuestState inspect(FordQuestState state, int perception) {
        if (state.schemaVersion() != FordQuestState.SCHEMA_VERSION
                || state.foundTracks() || perception < TRACKS_PERCEPTION_THRESHOLD
                || !(state.stage().equals(FordQuestState.NOT_STARTED)
                || state.stage().equals(FordQuestState.ACTIVE))) return state;
        return state.withTracks();
    }

    public static FordQuestState accept(FordQuestState state) {
        if (state.schemaVersion() != FordQuestState.SCHEMA_VERSION
                || !state.stage().equals(FordQuestState.NOT_STARTED)) return state;
        return state.withStage(FordQuestState.ACTIVE);
    }

    public static FordQuestState onOwnedRaiderDefeated(FordQuestState state) {
        if (state.schemaVersion() != FordQuestState.SCHEMA_VERSION
                || !state.stage().equals(FordQuestState.ACTIVE)) return state;
        return state.withStage(FordQuestState.RETURN_TO_WARDEN);
    }

    /** Legacy helper, retained to test v2 recovery budget; automatic re-spawns are disabled in E2. */
    public static boolean canRecover(FordQuestState state) {
        return state.schemaVersion() == FordQuestState.SCHEMA_VERSION
                && state.stage().equals(FordQuestState.ACTIVE)
                && state.recoveryCount() < MAX_RECOVERIES;
    }

    public static FordQuestState onRecoveredRaiderSpawned(FordQuestState state) {
        return canRecover(state) ? state.withRecoveredRaider() : state;
    }

    public static FordQuestState claim(FordQuestState state) {
        if (state.schemaVersion() != FordQuestState.SCHEMA_VERSION
                || !state.stage().equals(FordQuestState.RETURN_TO_WARDEN)) return state;
        return state.withStage(FordQuestState.REWARDED);
    }

    /** One-time claim also records a cumulative XP floor for repair on relog/respawn. */
    public static FordQuestState claimAtExperience(FordQuestState state, long currentXp,
                                                    long maxLevelXp) {
        if (state.schemaVersion() != FordQuestState.SCHEMA_VERSION
                || !state.stage().equals(FordQuestState.RETURN_TO_WARDEN)
                || currentXp < 0 || maxLevelXp < 0) return state;
        long amount = rewardExperience(state);
        long candidate = currentXp > Long.MAX_VALUE - amount
                ? Long.MAX_VALUE : currentXp + amount;
        long floor = currentXp >= maxLevelXp ? currentXp : Math.min(maxLevelXp, candidate);
        return state.withRewardFloor(floor);
    }

    public static long missingExperience(FordQuestState state, long actualXp) {
        if (state.schemaVersion() != FordQuestState.SCHEMA_VERSION
                || !state.stage().equals(FordQuestState.REWARDED)
                || state.rewardXpFloor() < 0L || actualXp < 0L) return 0L;
        return Math.max(0L, state.rewardXpFloor() - actualXp);
    }

    public static boolean matchesEncounter(FordQuestState state, String actualId) {
        // Legacy v1/v2 may have no tracked ID. They can still progress via owner tags,
        // but only after a live server-side entity is verified.
        return state.schemaVersion() == FordQuestState.SCHEMA_VERSION
                && state.stage().equals(FordQuestState.ACTIVE)
                && (state.encounterId().isEmpty() || state.encounterId().equals(actualId));
    }

    public static long rewardExperience(FordQuestState state) {
        return BASE_XP_REWARD + (state.foundTracks() ? TRACKS_XP_BONUS : 0L);
    }
}
