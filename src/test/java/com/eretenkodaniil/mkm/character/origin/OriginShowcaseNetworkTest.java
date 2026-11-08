package com.eretenkodaniil.mkm.character.origin;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.origin.net.OriginClassActionRequest;
import com.eretenkodaniil.mkm.quests.net.FordChoiceRequest;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

/** The client sends only an intent, never actor UUID, class, reward, damage or cost. */
final class OriginShowcaseNetworkTest {
    @Test void classActionPacketIsExactlyOneByteAndForgedOpcodeCannotExecute() {
        var data = Unpooled.buffer();
        try {
            OriginClassActionRequest.STREAM_CODEC.encode(data, new OriginClassActionRequest((byte) 1));
            assertEquals(1, data.readableBytes());
            assertEquals(new OriginClassActionRequest((byte) 1),
                    OriginClassActionRequest.STREAM_CODEC.decode(data));
            assertEquals(0, data.readableBytes());
        } finally { data.release(); }
        // Intent validation happens before resolving actor state.
        assertFalse(OriginClassActionService.execute(null, (byte) -1));
        assertFalse(OriginClassActionService.execute(null, (byte) 2));
        assertFalse(OriginClassActionService.execute(null, (byte) 1));
    }

    @Test void culturalDialogueChoicesStillUseOriginalSessionBoundedPacketCodec() {
        for (byte choice : new byte[]{
                FordChoiceRequest.HUMAN_PARLEY,
                FordChoiceRequest.DWARF_RUNES,
                FordChoiceRequest.MENTOR_TRAIN}) {
            var data = Unpooled.buffer();
            try {
                var request = new FordChoiceRequest(73, (byte) 1, choice, 0x0123456700000009L);
                FordChoiceRequest.CODEC.encode(data, request);
                assertEquals(14, data.readableBytes(),
                        "same NPC ID/stage/choice/nonce wire size; no actor spoofing or added XP");
                assertEquals(request, FordChoiceRequest.CODEC.decode(data));
                assertEquals(0, data.readableBytes());
            } finally { data.release(); }
        }
    }
}
