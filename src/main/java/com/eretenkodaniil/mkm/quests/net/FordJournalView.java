package com.eretenkodaniil.mkm.quests.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server-derived journal; only owner receives their quest details. */
public record FordJournalView(byte stage, boolean tracks, boolean encounterLoaded)
        implements CustomPacketPayload {
    public static final Type<FordJournalView> TYPE = new Type<>(MkmMod.id("ford_journal_view"));
    public static final StreamCodec<ByteBuf, FordJournalView> CODEC = new StreamCodec<>() {
        @Override public FordJournalView decode(ByteBuf buf) {
            return new FordJournalView(buf.readByte(), buf.readBoolean(), buf.readBoolean());
        }
        @Override public void encode(ByteBuf buf, FordJournalView value) {
            buf.writeByte(value.stage());
            buf.writeBoolean(value.tracks());
            buf.writeBoolean(value.encounterLoaded());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
