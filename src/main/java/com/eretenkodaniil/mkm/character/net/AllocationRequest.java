package com.eretenkodaniil.mkm.character.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Untrusted intent: canonical seven absolute allocation counters, optimistic expected-spent
 * revision, and a UI correlation ID. No authoritative balance or character ID is transmitted.
 */
public record AllocationRequest(
        int requestId, int expectedSpent,
        int str, int dex, int vit, int end, int intelligence, int wil, int per)
        implements CustomPacketPayload {

    public static final Type<AllocationRequest> TYPE = new Type<>(MkmMod.id("allocate_attributes"));
    public static final StreamCodec<ByteBuf, AllocationRequest> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AllocationRequest decode(ByteBuf buf) {
            return new AllocationRequest(buf.readInt(), buf.readVarInt(),
                    buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(),
                    buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(),
                    buf.readUnsignedByte());
        }

        @Override
        public void encode(ByteBuf buf, AllocationRequest packet) {
            buf.writeInt(packet.requestId());
            buf.writeVarInt(packet.expectedSpent());
            for (int count : packet.totals()) {
                if (count < 0 || count > 255) {
                    throw new IllegalArgumentException("Allocation counter out of wire range");
                }
                buf.writeByte(count);
            }
        }
    };

    public int[] totals() {
        return new int[] {str, dex, vit, end, intelligence, wil, per};
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
