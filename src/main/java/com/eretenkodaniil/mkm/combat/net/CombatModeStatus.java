package com.eretenkodaniil.mkm.combat.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server ACK; the client NEVER intercepts vanilla LMB until enabled=true. */
public record CombatModeStatus(boolean enabled) implements CustomPacketPayload {
    public static final Type<CombatModeStatus> TYPE = new Type<>(MkmMod.id("combat_mode_status"));
    public static final StreamCodec<ByteBuf, CombatModeStatus> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public CombatModeStatus decode(ByteBuf buf) {
                    return new CombatModeStatus(buf.readBoolean());
                }
                @Override public void encode(ByteBuf buf, CombatModeStatus value) {
                    buf.writeBoolean(value.enabled());
                }
            };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
