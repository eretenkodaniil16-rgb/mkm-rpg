package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.client.gui.ShowcaseDecisionScreen;
import com.eretenkodaniil.mkm.showcase.net.ShowcaseDecisionView;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Cosmetic UI only, never writes quest attachments or grants XP client-side. */
public final class ShowcaseClientNetworking {
    private ShowcaseClientNetworking() {}
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(ShowcaseDecisionView.TYPE,
                (payload,ctx)->ctx.client().execute(()->{
                    if (ctx.client().player==null) return;
                    var current=ctx.client().gui.screen();
                    // A delayed response must not replace inventory or another mod's dialog.
                    if (current==null || current instanceof ShowcaseDecisionScreen)
                        ctx.client().gui.setScreen(new ShowcaseDecisionScreen(payload));
                }));
    }
}
