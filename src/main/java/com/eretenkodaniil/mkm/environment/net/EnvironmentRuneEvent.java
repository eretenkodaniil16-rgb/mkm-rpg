package com.eretenkodaniil.mkm.environment.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 12-byte position payload. Server -> subscribed clients only. */
public record EnvironmentRuneEvent(int x, int y, int z) implements CustomPacketPayload {
    public static final Type<EnvironmentRuneEvent> TYPE = new Type<>(MkmMod.id("environment_rune_pulse"));
    public static final StreamCodec<ByteBuf, EnvironmentRuneEvent> CODEC = new StreamCodec<>() {
        @Override public EnvironmentRuneEvent decode(ByteBuf buf) {
            return new EnvironmentRuneEvent(buf.readInt(), buf.readInt(), buf.readInt());
        }
        @Override public void encode(ByteBuf buf, EnvironmentRuneEvent packet) {
            buf.writeInt(packet.x());
            buf.writeInt(packet.y());
            buf.writeInt(packet.z());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
