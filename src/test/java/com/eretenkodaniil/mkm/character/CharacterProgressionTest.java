package com.eretenkodaniil.mkm.character;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CharacterProgressionTest {
    @Test
    void quadraticThresholdsAndBoundaries() {
        assertEquals(0, ProgressionRules.experienceForLevel(1));
        assertEquals(100, ProgressionRules.experienceForLevel(2));
        assertEquals(300, ProgressionRules.experienceForLevel(3));
        assertEquals(495000, ProgressionRules.experienceForLevel(100));
        assertEquals(1, ProgressionRules.levelForExperience(-1));
        assertEquals(1, ProgressionRules.levelForExperience(99));
        assertEquals(2, ProgressionRules.levelForExperience(100));
        assertEquals(2, ProgressionRules.levelForExperience(299));
        assertEquals(3, ProgressionRules.levelForExperience(300));
        assertEquals(100, ProgressionRules.levelForExperience(Long.MAX_VALUE));
        assertEquals(198, ProgressionRules.earnedPointsForLevel(100));
        for (int level = 2; level <= ProgressionRules.MAX_LEVEL; level++) {
            assertTrue(ProgressionRules.experienceForLevel(level)
                    > ProgressionRules.experienceForLevel(level - 1));
        }
    }

    @Test
    void xpGrantsCreditsExactlyOnceEvenAfterAdministrativeDecrease() {
        CharacterData novice = new CharacterData();
        assertEquals(0, novice.availablePoints());
        CharacterData levelThree = novice.withExperience(300);
        assertEquals(3, levelThree.level());
        assertEquals(4, levelThree.earnedPoints());
        assertEquals(4, levelThree.availablePoints());
        assertEquals(4, levelThree.withExperience(0).withExperience(300).earnedPoints());
        assertEquals(198, levelThree.withExperience(Long.MAX_VALUE).earnedPoints());
    }

    @Test
    void migrateLegacySavesIncludingMissingSchemaMarker() {
        JsonObject oldSave = new JsonObject();
        oldSave.addProperty("experience", 300L);
        oldSave.addProperty("strength", 27);
        CharacterData migrated = CharacterData.CODEC.codec()
                .parse(JsonOps.INSTANCE, oldSave).result().orElseThrow();
        assertEquals(3, migrated.schemaVersion());
        assertEquals(27, migrated.strength());
        assertEquals(10, migrated.perception());
        assertEquals(4, migrated.earnedPoints());
        assertEquals(0, migrated.spentPoints());
        assertEquals(4, migrated.availablePoints());
        CharacterData saved = CharacterData.CODEC.codec()
                .parse(JsonOps.INSTANCE, CharacterData.CODEC.codec()
                        .encodeStart(JsonOps.INSTANCE, migrated).result().orElseThrow())
                .result().orElseThrow();
        assertEquals(migrated, saved);
    }

    @Test
    void spendUpdatesExistingStatsAndDerivedModifiers() {
        CharacterData original = new CharacterData().withExperience(300);
        int[] request = {2, 0, 0, 0, 2, 0, 0};
        CharacterData spent = AllocationRules.tryAllocate(original, request);
        assertNotNull(spent);
        assertEquals(12, spent.strength());
        assertEquals(12, spent.intelligence());
        assertEquals(0, spent.availablePoints());
        assertEquals(4, spent.spentPoints());
        assertEquals(10, original.strength());
        assertTrue(PlayerModifierCalculator.calculate(spent).get(PlayerModifierId.PHYSICAL_POWER)
                > PlayerModifierCalculator.calculate(original).get(PlayerModifierId.PHYSICAL_POWER));
        assertTrue(PlayerModifierCalculator.calculate(spent).get(PlayerModifierId.MAGIC_POWER)
                > PlayerModifierCalculator.calculate(original).get(PlayerModifierId.MAGIC_POWER));
        assertNull(AllocationRules.tryAllocate(spent, request), "packet replay may not spend twice");
    }

    @Test
    void hostileRequestsFailWithoutMutatingState() {
        CharacterData data = new CharacterData().withExperience(100);
        assertNull(AllocationRules.tryAllocate(data, new int[] {3, 0, 0, 0, 0, 0, 0}));
        assertNull(AllocationRules.tryAllocate(data, new int[] {-1, 0, 0, 0, 0, 0, 0}));
        assertNull(AllocationRules.tryAllocate(data, new int[] {1, 0}));
        assertNull(AllocationRules.tryAllocate(data, new int[] {99, 0, 0, 0, 0, 0, 0}));
        assertNull(AllocationRules.tryAllocate(data, null));
        CharacterData spent = AllocationRules.tryAllocate(data, new int[] {1, 0, 0, 0, 0, 0, 0});
        assertNotNull(spent);
        assertNull(AllocationRules.tryAllocate(spent, new int[] {0, 0, 0, 0, 0, 0, 0}),
                "rollback is forbidden without an explicit respec transaction");
    }

    @Test
    void gameplayCapDoesNotSilentlyClampLegacyAttributes() {
        CharacterData legacy = new CharacterData(2, 100, 63, 10, 10, 10, 10, 10, 10, 0, List.of());
        assertEquals(63, legacy.strength());
        assertEquals(3, legacy.schemaVersion());
        assertNull(AllocationRules.tryAllocate(legacy, new int[] {1, 0, 0, 0, 0, 0, 0}));
        assertNotNull(AllocationRules.tryAllocate(legacy, new int[] {0, 1, 0, 0, 0, 0, 0}));
    }

    @Test
    void futureSchemaAndOverspentLedgerFailClosed() {
        CharacterData future = new CharacterData(4, 300, 10, 10, 10, 10, 10, 10, 10,
                4, List.of(0, 0, 0, 0, 0, 0, 0));
        assertNull(AllocationRules.tryAllocate(future, new int[] {1, 0, 0, 0, 0, 0, 0}));
        CharacterData broken = new CharacterData(3, 100, 10, 10, 10, 10, 10, 10, 10,
                2, List.of(3, 0, 0, 0, 0, 0, 0));
        assertNull(AllocationRules.tryAllocate(broken, new int[] {4, 0, 0, 0, 0, 0, 0}));
    }

    @Test
    void allocationLedgerIsImmutableAndSurvivesCodecRoundTrip() {
        CharacterData purchased = AllocationRules.tryAllocate(
                new CharacterData().withExperience(100),
                new int[] {0, 0, 1, 0, 0, 1, 0});
        assertNotNull(purchased);
        assertThrows(UnsupportedOperationException.class,
                () -> purchased.allocatedPoints().set(0, 30));
        CharacterData restored = CharacterData.CODEC.codec()
                .parse(JsonOps.INSTANCE, CharacterData.CODEC.codec()
                        .encodeStart(JsonOps.INSTANCE, purchased).result().orElseThrow())
                .result().orElseThrow();
        assertEquals(purchased, restored);
        assertEquals(0, restored.availablePoints());
    }
}
