package com.eretenkodaniil.mkm.character;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.QuestRewardRecovery.Decision;
import com.eretenkodaniil.mkm.character.CharacterRewardRules.Status;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

final class QuestRewardRecoveryTest {
    private static final String ID = "mkm:ford_warden/complete";

    private static CharacterData diskRoundTrip(CharacterData c) {
        var codec = CharacterData.CODEC.codec();
        return codec.parse(JsonOps.INSTANCE, codec.encodeStart(JsonOps.INSTANCE, c)
                .result().orElseThrow()).result().orElseThrow();
    }

    @Test
    void crashCutBeforeAndAfterCharacterSaveReplaysExactlyOnce() {
        CharacterData starting = new CharacterData().withExperience(25);
        // Saved quest still RETURN_TO_WARDEN; saved character is old => grant once.
        assertEquals(Decision.GRANT_ONCE,
                QuestRewardRecovery.decide(diskRoundTrip(starting), ID, true, false, -1));
        var granted = CharacterRewardRules.grantOnce(starting, ID, 75);
        assertEquals(Status.GRANTED, granted.status());
        assertEquals(100, granted.character().experience());
        // Character saved but quest state still RETURN_TO_WARDEN => finish with zero XP.
        CharacterData onDisk = diskRoundTrip(granted.character());
        assertEquals(Decision.FINALIZE_QUEST,
                QuestRewardRecovery.decide(onDisk, ID, true, false, -1));
        assertEquals(Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(onDisk, ID, 75).status());
        // Both saved => never run the floor repair or grant again.
        assertEquals(Decision.COMPLETE,
                QuestRewardRecovery.decide(onDisk, ID, false, true, 125));
    }

    @Test
    void legacyCompletionWithoutReceiptNeverTriggersApproximateTopUp() {
        CharacterData withoutReceipt = new CharacterData().withExperience(25);
        for (long legacyFloor : new long[]{-1, 100, 125, 495000, Long.MAX_VALUE}) {
            assertEquals(Decision.MANUAL_REVIEW, QuestRewardRecovery.decide(
                    withoutReceipt, ID, false, true, legacyFloor));
        }
        // A positive floor is NOT a transaction receipt, even if current XP already exceeds it.
        assertEquals(Decision.MANUAL_REVIEW, QuestRewardRecovery.decide(
                withoutReceipt.withExperience(500), ID, false, true, 100));
    }

    @Test
    void independentOwnersWithSameStableRewardIdCannotCrossRedeem() {
        CharacterData ownerA = CharacterRewardRules.grantOnce(
                new CharacterData(), ID, 100).character();
        CharacterData ownerB = new CharacterData();
        assertEquals(Decision.GRANT_ONCE, QuestRewardRecovery.decide(
                ownerB, ID, true, false, -1));
        CharacterData paidB = CharacterRewardRules.grantOnce(ownerB, ID, 75).character();
        assertEquals(100, ownerA.experience());
        assertEquals(75, paidB.experience());
        assertEquals(Status.ALREADY_GRANTED, CharacterRewardRules.grantOnce(
                ownerA, ID, 100).status());
        assertEquals(Status.ALREADY_GRANTED, CharacterRewardRules.grantOnce(
                paidB, ID, 100).status());
        assertNotSame(ownerA, paidB);
    }

    @Test
    void schemaMalformedIdsAndImpossibleStagePairsFailClosed() {
        CharacterData fresh = new CharacterData();
        assertEquals(Decision.REJECT, QuestRewardRecovery.decide(fresh, ID, false, false, -1));
        assertEquals(Decision.REJECT, QuestRewardRecovery.decide(fresh, ID, true, true, -1));
        assertEquals(Decision.REJECT, QuestRewardRecovery.decide(fresh, ID, true, false, -2));
        assertEquals(Decision.REJECT, QuestRewardRecovery.decide(fresh, "../evil", true, false, -1));
        assertEquals(Decision.REJECT, QuestRewardRecovery.decide(null, ID, true, false, -1));
        CharacterData future = new CharacterData(5, 100, 10, 10, 10, 10, 10, 10,
                10, 2, java.util.List.of(0,0,0,0,0,0,0), java.util.List.of());
        assertEquals(Decision.REJECT, QuestRewardRecovery.decide(future, ID, true, false, -1));
    }
}
