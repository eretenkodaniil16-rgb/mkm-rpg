package com.eretenkodaniil.mkm.character;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.CharacterRewardRules.Status;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class CharacterOwnerSyncTest {
    private static final String REWARD = "mkm:ford_warden/complete";

    @Test
    void ownerSyncContainsGameplayProgressButNeverReceiptHistory() {
        CharacterData granted = CharacterRewardRules.grantOnce(
                new CharacterData().withExperience(300), REWARD, 75).character();
        CharacterData allocated = AllocationRules.tryAllocate(
                granted, new int[] {1, 0, 1, 0, 0, 1, 0}, granted.spentPoints());
        assertNotNull(allocated);

        RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            CharacterData.STREAM_CODEC.encode(wire, allocated);
            assertTrue(wire.readableBytes() < 96, "receipt list must not inflate owner sync");
            CharacterData client = CharacterData.STREAM_CODEC.decode(wire);
            assertEquals(0, wire.readableBytes());
            assertEquals(allocated.schemaVersion(), client.schemaVersion());
            assertEquals(allocated.experience(), client.experience());
            assertEquals(allocated.earnedPoints(), client.earnedPoints());
            assertEquals(allocated.allocatedPoints(), client.allocatedPoints());
            assertEquals(allocated.strength(), client.strength());
            assertEquals(allocated.vitality(), client.vitality());
            assertEquals(allocated.willpower(), client.willpower());
            assertTrue(client.claimedRewardIds().isEmpty(),
                    "client must never receive server-side reward receipt keys");
            assertTrue(allocated.hasClaimedReward(REWARD),
                    "server persisted object must retain the paid receipt");
        } finally {
            wire.release();
        }
    }

    @Test
    void ownerSyncDoesNotProvideAnAuthoritativeRewardRecord() {
        CharacterData server = CharacterRewardRules.grantOnce(new CharacterData(), REWARD, 100)
                .character();
        RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            CharacterData.STREAM_CODEC.encode(wire, server);
            CharacterData displayed = CharacterData.STREAM_CODEC.decode(wire);
            assertFalse(displayed.hasClaimedReward(REWARD));
            assertEquals(Status.ALREADY_GRANTED,
                    CharacterRewardRules.grantOnce(server, REWARD, 100).status(),
                    "authoritative redemption uses server attachment, never the client mirror");
        } finally {
            wire.release();
        }
    }
}
