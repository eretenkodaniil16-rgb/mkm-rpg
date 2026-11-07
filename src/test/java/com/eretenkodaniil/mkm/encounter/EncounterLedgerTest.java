package com.eretenkodaniil.mkm.encounter;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterRewardRules;
import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class EncounterLedgerTest {
    private static EncounterLedger.Entry saved(String owner) {
        return new EncounterLedger.Entry(1, owner, 0, 64, 0, 100, EncounterLedger.RESERVED, List.of(
                new EncounterLedger.Member("mob-h1", "minecraft:husk", "frontline", false, false),
                new EncounterLedger.Member("mob-h2", "minecraft:husk", "frontline", false, false),
                new EncounterLedger.Member("mob-s1", "minecraft:skeleton", "leader", true, false)));
    }

    @Test void preSpawnReservationPreventsReplayAndCountsBudget() {
        EncounterLedger empty = new EncounterLedger();
        var reserved = empty.reserve("mkm:ford_ambush", saved("owner-a"));
        assertEquals(3, reserved.activeMobReservations());
        assertFalse(reserved.canReserve("mkm:ford_ambush", 3));
        assertSame(reserved, reserved.reserve("mkm:ford_ambush", saved("owner-b")));
        assertSame(reserved, reserved.markDeath("mkm:ford_ambush", "unrelated-mob"));
        assertEquals("owner-a",reserved.find("mkm:ford_ambush").ownerId());
    }

    @Test void savedRosterDoesNotTreatUnloadedOrMissingMobAsKilled() {
        var reserved = new EncounterLedger().reserve("mkm:ford_ambush", saved("owner-a"));
        var active = reserved.replace("mkm:ford_ambush",reserved.find("mkm:ford_ambush"),
                reserved.find("mkm:ford_ambush").start());
        var encoded = EncounterLedger.CODEC.encodeStart(JsonOps.INSTANCE, active).getOrThrow();
        var restarted = EncounterLedger.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(3, restarted.activeMobReservations());
        assertEquals(EncounterLedger.ACTIVE, restarted.find("mkm:ford_ambush").phase());
        assertEquals(3, restarted.find("mkm:ford_ambush").aliveCount());
        assertFalse(restarted.canReserve("mkm:ford_ambush", 3));
    }

    @Test void oneExactDeathEventPerMemberAndOnlyOneTerminalReward() {
        var ledger = new EncounterLedger().reserve("mkm:ford_ambush",saved("owner-a"));
        ledger = ledger.replace("mkm:ford_ambush",ledger.find("mkm:ford_ambush"),
                ledger.find("mkm:ford_ambush").start());
        var once = ledger.markDeath("mkm:ford_ambush","mob-h1");
        assertEquals(2,once.find("mkm:ford_ambush").aliveCount());
        assertSame(once,once.markDeath("mkm:ford_ambush","mob-h1"));
        var all = once.markDeath("mkm:ford_ambush","mob-h2")
                .markDeath("mkm:ford_ambush","mob-s1");
        assertEquals(EncounterLedger.DEFEATED,all.find("mkm:ford_ambush").phase());
        assertEquals(0,all.find("mkm:ford_ambush").aliveCount());
        var claimed = all.claim("mkm:ford_ambush");
        assertEquals(EncounterLedger.CLAIMED,claimed.find("mkm:ford_ambush").phase());
        assertSame(claimed,claimed.claim("mkm:ford_ambush"));
        assertSame(claimed,claimed.markDeath("mkm:ford_ambush","mob-s1"));
        assertFalse(claimed.canReserve("mkm:ford_ambush",3));
    }

    @Test void ownerScopedCharacterV4ReceiptNotDuplicateOnCrashRetry() {
        var claimed = new EncounterLedger().reserve("mkm:ford_ambush", saved("owner-a"));
        var a = new CharacterData();
        var b = new CharacterData();
        String reward = "mkm:encounter/ford_ambush";
        var receipt = CharacterRewardRules.grantOnce(a,reward,
                claimed.find("mkm:ford_ambush").rewardXp());
        assertEquals(CharacterRewardRules.Status.GRANTED,receipt.status());
        assertEquals(100L,receipt.character().experience());
        assertEquals(0L,b.experience());
        var replay = CharacterRewardRules.grantOnce(receipt.character(),reward,100);
        assertEquals(CharacterRewardRules.Status.ALREADY_GRANTED,replay.status());
        assertSame(receipt.character(), replay.character());
        assertFalse(claimed.find("mkm:ford_ambush").ownerId().equals("owner-b"));
    }

    @Test void futureSchemaRejectedInsteadOfCorruptingExistingSave() {
        assertThrows(IllegalArgumentException.class,() -> new EncounterLedger.Entry(
                2,"owner",0,64,0,100,EncounterLedger.ACTIVE,saved("owner").members()));
    }
}
