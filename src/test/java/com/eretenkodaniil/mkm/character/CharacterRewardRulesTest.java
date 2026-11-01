package com.eretenkodaniil.mkm.character;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.CharacterRewardRules.Result;
import com.eretenkodaniil.mkm.character.CharacterRewardRules.Status;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CharacterRewardRulesTest {
    private static final String FORD = "mkm:ford_warden/complete";

    private static CharacterData reload(CharacterData data) {
        return CharacterData.CODEC.codec().parse(JsonOps.INSTANCE,
                CharacterData.CODEC.codec().encodeStart(JsonOps.INSTANCE, data)
                        .result().orElseThrow()).result().orElseThrow();
    }

    @Test
    void combatAndQuestCreditsComposeExactlyOnce() {
        // A real credited hostile kill passes through CharacterService.addExperience.
        CharacterData afterKill = new CharacterData().withExperience(
                ProgressionRules.experienceAfterReward(0, 25));
        Result first = CharacterRewardRules.grantOnce(afterKill, FORD, 75);
        assertEquals(Status.GRANTED, first.status());
        assertEquals(75, first.creditedExperience());
        assertEquals(100, first.character().experience());
        assertEquals(2, first.character().level());
        assertEquals(2, first.character().availablePoints());
        Result duplicate = CharacterRewardRules.grantOnce(first.character(), FORD, 100);
        assertEquals(Status.ALREADY_GRANTED, duplicate.status());
        assertSame(first.character(), duplicate.character());
        assertEquals(0, duplicate.creditedExperience());
        CharacterData anotherKill = first.character().withExperience(
                ProgressionRules.experienceAfterReward(first.character().experience(), 25));
        assertEquals(125, anotherKill.experience());
        assertTrue(anotherKill.hasClaimedReward(FORD));
        assertEquals(Status.ALREADY_GRANTED, CharacterRewardRules.grantOnce(
                reload(anotherKill), FORD, 75).status());

        Result bonus = CharacterRewardRules.grantOnce(afterKill, FORD, 100);
        assertEquals(125, bonus.character().experience());
        assertEquals(2, bonus.character().level());
    }

    @Test
    void crashWindowsWithPersistedPlayerSnapshotsAreRecoverable() {
        CharacterData before = new CharacterData().withExperience(25);
        // Crash BEFORE character commit: the pending quest can be retried once.
        CharacterData restoredBefore = reload(before);
        Result retryBefore = CharacterRewardRules.grantOnce(restoredBefore, FORD, 75);
        assertEquals(Status.GRANTED, retryBefore.status());
        assertEquals(100, retryBefore.character().experience());

        // Crash AFTER character commit but BEFORE quest stage REWARDED:
        // persisted receipt prevents a second payment; caller may finish quest stage.
        CharacterData restoredAfter = reload(retryBefore.character());
        assertEquals(Status.ALREADY_GRANTED, CharacterRewardRules.grantOnce(
                restoredAfter, FORD, 75).status());
        assertEquals(100, restoredAfter.experience());
        assertEquals(1, restoredAfter.claimedRewardIds().size());
    }

    @Test
    void preservesV3AllocatedBuildAndMigratesWithoutFabricatingReceipts() {
        JsonObject old = new JsonObject();
        old.addProperty("schema_version", 3);
        old.addProperty("experience", 300);
        old.addProperty("strength", 12);
        old.addProperty("willpower", 15);
        old.addProperty("earned_points", 4);
        JsonArray spent = new JsonArray();
        for (int x : new int[]{2, 0, 0, 0, 0, 0, 0}) spent.add(x);
        old.add("allocated_points", spent);
        CharacterData migrated = CharacterData.CODEC.codec()
                .parse(JsonOps.INSTANCE, old).result().orElseThrow();
        assertEquals(4, migrated.schemaVersion());
        assertEquals(300, migrated.experience());
        assertEquals(12, migrated.strength());
        assertEquals(15, migrated.willpower());
        assertEquals(4, migrated.earnedPoints());
        assertEquals(2, migrated.spentPoints());
        assertEquals(2, migrated.availablePoints());
        assertTrue(migrated.claimedRewardIds().isEmpty());
        assertEquals(migrated, reload(migrated));
        CharacterData claimed = CharacterRewardRules.grantOnce(migrated, FORD, 75).character();
        assertEquals(migrated.allocatedPoints(), claimed.allocatedPoints());
        assertEquals(375, claimed.experience());
    }

    @Test
    void legacyExplicitV2MigrationPreservesAllSevenStatsAndExperience() {
        JsonObject old = new JsonObject();
        old.addProperty("schema_version", 2);
        old.addProperty("experience", 4500);
        old.addProperty("strength", 21);
        old.addProperty("dexterity", 11);
        old.addProperty("vitality", 19);
        old.addProperty("endurance", 13);
        old.addProperty("intelligence", 14);
        old.addProperty("willpower", 18);
        old.addProperty("perception", 16);
        CharacterData migrated = CharacterData.CODEC.codec()
                .parse(JsonOps.INSTANCE, old).result().orElseThrow();
        assertEquals(4, migrated.schemaVersion());
        assertEquals(4500, migrated.experience());
        assertEquals(21, migrated.strength());
        assertEquals(11, migrated.dexterity());
        assertEquals(19, migrated.vitality());
        assertEquals(13, migrated.endurance());
        assertEquals(14, migrated.intelligence());
        assertEquals(18, migrated.willpower());
        assertEquals(16, migrated.perception());
        assertEquals(18, migrated.earnedPoints());
        assertEquals(0, migrated.spentPoints());
        assertTrue(migrated.claimedRewardIds().isEmpty());
        assertEquals(migrated, reload(migrated));
    }

    @Test
    void futureSchemaFailsClosedAndCorruptReceiptsAreNotSilentlyDiscarded() {
        CharacterData future = new CharacterData(5, 300,
                10, 10, 10, 10, 10, 10, 10, 4,
                List.of(0,0,0,0,0,0,0), List.of(FORD));
        assertEquals(5, future.schemaVersion());
        assertThrows(IllegalStateException.class, () -> future.withExperience(500));
        assertThrows(IllegalStateException.class,
                () -> future.withAttribute(
                        com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute.STRENGTH, 15));
        assertEquals(Status.INVALID_REQUEST,
                CharacterRewardRules.grantOnce(future, "mkm:another", 50).status());
        assertThrows(IllegalArgumentException.class, () -> new CharacterData(
                4, 300, 10, 10, 10, 10, 10, 10, 10, 4,
                List.of(0,0,0,0,0,0,0), List.of(FORD, FORD)));
        assertThrows(IllegalArgumentException.class, () -> new CharacterData(
                4, 300, 10, 10, 10, 10, 10, 10, 10, 4,
                List.of(0,0,0,0,0,0,0), List.of("not namespaced")));
    }

    @Test
    void doesNotDropReceiptsOnAttributeOrXpOperations() {
        CharacterData granted = CharacterRewardRules.grantOnce(
                new CharacterData().withExperience(100), FORD, 75).character();
        CharacterData allocated = AllocationRules.tryAllocate(granted,
                new int[] {1, 1, 0, 0, 0, 0, 0});
        assertNotNull(allocated);
        assertTrue(allocated.hasClaimedReward(FORD));
        assertTrue(allocated.withExperience(0).withExperience(100).hasClaimedReward(FORD));
        assertThrows(UnsupportedOperationException.class,
                () -> allocated.claimedRewardIds().add("mkm:forgery"));
        assertEquals(allocated, reload(allocated));
    }

    @Test
    void rewardIdValidationAndSizeCannotBeEvaded() {
        for (String malformed : new String[] {"", "No:Capital", "mkm:", ":foo",
                "mkm:/foo", "mkm:../foo", "mkm:x/../y", "mkm:x//y", "foo",
                "mkm:has spaces", "mkm:x/.", "mkm:x/..",
                "mkm:" + "x".repeat(100)}) {
            assertFalse(CharacterRewardRules.validRewardId(malformed), malformed);
            assertEquals(Status.INVALID_REQUEST,
                    CharacterRewardRules.grantOnce(new CharacterData(), malformed, 75).status());
        }
        assertFalse(CharacterRewardRules.validRewardId(null));
        assertTrue(CharacterRewardRules.validRewardId("mkm:ford_warden/complete"));
        assertEquals(Status.INVALID_REQUEST, CharacterRewardRules.grantOnce(
                new CharacterData(), FORD, -1).status());
        assertEquals(Status.INVALID_REQUEST, CharacterRewardRules.grantOnce(
                new CharacterData(), FORD, 0).status());
        assertEquals(Status.INVALID_REQUEST, CharacterRewardRules.grantOnce(
                new CharacterData(), FORD, Long.MAX_VALUE).status());
    }

    @Test
    void capStillRecordsRedemptionAndPreventsDelayedDuplicate() {
        CharacterData max = new CharacterData().withExperience(
                ProgressionRules.experienceForLevel(100));
        Result r = CharacterRewardRules.grantOnce(max, FORD, 75);
        assertEquals(Status.GRANTED, r.status());
        assertEquals(0, r.creditedExperience());
        assertTrue(r.character().hasClaimedReward(FORD));
        assertEquals(Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(reload(r.character()), FORD, 75).status());
    }

    @Test
    void fullLedgerFailsClosedButNeverEvictsPreviouslyClaimedIds() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < CharacterRewardRules.MAX_CLAIMED_REWARDS; i++) {
            ids.add("mkm:q/" + i);
        }
        CharacterData full = new CharacterData(4, 0, 10, 10, 10, 10, 10, 10, 10,
                0, List.of(0,0,0,0,0,0,0), ids);
        assertEquals(Status.LEDGER_FULL,
                CharacterRewardRules.grantOnce(full, "mkm:q/new", 100).status());
        assertEquals(Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(full, "mkm:q/0", 100).status());
        assertEquals(0, full.experience());
    }

    @Test
    void existingSnapshotChangesWithAllocatedStatsWithoutMagicDuplication() {
        CharacterData initial = new CharacterData().withExperience(6000);
        CharacterData built = AllocationRules.tryAllocate(initial,
                new int[]{4, 0, 4, 4, 4, 4, 0});
        assertNotNull(built);
        PlayerModifierSnapshot a = PlayerModifierCalculator.calculate(initial);
        PlayerModifierSnapshot b = PlayerModifierCalculator.calculate(built);
        for (PlayerModifierId id : new PlayerModifierId[]{
                PlayerModifierId.PHYSICAL_POWER, PlayerModifierId.MELEE_DAMAGE,
                PlayerModifierId.PHYSICAL_RESISTANCE, PlayerModifierId.MAX_HEALTH,
                PlayerModifierId.MAX_STAMINA, PlayerModifierId.STAMINA_REGENERATION,
                PlayerModifierId.MAGIC_POWER, PlayerModifierId.MAX_MANA,
                PlayerModifierId.MANA_REGENERATION, PlayerModifierId.HEALING_POWER,
                PlayerModifierId.HEALING_RECEIVED, PlayerModifierId.MAGIC_RESISTANCE}) {
            assertTrue(Double.isFinite(b.get(id)), id.name());
            assertTrue(b.get(id) >= a.get(id), id.name());
        }
        assertTrue(b.get(PlayerModifierId.HEALING_POWER) > a.get(PlayerModifierId.HEALING_POWER));
        assertTrue(b.get(PlayerModifierId.MAX_MANA) > a.get(PlayerModifierId.MAX_MANA));
        assertTrue(b.get(PlayerModifierId.MAX_STAMINA) > a.get(PlayerModifierId.MAX_STAMINA));
    }
}
