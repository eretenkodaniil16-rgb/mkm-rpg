package com.eretenkodaniil.mkm.character;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
import com.eretenkodaniil.mkm.character.modifier.CharacterModifierAccess;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Golden v3 fixture schema copied from the CharacterData codec actually packaged by
 * integration PR #30 (0.0.5-fabric.4). Fixtures are synthetic, not player data.
 * Tests defend upgrades to M2/M3 schema v4 without re-running reward payment.
 */
final class Fabric054CharacterV3MigrationTest {
    private static final String PATH = "/migration/character_v3_0_0_5_fabric_4_";
    private static final String FORD = "mkm:ford_warden/complete";

    private static JsonObject oldRecord(String variant) {
        var stream = Fabric054CharacterV3MigrationTest.class.getResourceAsStream(PATH + variant + ".json");
        assertNotNull(stream, "Missing v3 golden record: " + variant);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CharacterData decode(JsonObject source) {
        return CharacterData.CODEC.codec().parse(JsonOps.INSTANCE, source)
                .result().orElseThrow();
    }

    private static CharacterData saveReload(CharacterData data) {
        var codec = CharacterData.CODEC.codec();
        return codec.parse(JsonOps.INSTANCE, codec.encodeStart(JsonOps.INSTANCE, data)
                .result().orElseThrow()).result().orElseThrow();
    }

    private static void assertUnchangedV3Fields(JsonObject v3, CharacterData v4) {
        assertEquals(3, v3.get("schema_version").getAsInt());
        assertEquals(4, v4.schemaVersion());
        assertEquals(v3.get("experience").getAsLong(), v4.experience());
        assertEquals(v3.get("strength").getAsInt(), v4.strength());
        assertEquals(v3.get("dexterity").getAsInt(), v4.dexterity());
        assertEquals(v3.get("vitality").getAsInt(), v4.vitality());
        assertEquals(v3.get("endurance").getAsInt(), v4.endurance());
        assertEquals(v3.get("intelligence").getAsInt(), v4.intelligence());
        assertEquals(v3.get("willpower").getAsInt(), v4.willpower());
        assertEquals(v3.get("perception").getAsInt(), v4.perception());
        assertEquals(v3.get("earned_points").getAsInt(), v4.earnedPoints());
        for (int i = 0; i < CharacterData.ATTRIBUTE_COUNT; i++) {
            assertEquals(v3.getAsJsonArray("allocated_points").get(i).getAsInt(),
                    v4.allocatedPoints().get(i), "Allocation index " + i);
        }
        int spent = v3.getAsJsonArray("allocated_points").asList().stream()
                .mapToInt(value -> value.getAsInt()).sum();
        assertEquals(spent, v4.spentPoints());
        assertEquals(v4.earnedPoints() - spent, v4.availablePoints());
        assertTrue(v4.claimedRewardIds().isEmpty(),
                "The v3 codec did not store receipt history: migration cannot fabricate it");
        assertEquals(v4, saveReload(v4), "v4 must be stable after second restart");
    }

    @Test
    void real054SchemaWithFourteenPurchasedPointsSurvivesUpgradeAndRelog() {
        JsonObject v3 = oldRecord("allocated");
        CharacterData upgraded = decode(v3);
        assertUnchangedV3Fields(v3, upgraded);
        assertEquals(14, upgraded.spentPoints());
        assertEquals(2, upgraded.availablePoints());
        assertEquals(9, upgraded.level());

        // The first v4 allocation should use exactly the remaining credits,
        // not reset progress or re-mint a new credit balance.
        CharacterData updated = AllocationRules.tryAllocate(
                upgraded, new int[]{3, 4, 1, 0, 2, 3, 2}, 14);
        assertNotNull(updated);
        assertEquals(15, updated.spentPoints());
        assertEquals(1, updated.availablePoints());
        assertEquals(12, updated.perception());
        assertNull(AllocationRules.tryAllocate(updated,
                new int[]{3, 4, 1, 0, 2, 3, 2}, 14), "stale v3 allocation replay");
        assertEquals(updated, saveReload(updated));

        var first = CharacterRewardRules.grantOnce(updated, FORD, 100);
        assertEquals(CharacterRewardRules.Status.GRANTED, first.status());
        assertEquals(4100L, first.character().experience());
        assertEquals(updated.spentPoints(), first.character().spentPoints());
        CharacterData afterRestart = saveReload(first.character());
        assertEquals(CharacterRewardRules.Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(afterRestart, FORD, 100).status());
        assertEquals(4100L, afterRestart.experience());
        assertEquals(1, afterRestart.availablePoints());
    }

    @Test
    void levelCapAndHistoricalExcessXpRemainIntact() {
        JsonObject v3 = oldRecord("max_level");
        CharacterData upgraded = decode(v3);
        assertUnchangedV3Fields(v3, upgraded);
        assertEquals(650000L, upgraded.experience(), "old higher-than-cap XP must survive");
        assertEquals(100, upgraded.level());
        assertEquals(198, upgraded.earnedPoints());
        assertEquals(138, upgraded.spentPoints());
        assertEquals(60, upgraded.availablePoints());
        var claim = CharacterRewardRules.grantOnce(upgraded, FORD, 75);
        assertEquals(CharacterRewardRules.Status.GRANTED, claim.status());
        assertEquals(0L, claim.creditedExperience());
        assertEquals(650000L, claim.character().experience());
        assertTrue(saveReload(claim.character()).hasClaimedReward(FORD));
    }

    @Test
    void oldAdminModifiedStatAboveGameplayCapMustNotBeSilentlyClampedToForty() {
        JsonObject v3 = oldRecord("legacy_admin");
        CharacterData migrated = decode(v3);
        assertUnchangedV3Fields(v3, migrated);
        assertEquals(63, migrated.strength());
        assertEquals(2, migrated.spentPoints());
        assertEquals(2, migrated.availablePoints());
        assertNull(AllocationRules.tryAllocate(migrated,
                new int[]{1, 2, 0, 0, 0, 0, 0}, 2),
                "legacy admin STR=63 is preserved but cannot be incremented by normal allocation");
        CharacterData elsewhere = AllocationRules.tryAllocate(migrated,
                new int[]{0, 2, 1, 0, 0, 0, 0}, 2);
        assertNotNull(elsewhere);
        assertEquals(63, elsewhere.strength());
    }

    @Test
    void derivedEquipmentCompositionSameBeforeAndAfterV4SaveAndFurtherReward() {
        CharacterData upgraded = decode(oldRecord("allocated"));
        Map<PlayerModifierId, Double> equipped = Map.of(
                PlayerModifierId.PHYSICAL_POWER, 0.08D,
                PlayerModifierId.MELEE_DAMAGE, 0.04D,
                PlayerModifierId.MAX_STAMINA, 13.0D,
                PlayerModifierId.PHYSICAL_RESISTANCE, 0.02D);
        var before = PlayerModifierCalculator.calculate(upgraded, equipped);
        var after = CharacterModifierAccess.withContributions(saveReload(upgraded), equipped);
        assertEquals(before.all(), after.all());
        var claimed = CharacterRewardRules.grantOnce(upgraded, FORD, 75).character();
        assertEquals(before.all(), CharacterModifierAccess.withContributions(
                saveReload(claimed), equipped).all(),
                "XP/receipt alone cannot change combat stats when allocated attributes are unchanged");
        // Equipment is authoritative in vanilla inventory; no item IDs are stored in CharacterData.
        JsonObject saved = CharacterData.CODEC.codec().encodeStart(
                JsonOps.INSTANCE, claimed).result().orElseThrow().getAsJsonObject();
        assertFalse(saved.has("equipment"));
        assertFalse(saved.has("inventory"));
        assertEquals(PrimaryAttribute.values().length, upgraded.allocatedPoints().size());
    }

    @Test
    void corruptedV3AllocationLedgerFailsClosedInsteadOfResetingPurchasedCredits() {
        JsonObject v3 = oldRecord("allocated");
        v3.remove("allocated_points");
        assertThrows(IllegalArgumentException.class,
                () -> CharacterData.CODEC.codec().parse(JsonOps.INSTANCE, v3),
                "missing v3 ledger fails closed via constructor exception, not DataResult.error");
        JsonObject v3WrongLength = oldRecord("allocated");
        v3WrongLength.getAsJsonArray("allocated_points").remove(0);
        assertThrows(IllegalArgumentException.class,
                () -> CharacterData.CODEC.codec().parse(JsonOps.INSTANCE, v3WrongLength),
                "short v3 ledger must never be silently rewritten as zero spent points");
    }

    @Test
    void unsupportedFutureSaveMustNotAllowClaimOrSpend() {
        JsonObject future = oldRecord("allocated");
        future.addProperty("schema_version", 5);
        CharacterData loaded = decode(future);
        assertEquals(5, loaded.schemaVersion());
        assertEquals(CharacterRewardRules.Status.INVALID_REQUEST,
                CharacterRewardRules.grantOnce(loaded, FORD, 75).status());
        assertNull(AllocationRules.tryAllocate(loaded,
                new int[]{3,4,1,0,2,3,2},14));
        assertThrows(IllegalStateException.class, () -> loaded.withExperience(5000));
    }
}
