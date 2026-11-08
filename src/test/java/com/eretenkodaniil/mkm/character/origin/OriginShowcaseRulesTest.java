package com.eretenkodaniil.mkm.character.origin;

import static org.junit.jupiter.api.Assertions.*;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterRewardRules;
import com.eretenkodaniil.mkm.quests.FordQuestState;
import org.junit.jupiter.api.Test;

final class OriginShowcaseRulesTest {
    private static CharacterOriginData pair(String race, String clazz) {
        return CharacterOriginRules.select(new CharacterOriginData(), race, clazz, 0).data();
    }
    @Test void raceRoutesAreDistinctPerPlayerAndNeverGrantOtherRaces() {
        for (String clazz : new String[]{"mkm:warrior", "mkm:mage"}) {
            var human = pair("mkm:human", clazz);
            var dwarf = pair("mkm:dwarf", clazz);
            for (String stage : new String[]{FordQuestState.NOT_STARTED, FordQuestState.ACTIVE}) {
                var quest = new FordQuestState().withStage(stage);
                assertTrue(OriginShowcaseRules.canDiscover(human, quest,
                        OriginShowcaseRules.Clue.HUMAN_PARLEY));
                assertFalse(OriginShowcaseRules.canDiscover(human, quest,
                        OriginShowcaseRules.Clue.DWARF_RUNES));
                assertFalse(OriginShowcaseRules.canDiscover(dwarf, quest,
                        OriginShowcaseRules.Clue.HUMAN_PARLEY));
                assertTrue(OriginShowcaseRules.canDiscover(dwarf, quest,
                        OriginShowcaseRules.Clue.DWARF_RUNES));
                assertFalse(OriginShowcaseRules.canDiscover(dwarf, quest.withTracks(),
                        OriginShowcaseRules.Clue.DWARF_RUNES));
            }
            assertFalse(OriginShowcaseRules.canDiscover(human,
                    new FordQuestState().withStage(FordQuestState.REWARDED),
                    OriginShowcaseRules.Clue.HUMAN_PARLEY));
            assertTrue(OriginShowcaseRules.allowedClassAction(human, clazz));
            assertTrue(OriginShowcaseRules.allowedClassAction(dwarf, clazz));
            assertFalse(OriginShowcaseRules.allowedClassAction(human,
                    clazz.equals("mkm:warrior") ? "mkm:mage" : "mkm:warrior"));
        }
    }

    @Test void levelTwoPlusClueAndVerifiedRewardUnlockExactlyOnceWithoutMoreXp() {
        var origin = pair("mkm:dwarf", "mkm:mage");
        var progress = new OriginProgressData();
        var quest = new FordQuestState().withStage(FordQuestState.REWARDED).withTracks();
        var old = new CharacterData().withExperience(99);
        assertEquals(OriginShowcaseRules.Unlock.NOT_READY,
                OriginShowcaseRules.canLearn(old, origin, progress, quest));
        old = old.withExperience(100);
        assertEquals(OriginShowcaseRules.Unlock.GRANTED,
                OriginShowcaseRules.canLearn(old, origin, progress, quest));
        assertEquals(OriginShowcaseRules.Unlock.NOT_READY,
                OriginShowcaseRules.canLearn(old, origin, progress,
                        new FordQuestState().withStage(FordQuestState.REWARDED)));
        assertEquals(OriginShowcaseRules.Unlock.NOT_READY,
                OriginShowcaseRules.canLearn(old, origin, progress,
                        new FordQuestState().withTracks()));
        long xp = old.experience();
        var settled = CharacterRewardRules.grantOnce(old, "mkm:ford_warden/complete", 25).character();
        var mastered = progress.unlock(OriginProgressData.FORD_MENTORSHIP);
        assertTrue(mastered.has(OriginProgressData.FORD_MENTORSHIP));
        assertEquals(OriginShowcaseRules.Unlock.ALREADY_UNLOCKED,
                OriginShowcaseRules.canLearn(settled, origin, mastered, quest));
        assertEquals(xp + 25, settled.experience());
        assertEquals(settled, settled, "mentoring does not mutate CharacterData or quest reward receipts");
    }

    @Test void fakeOldAndUnknownCatalogIdsNeverGrantOrTrain() {
        CharacterData v4 = new CharacterData().withExperience(100);
        FordQuestState completed = new FordQuestState()
                .withStage(FordQuestState.REWARDED).withTracks();
        var progress = new OriginProgressData();
        var future = new CharacterOriginData(2, 1, "mkm:dwarf", "mkm:warrior");
        var unknown = new CharacterOriginData(1, 1, "mkm:alien", "mkm:warrior");
        var empty = new CharacterOriginData();
        for (var origin : new CharacterOriginData[]{future, unknown, empty}) {
            assertEquals(OriginShowcaseRules.Unlock.UNSUPPORTED,
                    OriginShowcaseRules.canLearn(v4, origin, progress, completed));
            assertFalse(OriginShowcaseRules.canDiscover(origin, new FordQuestState(),
                    OriginShowcaseRules.Clue.DWARF_RUNES));
            assertFalse(OriginShowcaseRules.allowedClassAction(origin, "mkm:warrior"));
        }
        assertEquals(OriginShowcaseRules.Unlock.UNSUPPORTED,
                OriginShowcaseRules.canLearn(v4,
                        pair("mkm:human", "mkm:mage"),
                        new OriginProgressData(2, java.util.List.of()), completed));
    }
}
