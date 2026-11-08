package com.eretenkodaniil.mkm.character.origin.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Untrusted class-action intent. Actor/class/effect/cost/target are NEVER client inputs. */
public record OriginClassActionRequest(byte intent) implements CustomPacketPayload {
    public static final Type<OriginClassActionRequest> TYPE =
            new Type<>(MkmMod.id("origin_class_action"));
    public static final StreamCodec<ByteBuf, OriginClassActionRequest> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public OriginClassActionRequest decode(ByteBuf b) {
                    return new OriginClassActionRequest(b.readByte());
                }
                @Override public void encode(ByteBuf b, OriginClassActionRequest p) {
                    b.writeByte(p.intent());
                }
            };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
