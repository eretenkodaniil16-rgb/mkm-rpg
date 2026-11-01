package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.client.gui.FordDialogueScreen;
import com.eretenkodaniil.mkm.client.gui.FordJournalScreen;
import com.eretenkodaniil.mkm.quests.net.FordDialogueView;
import com.eretenkodaniil.mkm.quests.net.FordJournalView;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-only view handling; never grants XP, changes quest stage or creates entities. */
public final class FordClientNetworking {
    private FordClientNetworking() {}
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(FordDialogueView.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().player != null)
                        context.client().gui.setScreen(new FordDialogueScreen(payload));
                }));
        ClientPlayNetworking.registerGlobalReceiver(FordJournalView.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().player != null)
                        context.client().gui.setScreen(new FordJournalScreen(payload));
                }));
    }
}
