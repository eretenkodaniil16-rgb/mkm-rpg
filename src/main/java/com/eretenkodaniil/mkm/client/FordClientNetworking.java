package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.client.gui.FordDialogueScreen;
import com.eretenkodaniil.mkm.client.gui.FordJournalScreen;
import com.eretenkodaniil.mkm.quests.net.FordDialogueView;
import com.eretenkodaniil.mkm.quests.net.FordJournalRequest;
import com.eretenkodaniil.mkm.quests.net.FordJournalView;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Player-local UI snapshots; never modifies quest stage, awards, combat or animation. */
public final class FordClientNetworking {
    private static int awaitedJournalRequestId;
    private static int nextJournalRequestId = 1;

    private FordClientNetworking() {}

    public static void requestJournal() {
        if (!ClientPlayNetworking.canSend(FordJournalRequest.TYPE)) return;
        int id = nextJournalRequestId++;
        if (id <= 0) {
            nextJournalRequestId = 2;
            id = 1;
        }
        awaitedJournalRequestId = id;
        ClientPlayNetworking.send(new FordJournalRequest((byte) 1, id));
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
                        awaitedJournalRequestId = 0;
                        return;
                    }
                    // Only responses matching an explicit J press may open a closed UI.
                    // Push snapshots (requestId=0) update an already visible journal.
                    if (payload.requestId() != 0
                            && payload.requestId() != awaitedJournalRequestId) return;
                    var active = context.client().gui.screen();
                    if (active instanceof FordJournalScreen) {
                        context.client().gui.setScreen(new FordJournalScreen(payload));
                    } else if (payload.requestId() > 0 && active == null) {
                        context.client().gui.setScreen(new FordJournalScreen(payload));
                    }
                    if (payload.requestId() > 0) awaitedJournalRequestId = 0;
                }));
    }
}
