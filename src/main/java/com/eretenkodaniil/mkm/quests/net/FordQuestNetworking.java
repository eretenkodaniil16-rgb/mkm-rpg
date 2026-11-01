package com.eretenkodaniil.mkm.quests.net;

import com.eretenkodaniil.mkm.quests.FordQuestService;
import com.eretenkodaniil.mkm.quests.FordQuestState;
import com.eretenkodaniil.mkm.character.CharacterService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;

/** Mod networking registration on the logical server. No client classes imported. */
public final class FordQuestNetworking {
    private FordQuestNetworking() {}
    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(FordChoiceRequest.TYPE, FordChoiceRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FordJournalRequest.TYPE, FordJournalRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FordDialogueView.TYPE, FordDialogueView.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FordJournalView.TYPE, FordJournalView.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FordChoiceRequest.TYPE,
                (packet, context) -> FordQuestService.handleChoice(context.player(), packet));
        ServerPlayNetworking.registerGlobalReceiver(FordJournalRequest.TYPE, (packet, context) -> {
            if (packet.version() == 1) sendJournal(context.player());
        });
    }

    public static byte stage(FordQuestState state) {
        if (state.schemaVersion() != FordQuestState.SCHEMA_VERSION) return 4;
        return switch (state.stage()) {
            case FordQuestState.NOT_STARTED -> 0;
            case FordQuestState.ACTIVE -> 1;
            case FordQuestState.RETURN_TO_WARDEN -> 2;
            case FordQuestState.REWARDED -> 3;
            default -> 4;
        };
    }

    public static boolean showDialogue(ServerPlayer player, Villager npc) {
        if (!ServerPlayNetworking.canSend(player, FordDialogueView.TYPE)) return false;
        var quest = FordQuestService.get(player);
        ServerPlayNetworking.send(player, new FordDialogueView(npc.getId(), stage(quest),
                quest.foundTracks(), CharacterService.get(player).perception()));
        return true;
    }

    public static void sendJournal(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, FordJournalView.TYPE)) return;
        var quest = FordQuestService.get(player);
        ServerPlayNetworking.send(player, new FordJournalView(stage(quest), quest.foundTracks(),
                FordQuestService.isEncounterLoaded(player)));
    }
}
