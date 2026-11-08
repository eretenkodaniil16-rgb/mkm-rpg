package com.eretenkodaniil.mkm.abilities.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CastAbilityRequestV2Test {
    @Test void strictV2RoundTripContainsNoClientGameplayOutcome() {
        ByteBuf buf = Unpooled.buffer();
        try {
            CastAbilityRequest request = new CastAbilityRequest("mkm:renewal", 42);
            CastAbilityRequest.STREAM_CODEC.encode(buf, request);
            assertTrue(buf.readableBytes() <= CastAbilityRequest.MAX_WIRE_BYTES);
            assertEquals(request, CastAbilityRequest.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());
        } finally { buf.release(); }
    }

    @Test void oldIdOnlyProtocolAndUnknownVersionsAreRejected() {
        ByteBuf old = Unpooled.buffer();
        ByteBuf invalid = Unpooled.buffer();
        try {
            AbilityIntentCodec.encode(old, "mkm:renewal"); // M3 C2S v1
            assertThrows(IllegalArgumentException.class,
                    () -> CastAbilityRequest.STREAM_CODEC.decode(old));
            CastAbilityRequest.STREAM_CODEC.encode(invalid,
                    new CastAbilityRequest("mkm:renewal", 1));
            invalid.setByte(0, 99);
            assertThrows(IllegalArgumentException.class,
                    () -> CastAbilityRequest.STREAM_CODEC.decode(invalid));
        } finally {
            old.release();
            invalid.release();
        }
    }

    @Test void sequenceMustBePositiveAndPacketExactlyBounded() {
        assertThrows(IllegalArgumentException.class,
                () -> new CastAbilityRequest("mkm:renewal", 0));
        ByteBuf forged = Unpooled.buffer();
        ByteBuf excessive = Unpooled.buffer();
        ByteBuf truncated = Unpooled.buffer();
        try {
            CastAbilityRequest.STREAM_CODEC.encode(forged,
                    new CastAbilityRequest("mkm:renewal", 1));
            forged.setInt(forged.writerIndex() - 4, -1);
            assertThrows(IllegalArgumentException.class,
                    () -> CastAbilityRequest.STREAM_CODEC.decode(forged));

            CastAbilityRequest.STREAM_CODEC.encode(excessive,
                    new CastAbilityRequest("mkm:renewal", 2));
            excessive.writeByte(1);
            assertThrows(IllegalArgumentException.class,
                    () -> CastAbilityRequest.STREAM_CODEC.decode(excessive));

            CastAbilityRequest.STREAM_CODEC.encode(truncated,
                    new CastAbilityRequest("mkm:renewal", 3));
            truncated.writerIndex(truncated.writerIndex() - 2);
            assertThrows(IndexOutOfBoundsException.class,
                    () -> CastAbilityRequest.STREAM_CODEC.decode(truncated));
        } finally {
            forged.release();
            excessive.release();
            truncated.release();
        }
    }
}
