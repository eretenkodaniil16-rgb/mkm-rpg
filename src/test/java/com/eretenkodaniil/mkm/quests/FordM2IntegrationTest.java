package com.eretenkodaniil.mkm.quests;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterRewardRules;
import com.eretenkodaniil.mkm.character.QuestRewardRecovery;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Saved crash-cut model using the real M2 immutable receipt algorithm, not a duplicate XP formula. */
final class FordM2IntegrationTest {
    private static final String REWARD_ID = "mkm:ford_warden/complete";

    @Test void awardBeforeStagePreventsDuplicateOnAnyConsistentSavedCut() {
        FordQuestState ready = FordQuestRules.onOwnedRaiderDefeated(
                FordQuestRules.accept(new FordQuestState()));
        CharacterData initial = new CharacterData().withExperience(25);
        assertEquals(QuestRewardRecovery.Decision.GRANT_ONCE,
                QuestRewardRecovery.decide(initial, REWARD_ID, true, false, ready.rewardXpFloor()));
        var first = CharacterRewardRules.grantOnce(initial, REWARD_ID, FordQuestRules.rewardExperience(ready));
        assertEquals(CharacterRewardRules.Status.GRANTED, first.status());
        assertEquals(100L, first.character().experience());
        assertEquals(2, first.character().earnedPoints());
        assertTrue(first.character().hasClaimedReward(REWARD_ID));
        // Crash after Character M2/M3 receipt commit but before E2 stage transition.
        assertEquals(QuestRewardRecovery.Decision.FINALIZE_QUEST,
                QuestRewardRecovery.decide(first.character(),REWARD_ID,true,false,-1L));
        var afterCrash = CharacterRewardRules.grantOnce(first.character(), REWARD_ID,
                FordQuestRules.rewardExperience(ready));
        assertEquals(CharacterRewardRules.Status.ALREADY_GRANTED, afterCrash.status());
        assertSame(first.character(), afterCrash.character());
        FordQuestState rewarded = FordQuestRules.claim(ready);
        assertEquals(FordQuestState.REWARDED, rewarded.stage());
        assertSame(rewarded, FordQuestRules.claim(rewarded));
        assertEquals(QuestRewardRecovery.Decision.COMPLETE,
                QuestRewardRecovery.decide(first.character(),REWARD_ID,false,true,-1L));
    }

    @Test void multiplePlayersHaveIndependentReceiptsAndQuests() {
        FordQuestState a = FordQuestRules.accept(new FordQuestState())
                .withEncounter("11111111-2222-3333-4444-555555555555", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        FordQuestState b = FordQuestRules.accept(new FordQuestState())
                .withEncounter("99999999-2222-3333-4444-555555555555", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        assertFalse(FordQuestRules.matchesEncounter(a,b.encounterId()));
        assertFalse(FordQuestRules.matchesEncounter(b,a.encounterId()));
        var ra = CharacterRewardRules.grantOnce(new CharacterData(), REWARD_ID,75);
        var rb = CharacterRewardRules.grantOnce(new CharacterData(), REWARD_ID,100);
        assertEquals(75,ra.character().experience());
        assertEquals(100,rb.character().experience());
        assertEquals(CharacterRewardRules.Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(ra.character(),REWARD_ID,75).status());
    }

    @Test void legacyRewardedStateDoesNotGrantAgainAndNoAutoRepair() {
        FordQuestState legacy = new FordQuestState(2, FordQuestState.REWARDED, true, 0);
        assertEquals(FordQuestState.REWARDED, legacy.stage());
        assertSame(legacy, FordQuestRules.claim(legacy));
        // The legacy floor is not proof of a receipt and must NEVER trigger a claim.
        FordQuestState withFloor = legacy.withRewardFloor(500);
        assertSame(withFloor, FordQuestRules.claim(withFloor));
        assertEquals(QuestRewardRecovery.Decision.MANUAL_REVIEW,
                QuestRewardRecovery.decide(new CharacterData(),REWARD_ID,false,true,500));
        var encoded = FordQuestState.CODEC.encodeStart(JsonOps.INSTANCE,withFloor).getOrThrow();
        assertEquals(withFloor,FordQuestState.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow());
    }
}
