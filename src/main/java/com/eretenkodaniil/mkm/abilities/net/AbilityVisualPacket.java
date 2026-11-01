package com.eretenkodaniil.mkm.abilities.net;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualEvent;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server-to-owner cosmetic confirmation only. No network-supplied outcomes. */
public record AbilityVisualPacket(AbilityVisualEvent event) implements CustomPacketPayload {
    public static final Type<AbilityVisualPacket> TYPE =
            new Type<>(MkmMod.id("ability_visual_event"));

    public static final StreamCodec<ByteBuf, AbilityVisualPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public AbilityVisualPacket decode(ByteBuf buf) {
            return new AbilityVisualPacket(AbilityVisualPacketCodec.decode(buf));
        }
        @Override public void encode(ByteBuf buf, AbilityVisualPacket value) {
            AbilityVisualPacketCodec.encode(buf, value.event());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
