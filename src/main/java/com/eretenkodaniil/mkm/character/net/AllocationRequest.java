package com.eretenkodaniil.mkm.character.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Seven bounded, absolute spent-point counters in canonical PrimaryAttribute enum order.
 * No XP, earned points, target player or derived stats are accepted from the client.
 */
public record AllocationRequest(int str, int dex, int vit, int end, int intelligence, int wil, int per)
        implements CustomPacketPayload {
    public static final Type<AllocationRequest> TYPE = new Type<>(MkmMod.id("allocate_attributes"));
    public static final StreamCodec<ByteBuf, AllocationRequest> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AllocationRequest decode(ByteBuf buf) {
            return new AllocationRequest(buf.readUnsignedByte(), buf.readUnsignedByte(),
                    buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(),
                    buf.readUnsignedByte(), buf.readUnsignedByte());
        }

        @Override
        public void encode(ByteBuf buf, AllocationRequest packet) {
            for (int count : packet.totals()) buf.writeByte(count);
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
