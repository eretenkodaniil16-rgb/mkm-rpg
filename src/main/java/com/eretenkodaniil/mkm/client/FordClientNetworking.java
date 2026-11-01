package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.client.gui.FordDialogueScreen;
import com.eretenkodaniil.mkm.client.gui.FordJournalScreen;
import com.eretenkodaniil.mkm.quests.net.FordDialogueView;
import com.eretenkodaniil.mkm.quests.net.FordJournalRequest;
import com.eretenkodaniil.mkm.quests.net.FordJournalView;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Player-local UI snapshots; never modifies quest stage, awards, combat or animation. */
public final class FordClientNetworking {
    private static boolean journalRequested;

    private FordClientNetworking() {}

    public static void requestJournal() {
        if (!ClientPlayNetworking.canSend(FordJournalRequest.TYPE)) return;
        journalRequested = true;
        ClientPlayNetworking.send(new FordJournalRequest((byte) 1));
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(FordDialogueView.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().player == null) return;
                    // A late NPC packet must never dismiss an unrelated inventory/menu.
                    var active = context.client().gui.screen();
                    if (active == null || active instanceof FordDialogueScreen) {
                        context.client().gui.setScreen(new FordDialogueScreen(payload));
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(FordJournalView.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().player == null) {
                        journalRequested = false;
                        return;
                    }
                    var active = context.client().gui.screen();
                    if (active instanceof FordJournalScreen) {
                        context.client().gui.setScreen(new FordJournalScreen(payload));
                    } else if (journalRequested && active == null) {
                        context.client().gui.setScreen(new FordJournalScreen(payload));
                    }
                    journalRequested = false;
                }));
    }
}
