package com.eretenkodaniil.mkm.quests.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Serverbound player-local journal read request; no owner UUID or stage supplied by client. */
public record FordJournalRequest(byte version, int requestId) implements CustomPacketPayload {
    public static final Type<FordJournalRequest> TYPE = new Type<>(MkmMod.id("ford_journal_request"));
    public static final StreamCodec<ByteBuf, FordJournalRequest> CODEC = new StreamCodec<>() {
        @Override public FordJournalRequest decode(ByteBuf buf) { return new FordJournalRequest(buf.readByte(),buf.readInt()); }
        @Override public void encode(ByteBuf buf, FordJournalRequest value) { buf.writeByte(value.version());
            buf.writeInt(value.requestId()); }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
