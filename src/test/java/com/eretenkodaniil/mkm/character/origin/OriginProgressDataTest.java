package com.eretenkodaniil.mkm.character.origin;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.List;

final class OriginProgressDataTest {
    private static OriginProgressData disk(OriginProgressData d) {
        return OriginProgressData.CODEC.parse(JsonOps.INSTANCE,
                OriginProgressData.CODEC.encodeStart(JsonOps.INSTANCE, d).result().orElseThrow())
                .result().orElseThrow();
    }
    @Test void missingOldAttachmentIsNeutralAndLearnsOnlyOneStableNode() {
        var old = new OriginProgressData();
        assertFalse(old.has(OriginProgressData.FORD_MENTORSHIP));
        var upgraded = old.unlock(OriginProgressData.FORD_MENTORSHIP);
        assertFalse(old.has(OriginProgressData.FORD_MENTORSHIP));
        assertTrue(disk(upgraded).has(OriginProgressData.FORD_MENTORSHIP));
        assertThrows(IllegalStateException.class,
                () -> upgraded.unlock(OriginProgressData.FORD_MENTORSHIP));
    }
    @Test void savedFutureIdAndFutureSchemaArePreservedButNotExecutable() {
        var unknownId = new OriginProgressData(1,
                List.of("mkm:future_culture/recovered"));
        assertEquals(unknownId, disk(unknownId));
        assertFalse(unknownId.has(OriginProgressData.FORD_MENTORSHIP));
        assertFalse(unknownId.has("mkm:future_culture/recovered"),
                "saved but unimplemented future IDs must never be executable");
        assertThrows(IllegalArgumentException.class, () ->
                new OriginProgressData().unlock("mkm:unregistered_skill"));
        var future = new OriginProgressData(2,
                List.of(OriginProgressData.FORD_MENTORSHIP));
        assertEquals(future, disk(future));
        assertFalse(future.has(OriginProgressData.FORD_MENTORSHIP));
        assertThrows(IllegalStateException.class,
                () -> future.unlock("mkm:extra"));
        assertThrows(IllegalArgumentException.class, () ->
                new OriginProgressData(1, List.of("mkm:a", "mkm:a")));
    }
    @Test void ownerSyncRoundTripDeathCopyEquivalentAndBoundedCount() {
        var original = new OriginProgressData().unlock(OriginProgressData.FORD_MENTORSHIP);
        RegistryFriendlyByteBuf b = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            OriginProgressData.STREAM_CODEC.encode(b, original);
            var parsed = OriginProgressData.STREAM_CODEC.decode(b);
            assertEquals(original, parsed);
            assertEquals(0, b.readableBytes());
            assertEquals(original, disk(disk(original)));
        } finally { b.release(); }
        RegistryFriendlyByteBuf forged = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            forged.writeVarInt(1);
            forged.writeVarInt(1000);
            assertThrows(IllegalArgumentException.class,
                    () -> OriginProgressData.STREAM_CODEC.decode(forged));
        } finally { forged.release(); }
    }
    @Test void schemaV4CharacterReceiptsRemainCompletelyUnmodified() {
        var character = new com.eretenkodaniil.mkm.character.CharacterData()
                .withExperience(325);
        var rewarded = com.eretenkodaniil.mkm.character.CharacterRewardRules.grantOnce(
                character, "mkm:ford_warden/complete", 75).character();
        var progress = new OriginProgressData().unlock(OriginProgressData.FORD_MENTORSHIP);
        assertEquals(4, rewarded.schemaVersion());
        assertEquals(400, rewarded.experience());
        assertTrue(rewarded.hasClaimedReward("mkm:ford_warden/complete"));
        assertEquals(progress, disk(progress));
        assertEquals(400, rewarded.experience());
    }
}
