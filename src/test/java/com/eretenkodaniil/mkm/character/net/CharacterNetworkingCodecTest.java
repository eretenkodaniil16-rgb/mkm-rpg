package com.eretenkodaniil.mkm.character.net;

import static org.junit.jupiter.api.Assertions.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

final class CharacterNetworkingCodecTest {
    @Test
    void allocationIntentRoundTripsWithFixedSevenCounters() {
        ByteBuf buf = Unpooled.buffer();
        try {
            AllocationRequest outgoing = new AllocationRequest(
                    42, 18, 0, 1, 2, 3, 4, 5, 30);
            AllocationRequest.STREAM_CODEC.encode(buf, outgoing);
            assertEquals(12, buf.readableBytes(), "four-byte ID + varint revision + seven stats");
            AllocationRequest actual = AllocationRequest.STREAM_CODEC.decode(buf);
            assertEquals(outgoing, actual);
            assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5, 30}, actual.totals());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void wireCountersRejectNegativeOrOverflowInsteadOfWrapping() {
        ByteBuf buf = Unpooled.buffer();
        try {
            assertThrows(IllegalArgumentException.class, () -> AllocationRequest.STREAM_CODEC.encode(
                    buf, new AllocationRequest(1, 0, -1, 0, 0, 0, 0, 0, 0)));
            assertThrows(IllegalArgumentException.class, () -> AllocationRequest.STREAM_CODEC.encode(
                    buf, new AllocationRequest(1, 0, 256, 0, 0, 0, 0, 0, 0)));
        } finally {
            buf.release();
        }
    }

    @Test
    void serverRepliesCorrelateExactly() {
        ByteBuf buf = Unpooled.buffer();
        try {
            AllocationResult.STREAM_CODEC.encode(buf, new AllocationResult(123456, false));
            assertEquals(new AllocationResult(123456, false), AllocationResult.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }
}
