package com.eretenkodaniil.mkm.character;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.CharacterRewardRules.Status;
import com.mojang.serialization.JsonOps;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Deterministic long-running replay simulation with independently persisted players. */
final class RewardReplayFuzzTest {
    private static final int STEPS = 3500;
    private static final int QUEST_IDS = 340;

    private static CharacterData persisted(CharacterData current) {
        var codec = CharacterData.CODEC.codec();
        return codec.parse(JsonOps.INSTANCE, codec.encodeStart(JsonOps.INSTANCE, current)
                .result().orElseThrow()).result().orElseThrow();
    }

    @Test
    void randomKillQuestReplayRelogAndAdminXpChangesNeverMintDuplicateReward() {
        Random random = new Random(0x43A8_29B1L);
        CharacterData[] players = {new CharacterData(), new CharacterData()};
        @SuppressWarnings("unchecked")
        Set<String>[] paid = new Set[]{new HashSet<String>(), new HashSet<String>()};
        long cap = ProgressionRules.experienceForLevel(ProgressionRules.MAX_LEVEL);

        for (int step = 0; step < STEPS; step++) {
            int owner = random.nextInt(2);
            CharacterData before = players[owner];
            int type = random.nextInt(9);
            if (type <= 4) {
                String id = "mkm:quest/instance_" + random.nextInt(QUEST_IDS);
                long amount = 1 + random.nextInt(1000);
                boolean first = paid[owner].add(id);
                var result = CharacterRewardRules.grantOnce(before, id, amount);
                assertEquals(first ? Status.GRANTED : Status.ALREADY_GRANTED, result.status());
                if (first) {
                    assertEquals(ProgressionRules.experienceAfterReward(before.experience(), amount),
                            result.character().experience());
                    assertTrue(result.character().hasClaimedReward(id));
                } else {
                    assertSame(before, result.character());
                    assertEquals(0L, result.creditedExperience());
                }
                players[owner] = result.character();
            } else if (type == 5) {
                players[owner] = before.withExperience(
                        ProgressionRules.experienceAfterReward(before.experience(), 25));
            } else if (type == 6) {
                // Simulates an admin adjustment. A settled receipt must not be credited again.
                players[owner] = before.withExperience(Math.max(0L, before.experience() - 20));
            } else {
                // Simulate save/relog and the immutable data copied across respawn.
                players[owner] = persisted(before);
                assertEquals(before, players[owner]);
            }

            CharacterData after = players[owner];
            assertEquals(paid[owner].size(), after.claimedRewardIds().size(), "step " + step);
            assertEquals(paid[owner], new HashSet<>(after.claimedRewardIds()));
            assertTrue(after.earnedPoints() >= before.earnedPoints(), "earned points rollback " + step);
            assertEquals(0, after.spentPoints());
            assertEquals(after.earnedPoints(), after.availablePoints());
            assertTrue(after.experience() >= 0);
            assertTrue(after.experience() <= cap);
        }
        assertFalse(paid[0].isEmpty());
        assertFalse(paid[1].isEmpty());
        assertTrue(players[0].claimedRewardIds().stream()
                .allMatch(CharacterRewardRules::validRewardId));
    }

    @Test
    void permanentClaimIdMustNotBeReusedForDistinctRepeatingQuests() {
        CharacterData start = new CharacterData();
        CharacterData first = CharacterRewardRules.grantOnce(
                start, "mkm:bounty/contract_1", 50).character();
        assertEquals(Status.ALREADY_GRANTED, CharacterRewardRules.grantOnce(
                persisted(first), "mkm:bounty/contract_1", 100).status());
        var nextInstance = CharacterRewardRules.grantOnce(
                first, "mkm:bounty/contract_2", 100);
        assertEquals(Status.GRANTED, nextInstance.status());
        assertEquals(150L, nextInstance.character().experience());
    }
}
