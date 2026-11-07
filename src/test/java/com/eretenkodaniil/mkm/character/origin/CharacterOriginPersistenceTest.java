package com.eretenkodaniil.mkm.character.origin;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.AllocationRules;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterRewardRules;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class CharacterOriginPersistenceTest {
    private static CharacterOriginData disk(CharacterOriginData data) {
        var codec = CharacterOriginData.CODEC;
        return codec.parse(JsonOps.INSTANCE, codec.encodeStart(JsonOps.INSTANCE, data)
                .result().orElseThrow()).result().orElseThrow();
    }

    @Test void separateAttachmentLeavesExistingV4StatsXpAllocationsAndReceiptsUntouched() {
        CharacterData character = new CharacterData().withExperience(300);
        character = AllocationRules.tryAllocate(character, new int[]{2, 0, 0, 0, 0, 2, 0});
        assertNotNull(character);
        character = CharacterRewardRules.grantOnce(
                character, "mkm:ford_warden/complete", 100).character();
        CharacterData original = character;

        // Old v4 players had no origin attachment. It is not necessary to rewrite v4.
        CharacterOriginData freshProfile = new CharacterOriginData();
        var chosen = CharacterOriginRules.select(freshProfile, "mkm:dwarf", "mkm:mage", 0);
        assertEquals(CharacterOriginRules.Outcome.SELECTED, chosen.outcome());
        assertEquals(original, character);
        assertEquals(400, character.experience());
        assertEquals(4, character.spentPoints());
        assertEquals(0, character.availablePoints(),
                "race selection must not mint or refund any build credits");
        assertTrue(character.hasClaimedReward("mkm:ford_warden/complete"));
        assertEquals(chosen.data(), disk(chosen.data()));
        assertEquals(original, CharacterData.CODEC.codec().parse(JsonOps.INSTANCE,
                CharacterData.CODEC.codec().encodeStart(JsonOps.INSTANCE, original)
                        .result().orElseThrow()).result().orElseThrow());
    }

    @Test void oldV4SaveWithoutProfileOpensAsUnselectedWithoutResettingCredits() {
        JsonObject previous = new JsonObject();
        previous.addProperty("schema_version", 4);
        previous.addProperty("experience", 300);
        previous.addProperty("strength", 12);
        previous.addProperty("earned_points", 4);
        var credits = new com.google.gson.JsonArray();
        for (int v : new int[]{2, 0, 0, 0, 0, 0, 0}) credits.add(v);
        previous.add("allocated_points", credits);
        var receipts = new com.google.gson.JsonArray();
        receipts.add("mkm:ford_warden/complete");
        previous.add("claimed_reward_ids", receipts);
        CharacterData loaded = CharacterData.CODEC.codec().parse(JsonOps.INSTANCE, previous)
                .result().orElseThrow();
        assertEquals(4, loaded.schemaVersion());
        assertEquals(12, loaded.strength());
        assertEquals(2, loaded.availablePoints());
        assertEquals(300, loaded.experience());
        assertTrue(loaded.hasClaimedReward("mkm:ford_warden/complete"));
        assertFalse(new CharacterOriginData().selected());
    }

    @Test void ownerStreamTransmitsOnlyProfileStateAndRoundTripsAfterRespawnCopySimulation() {
        CharacterOriginData selected = CharacterOriginRules.select(
                new CharacterOriginData(), "mkm:human", "mkm:warrior", 0).data();
        RegistryFriendlyByteBuf wire =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            CharacterOriginData.STREAM_CODEC.encode(wire, selected);
            assertTrue(wire.readableBytes() < 150);
            CharacterOriginData mirror = CharacterOriginData.STREAM_CODEC.decode(wire);
            assertEquals(selected, mirror);
            assertEquals(0, wire.readableBytes());
            assertEquals(selected, disk(disk(selected)), "relog/death copy must preserve ID pair");
            // Full death event copying still requires interactive Fabric test.
        } finally {
            wire.release();
        }
    }

    @Test void corruptHalfSelectionAndFutureDataCannotBeSilentlyRecreated() {
        assertThrows(IllegalArgumentException.class, () ->
                new CharacterOriginData(1, 1, "mkm:human", ""));
        assertThrows(IllegalArgumentException.class, () ->
                new CharacterOriginData(1, 0, "", "mkm:mage"));
        assertThrows(IllegalArgumentException.class, () ->
                new CharacterOriginData(1, 1, "mkm:human", "mkm:warrior/../bad"));
        assertEquals(2, disk(new CharacterOriginData(2, 1, "mkm:human", "mkm:mage")).schemaVersion());
        assertEquals(new CharacterOriginData(2, 2, "mkm:dwarf", "mkm:warrior"),
                disk(new CharacterOriginData(2, 2, "mkm:dwarf", "mkm:warrior")),
                "future schema revisions must be preserved, and never overwritten by M4");
    }
}
