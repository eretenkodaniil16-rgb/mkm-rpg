package com.eretenkodaniil.mkm.quests.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Bounded, server-authored client dialogue snapshot. No narrative state is client-owned. */
public record FordDialogueView(int entityId, byte stage, boolean tracks, int perception, long sessionId)
        implements CustomPacketPayload {
    public static final Type<FordDialogueView> TYPE = new Type<>(MkmMod.id("ford_dialogue_view"));
    public static final StreamCodec<ByteBuf, FordDialogueView> CODEC = new StreamCodec<>() {
        @Override public FordDialogueView decode(ByteBuf buf) {
            return new FordDialogueView(buf.readInt(), buf.readByte(), buf.readBoolean(), buf.readInt(), buf.readLong());
        }
        @Override public void encode(ByteBuf buf, FordDialogueView value) {
            buf.writeInt(value.entityId());
            buf.writeByte(value.stage());
            buf.writeBoolean(value.tracks());
            buf.writeInt(value.perception());
            buf.writeLong(value.sessionId());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
