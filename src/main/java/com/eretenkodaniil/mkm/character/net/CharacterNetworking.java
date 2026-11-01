package com.eretenkodaniil.mkm.character.net;

import com.eretenkodaniil.mkm.character.CharacterService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** A single narrow, server-validated build intent packet. */
public final class CharacterNetworking {
    private CharacterNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(AllocationRequest.TYPE, AllocationRequest.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AllocationRequest.TYPE,
                (payload, context) -> CharacterService.allocate(context.player(), payload.totals()));
    }
}
