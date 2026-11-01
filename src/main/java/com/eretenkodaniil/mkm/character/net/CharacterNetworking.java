package com.eretenkodaniil.mkm.character.net;

import com.eretenkodaniil.mkm.character.CharacterService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Strictly server-authoritative allocation; reply is correlated with the requesting UI. */
public final class CharacterNetworking {
    private CharacterNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(AllocationRequest.TYPE, AllocationRequest.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AllocationResult.TYPE, AllocationResult.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AllocationRequest.TYPE, (payload, context) -> {
            boolean accepted = CharacterService.allocate(
                    context.player(), payload.totals(), payload.expectedSpent());
            ServerPlayNetworking.send(context.player(), new AllocationResult(payload.requestId(), accepted));
        });
    }
}
