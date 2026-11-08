package com.eretenkodaniil.mkm.showcase.net;

import com.eretenkodaniil.mkm.showcase.ShowcaseService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class ShowcaseNetworking {
    private ShowcaseNetworking() {}
    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(ShowcaseChoiceRequest.TYPE,
                ShowcaseChoiceRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ShowcaseDecisionView.TYPE,
                ShowcaseDecisionView.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ShowcaseChoiceRequest.TYPE,
                (packet,ctx) -> ShowcaseService.onChoice(ctx.player(),packet));
    }
    public static void show(ServerPlayer player, ShowcaseDecisionView view) {
        if (ServerPlayNetworking.canSend(player,ShowcaseDecisionView.TYPE))
            ServerPlayNetworking.send(player,view);
    }
    public static boolean canShow(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player,ShowcaseDecisionView.TYPE);
    }
}
