package com.eretenkodaniil.mkm.combat.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Intent to begin the next combo strike. No target, damage, position,
 * duration, attack speed, or stamina is taken from the client.
 */
public record ComboRequest(byte kind) implements CustomPacketPayload {
    public static final Type<ComboRequest> TYPE = new Type<>(MkmMod.id("combo"));
    public static final StreamCodec<ByteBuf, ComboRequest> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ComboRequest decode(ByteBuf buf) {
            return new ComboRequest(buf.readByte());
        }

        @Override
        public void encode(ByteBuf buf, ComboRequest value) {
            buf.writeByte(value.kind());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
