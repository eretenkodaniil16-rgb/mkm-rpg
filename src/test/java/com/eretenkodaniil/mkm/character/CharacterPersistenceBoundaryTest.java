package com.eretenkodaniil.mkm.character;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.CharacterRewardRules.Status;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class CharacterPersistenceBoundaryTest {
    private static final String FIRST = "mkm:ford_warden/complete";
    private static final String SECOND = "mkm:world_e2/secondary";

    private static CharacterData roundTrip(CharacterData data) {
        var codec = CharacterData.CODEC.codec();
        return codec.parse(JsonOps.INSTANCE, codec.encodeStart(JsonOps.INSTANCE, data)
                .result().orElseThrow()).result().orElseThrow();
    }

    @Test
    void fullV4SaveReloadPreservesXpStatsAndMultipleReceipts() {
        CharacterData initial = new CharacterData().withExperience(300);
        CharacterData allocated = AllocationRules.tryAllocate(initial,
                new int[]{1, 0, 1, 0, 1, 0, 1});
        assertNotNull(allocated);
        var first = CharacterRewardRules.grantOnce(allocated, FIRST, 75);
        assertEquals(Status.GRANTED, first.status());
        var second = CharacterRewardRules.grantOnce(first.character(), SECOND, 100);
        assertEquals(Status.GRANTED, second.status());
        CharacterData restored = roundTrip(second.character());
        assertEquals(second.character(), restored);
        assertEquals(475, restored.experience());
        assertEquals(4, restored.spentPoints());
        assertEquals(0, restored.availablePoints());
        assertEquals(List.of(FIRST, SECOND), restored.claimedRewardIds());
        assertEquals(Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(restored, FIRST, 75).status());
        assertEquals(Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(restored, SECOND, 100).status());
    }

    @Test
    void legacyAbsentVersionMigratesWithoutCreatingClaimsOrAlteringStats() {
        JsonObject old = new JsonObject();
        old.addProperty("experience", 300);
        old.addProperty("strength", 25);
        old.addProperty("willpower", 19);
        CharacterData restored = CharacterData.CODEC.codec()
                .parse(JsonOps.INSTANCE, old).result().orElseThrow();
        assertEquals(4, restored.schemaVersion());
        assertEquals(300, restored.experience());
        assertEquals(25, restored.strength());
        assertEquals(19, restored.willpower());
        assertEquals(4, restored.earnedPoints());
        assertEquals(0, restored.spentPoints());
        assertTrue(restored.claimedRewardIds().isEmpty());
        assertEquals(restored, roundTrip(restored));
    }

    @Test
    void characterRecordAndDiskCodecCannotStoreAnimationFramesOrPoseState() {
        Set<String> fields = Arrays.stream(CharacterData.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("schemaVersion", "experience", "strength", "dexterity",
                "vitality", "endurance", "intelligence", "willpower", "perception",
                "earnedPoints", "allocatedPoints", "claimedRewardIds"), fields);
        JsonObject saved = CharacterData.CODEC.codec().encodeStart(
                JsonOps.INSTANCE, new CharacterData()).result().orElseThrow().getAsJsonObject();
        assertFalse(saved.has("animation"));
        assertFalse(saved.has("frames"));
        assertFalse(saved.has("pose"));
        assertFalse(saved.has("cosmetic_state"));
        assertFalse(saved.has("action_timeline"));
    }

    @Test
    void repeatedSavedCloneRetainsReceiptEvenAfterXpAdministrativeAdjustment() {
        CharacterData initial = CharacterRewardRules.grantOnce(
                new CharacterData().withExperience(300), FIRST, 100).character();
        CharacterData copyOnRespawn = roundTrip(initial);
        CharacterData relog = roundTrip(copyOnRespawn.withExperience(0));
        assertTrue(relog.hasClaimedReward(FIRST));
        assertEquals(4, relog.earnedPoints(),
                "Reducing XP must not refund or delete earned build credits");
        assertEquals(Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(relog, FIRST, 100).status());
        // This proves codec preservation, NOT that a real Fabric death event copies the attachment.
    }
}
