package com.eretenkodaniil.mkm.quests;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterRewardRules;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure cross-core E2 episode walkthrough using real quest, reservation and M2 reward rules.
 * This is NOT a replacement for a real two-client Fabric game test.
 */
final class FordEpisodeContractTest {
    private static final String REWARD = "mkm:ford_warden/complete";

    @Test void twoPlayerEpisodePerceptionOwnerIsolationAndReplay() {
        FordQuestState a = new FordQuestState();
        FordQuestState b = new FordQuestState();
        assertSame(a, FordQuestRules.inspect(a, 10));
        a = FordQuestRules.inspect(a, 12);
        assertTrue(a.foundTracks());
        assertFalse(b.foundTracks());

        assertTrue(FordDialogueRules.permitted((byte)0,(byte)FordQuestChoice.ACCEPT));
        a = FordQuestRules.accept(a).withEncounter("raider-a", "warden-a");
        b = FordQuestRules.accept(b).withEncounter("raider-b", "warden-a");

        var registry = new FordEncounterRegistry()
                .register("owner-a", new FordEncounterRegistry.Entry("raider-a","warden-a",8,65,8))
                .register("owner-b", new FordEncounterRegistry.Entry("raider-b","warden-a",12,65,8));
        assertEquals(2,registry.byOwner().size());
        assertFalse(FordQuestRules.matchesEncounter(a,"raider-b"));
        assertFalse(FordQuestRules.matchesEncounter(b,"raider-a"));
        assertEquals(2,registry.removeIfMatch("owner-a","raider-b").byOwner().size());

        assertSame(a,FordQuestRules.onOwnedRaiderDefeated(a.withStage(FordQuestState.RETURN_TO_WARDEN)));
        a = FordQuestRules.onOwnedRaiderDefeated(a);
        assertEquals(FordQuestState.RETURN_TO_WARDEN,a.stage());
        assertEquals(FordQuestState.ACTIVE,b.stage());
        assertEquals(1,registry.removeIfMatch("owner-a","raider-a").byOwner().size());

        CharacterData characterA = new CharacterData();
        CharacterData characterB = new CharacterData();
        var grantA = CharacterRewardRules.grantOnce(characterA,REWARD,FordQuestRules.rewardExperience(a));
        assertEquals(CharacterRewardRules.Status.GRANTED,grantA.status());
        assertEquals(100L,grantA.character().experience());
        assertEquals(0L,characterB.experience());
        assertEquals(CharacterRewardRules.Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(grantA.character(),REWARD,100L).status());
        a = FordQuestRules.claim(a);
        assertEquals(FordQuestState.REWARDED,a.stage());
        assertSame(a,FordQuestRules.claim(a));

        var questJson = FordQuestState.CODEC.encodeStart(JsonOps.INSTANCE,a).getOrThrow();
        assertEquals(a,FordQuestState.CODEC.parse(JsonOps.INSTANCE,questJson).getOrThrow());
        var registryJson = FordEncounterRegistry.CODEC.encodeStart(JsonOps.INSTANCE,registry).getOrThrow();
        assertEquals(registry,FordEncounterRegistry.CODEC.parse(JsonOps.INSTANCE,registryJson).getOrThrow());
    }

    private static final class FordQuestChoice { static final byte ACCEPT = 2; }
}
