package com.eretenkodaniil.mkm.combat.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Untrusted client intent. Only the direction (not displacement or stamina) crosses the wire.
 * 0 forward, 1 back, 2 left, 3 right, 4-7 diagonals.
 */
public record DodgeRequest(byte direction) implements CustomPacketPayload {
    public static final Type<DodgeRequest> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(MkmMod.MOD_ID, "dodge"));
    public static final StreamCodec<ByteBuf, DodgeRequest> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DodgeRequest decode(ByteBuf buf) {
            return new DodgeRequest(buf.readByte());
        }

        @Override
        public void encode(ByteBuf buf, DodgeRequest request) {
            buf.writeByte(request.direction());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
