package com.eretenkodaniil.mkm.quests;

import static org.junit.jupiter.api.Assertions.*;
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
    void perceptionIsDeterministicAndOnlyApplicableBeforeEnemyIsDefeated() {
        FordQuestState state = new FordQuestState();
        assertSame(state, FordQuestRules.inspect(state, 11));
        FordQuestState inspected = FordQuestRules.inspect(state, 12);
        assertTrue(inspected.foundTracks());
        assertEquals(100L, FordQuestRules.rewardExperience(inspected));
        assertEquals(75L, FordQuestRules.rewardExperience(state));
        assertTrue(FordQuestRules.accept(inspected).foundTracks());
        FordQuestState ready = FordQuestRules.onOwnedRaiderDefeated(FordQuestRules.accept(state));
        assertSame(ready, FordQuestRules.inspect(ready, 100));
    }

    @Test
    void unknownFutureOrCorruptStageDoesNotResetToNewQuest() {
        FordQuestState future = new FordQuestState(2, "branch_future", false);
        assertSame(future, FordQuestRules.accept(future));
        assertSame(future, FordQuestRules.onOwnedRaiderDefeated(future));
        assertSame(future, FordQuestRules.claim(future));
        FordQuestState unknown = new FordQuestState(1, "unexpected", false);
        assertSame(unknown, FordQuestRules.claim(unknown));
        assertSame(unknown, FordQuestRules.accept(unknown));
    }

    @Test
    void mustNotClaimBeforeEncounter() {
        FordQuestState start = new FordQuestState();
        assertSame(start, FordQuestRules.claim(start));
        assertSame(start, FordQuestRules.onOwnedRaiderDefeated(start));
        assertEquals(FordQuestState.NOT_STARTED, start.stage());
    }
}
