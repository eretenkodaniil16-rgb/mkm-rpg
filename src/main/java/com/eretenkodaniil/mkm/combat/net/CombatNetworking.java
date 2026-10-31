package com.eretenkodaniil.mkm.combat.net;

import com.eretenkodaniil.mkm.combat.CombatService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative combat input registration (mod event bus only). */
public final class CombatNetworking {
    private CombatNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                DodgeRequest.TYPE, DodgeRequest.STREAM_CODEC, CombatNetworking::onDodge);
    }

    private static void onDodge(DodgeRequest request, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            CombatService.tryDodge(player, request.direction());
        }
    }
}
