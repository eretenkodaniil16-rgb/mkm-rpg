package com.eretenkodaniil.mkm.character.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Reply to exactly one allocation intent; authoritative values arrive via attachment sync. */
public record AllocationResult(int requestId, boolean accepted) implements CustomPacketPayload {
    public static final Type<AllocationResult> TYPE = new Type<>(MkmMod.id("allocation_result"));

    public static final StreamCodec<ByteBuf, AllocationResult> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AllocationResult decode(ByteBuf buf) {
            return new AllocationResult(buf.readInt(), buf.readBoolean());
        }

        @Override
        public void encode(ByteBuf buf, AllocationResult result) {
            buf.writeInt(result.requestId());
            buf.writeBoolean(result.accepted());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
