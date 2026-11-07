package com.eretenkodaniil.mkm.character.origin.net;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.character.origin.OriginIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Untrusted intent contains only bounded IDs, expected revision and UI correlation token. */
public record OriginSelectionRequest(int requestId, int expectedRevision,
                                     String raceId, String classId) implements CustomPacketPayload {
    public static final Type<OriginSelectionRequest> TYPE = new Type<>(MkmMod.id("select_origin"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OriginSelectionRequest> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public OriginSelectionRequest decode(RegistryFriendlyByteBuf buf) {
                    return new OriginSelectionRequest(buf.readVarInt(), buf.readVarInt(),
                            buf.readUtf(OriginIds.MAX_ID_LENGTH), buf.readUtf(OriginIds.MAX_ID_LENGTH));
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, OriginSelectionRequest packet) {
                    buf.writeVarInt(packet.requestId);
                    buf.writeVarInt(packet.expectedRevision);
                    buf.writeUtf(packet.raceId, OriginIds.MAX_ID_LENGTH);
                    buf.writeUtf(packet.classId, OriginIds.MAX_ID_LENGTH);
                }
            };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
