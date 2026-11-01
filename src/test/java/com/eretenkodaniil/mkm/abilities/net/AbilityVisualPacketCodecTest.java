package com.eretenkodaniil.mkm.abilities.net;

import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualEvent;
import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualPolicy;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityVisualPacketCodecTest {
    private static AbilityVisualEvent event() {
        return new AbilityVisualEvent("mkm:renewal",
                AbilityVisualPolicy.RENEWAL_ACTION_ID,
                11, 3, AbilityVisualEvent.Hand.MAIN_HAND,
                AbilityVisualEvent.Phase.APPLIED, 12000L,
                AbilityVisualPolicy.RENEWAL_VISUAL_TICKS);
    }

    @Test void fullProtocolRoundTripAndSmallBoundedPayload() {
        ByteBuf buf = Unpooled.buffer();
        try {
            AbilityVisualPacketCodec.encode(buf, event());
            assertTrue(buf.readableBytes() < 160);
            assertEquals(event(), AbilityVisualPacketCodec.decode(buf));
            assertEquals(0, buf.readableBytes());
        } finally { buf.release(); }
    }

    @Test void rejectUnknownVersion() {
        ByteBuf buf = Unpooled.buffer();
        try {
            AbilityVisualPacketCodec.encode(buf, event());
            buf.setByte(0, 99);
            assertThrows(IllegalArgumentException.class,
                    () -> AbilityVisualPacketCodec.decode(buf));
        } finally { buf.release(); }
    }

    @Test void rejectInvalidPhaseAndHandFromUntrustedWire() {
        ByteBuf buf = Unpooled.buffer();
        try {
            AbilityVisualPacketCodec.encode(buf, event());
            int enumIndex = 1 + 1 + "mkm:renewal".length()
                    + 1 + AbilityVisualPolicy.RENEWAL_ACTION_ID.length() + 4 + 4;
            buf.setByte(enumIndex, 127);
            assertThrows(IllegalArgumentException.class,
                    () -> AbilityVisualPacketCodec.decode(buf));
            buf.readerIndex(0);
            buf.setByte(enumIndex, AbilityVisualEvent.Hand.MAIN_HAND.ordinal());
            buf.setByte(enumIndex + 1, 127);
            assertThrows(IllegalArgumentException.class,
                    () -> AbilityVisualPacketCodec.decode(buf));
        } finally { buf.release(); }
    }

    @Test void rejectTruncationAndNegativeDuration() {
        ByteBuf buf = Unpooled.buffer();
        try {
            AbilityVisualPacketCodec.encode(buf, event());
            buf.writerIndex(buf.writerIndex() - 4);
            assertThrows(IndexOutOfBoundsException.class,
                    () -> AbilityVisualPacketCodec.decode(buf));
            buf.clear();
            AbilityVisualPacketCodec.encode(buf, event());
            buf.setInt(buf.writerIndex() - 4, -1);
            assertThrows(IllegalArgumentException.class,
                    () -> AbilityVisualPacketCodec.decode(buf));
        } finally { buf.release(); }
    }

    @Test void rejectMalformedAbilityIdentifier() {
        ByteBuf buf = Unpooled.buffer();
        try {
            AbilityVisualPacketCodec.encode(buf, event());
            buf.setByte(2, '!');
            assertThrows(IllegalArgumentException.class,
                    () -> AbilityVisualPacketCodec.decode(buf));
        } finally { buf.release(); }
    }
}
