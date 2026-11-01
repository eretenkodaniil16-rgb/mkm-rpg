package com.eretenkodaniil.mkm.combat.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-owning-client acknowledgement of positive, unblocked direct
 * melee damage from an MKM combo. This is NOT an attack request and carries
 * no user-controlled damage or target selector.
 */
public record HitConfirm(int targetEntityId, float damageTaken, boolean staggered)
        implements CustomPacketPayload {
    public static final Type<HitConfirm> TYPE = new Type<>(MkmMod.id("hit_confirm"));
    public static final StreamCodec<ByteBuf, HitConfirm> STREAM_CODEC = new StreamCodec<>() {
        @Override public HitConfirm decode(ByteBuf buf) {
            return new HitConfirm(buf.readInt(), buf.readFloat(), buf.readBoolean());
        }
        @Override public void encode(ByteBuf buf, HitConfirm payload) {
            buf.writeInt(payload.targetEntityId());
            buf.writeFloat(payload.damageTaken());
            buf.writeBoolean(payload.staggered());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
