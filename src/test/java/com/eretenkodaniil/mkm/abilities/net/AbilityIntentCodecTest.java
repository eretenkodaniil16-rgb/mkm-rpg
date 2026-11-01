package com.eretenkodaniil.mkm.abilities.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityIntentCodecTest {
    @Test void trustedAbilityIdRoundTripsAsBoundedAscii() {
        ByteBuf packet = Unpooled.buffer();
        try {
            AbilityIntentCodec.encode(packet, "mkm:renewal");
            assertEquals("mkm:renewal", AbilityIntentCodec.decode(packet));
            assertEquals(0, packet.readableBytes());
        } finally {
            packet.release();
        }
    }

    @Test void senderRejectsForeignIdAndNonAsciiStrings() {
        ByteBuf packet = Unpooled.buffer();
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> AbilityIntentCodec.encode(packet, "other:renewal"));
            assertThrows(IllegalArgumentException.class,
                    () -> AbilityIntentCodec.encode(packet, "mkm:исцеление"));
            assertThrows(IllegalArgumentException.class,
                    () -> AbilityIntentCodec.encode(packet, null));
            assertThrows(IllegalArgumentException.class,
                    () -> AbilityIntentCodec.encode(packet, "mkm:" + "x".repeat(55)));
            assertEquals(0, packet.writerIndex());
        } finally {
            packet.release();
        }
    }

    @Test void receiverRejectsOversizeEmptyTruncatedAndNonAsciiPackets() {
        ByteBuf packet = Unpooled.buffer();
        try {
            packet.writeByte(0);
            assertThrows(IllegalArgumentException.class,
                    () -> AbilityIntentCodec.decode(packet));
            packet.clear().writeByte(65);
            assertThrows(IllegalArgumentException.class,
                    () -> AbilityIntentCodec.decode(packet));
            packet.clear().writeByte(5).writeBytes(new byte[]{'m', 'k'});
            assertThrows(IndexOutOfBoundsException.class,
                    () -> AbilityIntentCodec.decode(packet));
            packet.clear().writeByte(1).writeByte(255);
            assertThrows(IllegalArgumentException.class,
                    () -> AbilityIntentCodec.decode(packet));
        } finally {
            packet.release();
        }
    }

    @Test void receiverAllowsUnknownWellFormedIdForServerAllowlistToReject() {
        ByteBuf packet = Unpooled.buffer();
        try {
            AbilityIntentCodec.encode(packet, "mkm:unknown");
            assertEquals("mkm:unknown", AbilityIntentCodec.decode(packet));
        } finally {
            packet.release();
        }
    }
}
