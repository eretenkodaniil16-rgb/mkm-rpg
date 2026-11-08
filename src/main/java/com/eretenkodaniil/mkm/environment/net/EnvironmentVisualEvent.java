package com.eretenkodaniil.mkm.environment.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server-originated cosmetic signal; never accepted client-to-server. */
public record EnvironmentVisualEvent(int entityId, byte cue) implements CustomPacketPayload {
    public static final Type<EnvironmentVisualEvent> TYPE =
            new Type<>(MkmMod.id("environment_visual_event"));
    public static final StreamCodec<ByteBuf, EnvironmentVisualEvent> CODEC = new StreamCodec<>() {
        @Override public EnvironmentVisualEvent decode(ByteBuf buf) {
            return new EnvironmentVisualEvent(buf.readInt(), buf.readByte());
        }
        @Override public void encode(ByteBuf buf, EnvironmentVisualEvent event) {
            buf.writeInt(event.entityId());
            buf.writeByte(event.cue());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
