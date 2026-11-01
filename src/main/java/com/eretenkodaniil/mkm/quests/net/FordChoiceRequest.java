package com.eretenkodaniil.mkm.quests.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Intent only: exact clicked NPC, expected stage, allowlisted choice ID. */
public record FordChoiceRequest(int entityId, byte expectedStage, byte choice)
        implements CustomPacketPayload {
    public static final byte INSPECT = 1, ACCEPT = 2, LEAVE = 3, CLAIM = 4, LOCATE = 5;
    public static final Type<FordChoiceRequest> TYPE = new Type<>(MkmMod.id("ford_choice"));
    public static final StreamCodec<ByteBuf, FordChoiceRequest> CODEC = new StreamCodec<>() {
        @Override public FordChoiceRequest decode(ByteBuf buf) {
            return new FordChoiceRequest(buf.readInt(), buf.readByte(), buf.readByte());
        }
        @Override public void encode(ByteBuf buf, FordChoiceRequest value) {
            buf.writeInt(value.entityId());
            buf.writeByte(value.expectedStage());
            buf.writeByte(value.choice());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
