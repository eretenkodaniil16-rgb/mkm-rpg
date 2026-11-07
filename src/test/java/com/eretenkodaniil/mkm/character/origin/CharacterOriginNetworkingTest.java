package com.eretenkodaniil.mkm.character.origin;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.origin.net.OriginSelectionRequest;
import com.eretenkodaniil.mkm.character.origin.net.OriginSelectionResult;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

/** Netty wire format test: no character UUID/XP/attribute/ability lists in client request. */
final class CharacterOriginNetworkingTest {
    @Test void allFourIntentsAndCorrelatedRepliesSurviveCodecRoundTrip() {
        int id = 1;
        for (var race : OriginCatalog.racesAll().values()) {
            for (var clazz : OriginCatalog.classesAll().values()) {
                var request = new OriginSelectionRequest(id++, 0, race.id(), clazz.id());
                RegistryFriendlyByteBuf packet = new RegistryFriendlyByteBuf(
                        Unpooled.buffer(), RegistryAccess.EMPTY);
                try {
                    OriginSelectionRequest.STREAM_CODEC.encode(packet, request);
                    assertTrue(packet.readableBytes() < 145);
                    assertEquals(request, OriginSelectionRequest.STREAM_CODEC.decode(packet));
                    assertEquals(0, packet.readableBytes());
                } finally { packet.release(); }
            }
        }
        for (var outcome : CharacterOriginRules.Outcome.values()) {
            RegistryFriendlyByteBuf packet = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                var result = new OriginSelectionResult(71, outcome);
                OriginSelectionResult.STREAM_CODEC.encode(packet, result);
                assertEquals(result, OriginSelectionResult.STREAM_CODEC.decode(packet));
                assertEquals(0, packet.readableBytes());
            } finally { packet.release(); }
        }
    }

    @Test void invalidReplyCodeRejectsInsteadOfWrappingIntoSelected() {
        RegistryFriendlyByteBuf packet = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            packet.writeVarInt(1);
            packet.writeByte(255);
            assertThrows(IllegalArgumentException.class, () ->
                    OriginSelectionResult.STREAM_CODEC.decode(packet));
        } finally { packet.release(); }
    }

    @Test void largeOrMissingRaceValueCannotPassValidServerRules() {
        CharacterOriginData initial = new CharacterOriginData();
        assertEquals(CharacterOriginRules.Outcome.INVALID,
                CharacterOriginRules.select(initial, "mkm:human", "", 0).outcome());
        assertEquals(CharacterOriginRules.Outcome.STALE,
                CharacterOriginRules.select(initial, "mkm:human", "mkm:mage", -1).outcome());
    }
}
