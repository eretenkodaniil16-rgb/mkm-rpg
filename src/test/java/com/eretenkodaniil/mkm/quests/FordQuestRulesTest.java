package com.eretenkodaniil.mkm.quests;

import static org.junit.jupiter.api.Assertions.*;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

final class FordQuestRulesTest {
    @Test
    void fullLoopAndRepeatedEventsCannotPayTwice() {
        FordQuestState start = new FordQuestState();
        FordQuestState accepted = FordQuestRules.accept(start);
        assertEquals(FordQuestState.ACTIVE, accepted.stage());
        assertSame(accepted, FordQuestRules.accept(accepted));
        assertSame(accepted, FordQuestRules.claim(accepted));
        FordQuestState defeated = FordQuestRules.onOwnedRaiderDefeated(accepted);
        assertEquals(FordQuestState.RETURN_TO_WARDEN, defeated.stage());
        assertSame(defeated, FordQuestRules.onOwnedRaiderDefeated(defeated));
        FordQuestState claimed = FordQuestRules.claim(defeated);
        assertEquals(FordQuestState.REWARDED, claimed.stage());
        assertSame(claimed, FordQuestRules.claim(claimed));
        assertSame(claimed, FordQuestRules.onOwnedRaiderDefeated(claimed));
    }

    @Test
    void checksAreDeterministicAndOnlyBeforeEnemyIsDefeated() {
        FordQuestState state = new FordQuestState();
        assertSame(state, FordQuestRules.inspect(state, 11));
        FordQuestState inspected = FordQuestRules.inspect(state, 12);
        assertTrue(inspected.foundTracks());
        assertSame(inspected, FordQuestRules.inspect(inspected, 100));
        assertEquals(100L, FordQuestRules.rewardExperience(inspected));
        assertEquals(75L, FordQuestRules.rewardExperience(state));
        assertTrue(FordQuestRules.accept(inspected).foundTracks());
        FordQuestState ready = FordQuestRules.onOwnedRaiderDefeated(FordQuestRules.accept(state));
        assertSame(ready, FordQuestRules.inspect(ready, 100));
    }

    @Test
    void unknownFutureOrCorruptStageFailsClosed() {
        FordQuestState future = new FordQuestState(3, "branch_future", false, 0);
        assertSame(future, FordQuestRules.accept(future));
        assertSame(future, FordQuestRules.onOwnedRaiderDefeated(future));
        assertSame(future, FordQuestRules.claim(future));
        assertFalse(FordQuestRules.canRecover(future));
        FordQuestState unknown = new FordQuestState(2, "unexpected", false, 0);
        assertSame(unknown, FordQuestRules.claim(unknown));
        assertSame(unknown, FordQuestRules.accept(unknown));
        assertFalse(FordQuestRules.canRecover(unknown));
        FordQuestState invalidSchema = new FordQuestState(0, FordQuestState.ACTIVE, false, 0);
        assertFalse(FordQuestRules.canRecover(invalidSchema));
        assertSame(invalidSchema, FordQuestRules.claim(invalidSchema));
        FordQuestState invalidRecovery = new FordQuestState(2, FordQuestState.ACTIVE, false, -1);
        assertFalse(FordQuestRules.canRecover(invalidRecovery));
    }

    @Test
    void mustNotClaimBeforeEncounter() {
        FordQuestState start = new FordQuestState();
        assertSame(start, FordQuestRules.claim(start));
        assertSame(start, FordQuestRules.onOwnedRaiderDefeated(start));
        assertFalse(FordQuestRules.canRecover(start));
    }

    @Test
    void recoveryHasDurableBoundAndCannotChangeAwardedStage() {
        FordQuestState state = FordQuestRules.accept(new FordQuestState());
        assertTrue(FordQuestRules.canRecover(state));
        for (int i = 1; i <= FordQuestRules.MAX_RECOVERIES; i++) {
            state = FordQuestRules.onRecoveredRaiderSpawned(state);
            assertEquals(i, state.recoveryCount());
        }
        assertFalse(FordQuestRules.canRecover(state));
        assertSame(state, FordQuestRules.onRecoveredRaiderSpawned(state));
        FordQuestState completed = FordQuestRules.claim(FordQuestRules.onOwnedRaiderDefeated(state));
        assertSame(completed, FordQuestRules.onRecoveredRaiderSpawned(completed));
        assertEquals(FordQuestState.REWARDED, completed.stage());
    }

    @Test
    void persistentCodecRoundTripsClaimedAndFutureState() {
        for (FordQuestState saved : new FordQuestState[] {
                new FordQuestState(),
                new FordQuestState(2, FordQuestState.REWARDED, true, 1),
                new FordQuestState(3, "future_stage", false, 5) }) {
            var encoded = FordQuestState.CODEC.encodeStart(JsonOps.INSTANCE, saved).getOrThrow();
            var restored = FordQuestState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
            assertEquals(saved, restored);
        }
    }

    @Test
    void schemaV1MigratesWithoutResettingQuestOrInvestigation() {
        var encoded = JsonParser.parseString(
                "{\"schema_version\":1,\"stage\":\"return_to_warden\",\"found_tracks\":true}");
        FordQuestState restored = FordQuestState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(FordQuestState.SCHEMA_VERSION, restored.schemaVersion());
        assertEquals(FordQuestState.RETURN_TO_WARDEN, restored.stage());
        assertTrue(restored.foundTracks());
        assertEquals(0, restored.recoveryCount());
        assertEquals(100L, FordQuestRules.rewardExperience(restored));
    }

    @Test
    void oldOrEmptyCodecDefaultsSafely() {
        FordQuestState restored = FordQuestState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow();
        assertEquals(new FordQuestState(), restored);
        var corrupted = JsonParser.parseString("{\"schema_version\":3,\"stage\":\"active\"}");
        FordQuestState future = FordQuestState.CODEC.parse(JsonOps.INSTANCE, corrupted).getOrThrow();
        assertFalse(FordQuestRules.canRecover(future));
        assertSame(future, FordQuestRules.accept(future));
    }
}
