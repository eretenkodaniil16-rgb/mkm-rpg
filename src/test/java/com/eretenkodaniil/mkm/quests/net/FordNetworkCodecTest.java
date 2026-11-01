package com.eretenkodaniil.mkm.quests.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real packet-codec round trips, including malicious but parseable stage/choice bytes. */
final class FordNetworkCodecTest {
    @Test void choiceIntentCarriesOnlyEntityStageAndAction() {
        for (int stage=-128; stage<=127; stage+=13) {
            ByteBuf bytes=Unpooled.buffer();
            try {
                var input=new FordChoiceRequest(12345,(byte)stage,(byte)127);
                FordChoiceRequest.CODEC.encode(bytes,input);
                assertEquals(6,bytes.readableBytes());
                assertEquals(input,FordChoiceRequest.CODEC.decode(bytes));
            } finally { bytes.release(); }
        }
    }

    @Test void ownerJournalRequestAndSnapshotRoundtrip() {
        ByteBuf request=Unpooled.buffer();
        ByteBuf response=Unpooled.buffer();
        try {
            var q=new FordJournalRequest((byte)1);
            FordJournalRequest.CODEC.encode(request,q);
            assertEquals(q,FordJournalRequest.CODEC.decode(request));
            for (int stage=0;stage<=4;stage++) {
                var view=new FordJournalView((byte)stage,true,stage==1);
                FordJournalView.CODEC.encode(response,view);
                assertEquals(view,FordJournalView.CODEC.decode(response));
            }
        } finally { request.release(); response.release(); }
    }

    @Test void npcDialogueViewRoundtripIncludesServerPerception() {
        ByteBuf bytes=Unpooled.buffer();
        try {
            var view=new FordDialogueView(732,(byte)0,false,12);
            FordDialogueView.CODEC.encode(bytes,view);
            assertEquals(view,FordDialogueView.CODEC.decode(bytes));
        } finally { bytes.release(); }
    }
}
