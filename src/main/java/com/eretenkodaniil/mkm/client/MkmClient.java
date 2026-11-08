package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.abilities.AbilityCatalog;
import com.eretenkodaniil.mkm.abilities.net.CastAbilityRequest;
import com.eretenkodaniil.mkm.character.net.AllocationResult;
import com.eretenkodaniil.mkm.character.origin.net.OriginSelectionResult;
import com.eretenkodaniil.mkm.client.gui.CharacterOriginScreen;
import com.eretenkodaniil.mkm.client.gui.CharacterSheetScreen;
import com.eretenkodaniil.mkm.client.gui.MkmActionPaletteScreen;
import com.eretenkodaniil.mkm.client.accessibility.MkmAccessibility;
import com.eretenkodaniil.mkm.client.input.MkmActionRouter;
import com.eretenkodaniil.mkm.client.input.ClientActionGate;
import com.eretenkodaniil.mkm.client.animation.HitFeedbackParticles;
import com.eretenkodaniil.mkm.combat.net.HitConfirm;
import com.eretenkodaniil.mkm.combat.net.DodgeRequest;
import com.eretenkodaniil.mkm.combat.net.ComboRequest;
import com.eretenkodaniil.mkm.combat.DodgeDirection;
import com.eretenkodaniil.mkm.registry.MkmItems;
import com.eretenkodaniil.mkm.quests.net.FordJournalRequest;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;

public final class MkmClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(MkmMod.id("general"));
    private static final KeyMapping OPEN_SHEET = new KeyMapping(
            "key.mkm.open_character_sheet", InputConstants.KEY_K, CATEGORY);
    private static final KeyMapping DODGE = new KeyMapping(
            "key.mkm.dodge", InputConstants.KEY_G, CATEGORY);
    private static final KeyMapping JOURNAL = new KeyMapping(
            "key.mkm.quest_journal", InputConstants.KEY_J, CATEGORY);
    private static final KeyMapping COMBO = new KeyMapping(
            "key.mkm.combo", InputConstants.KEY_R, CATEGORY);

    private static final KeyMapping ACTION_PALETTE = new KeyMapping(
            "key.mkm.action_palette", InputConstants.KEY_M, CATEGORY);

    static final KeyMapping RENEWAL = new KeyMapping(
            "key.mkm.cast_renewal", InputConstants.KEY_H, CATEGORY);

    @Override
    public void onInitializeClient() {
        MkmAccessibility.load();
        ClientPlayNetworking.registerGlobalReceiver(AllocationResult.TYPE, (result, context) ->
                context.client().execute(() -> {
                    if (context.client().gui.screen() instanceof CharacterSheetScreen sheet) {
                        sheet.onAllocationResult(result);
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(OriginSelectionResult.TYPE, (result, context) ->
                context.client().execute(() -> {
                    if (context.client().gui.screen() instanceof CharacterOriginScreen screen) {
                        screen.onSelectionResult(result);
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(HitConfirm.TYPE,
                (payload, context) -> context.client().execute(
                        () -> HitFeedbackParticles.onConfirmedHit(context.client(), payload)));
        KeyMappingHelper.registerKeyMapping(OPEN_SHEET);
        KeyMappingHelper.registerKeyMapping(DODGE);
        KeyMappingHelper.registerKeyMapping(COMBO);
        KeyMappingHelper.registerKeyMapping(JOURNAL);
        FordClientNetworking.initialize();
        KeyMappingHelper.registerKeyMapping(RENEWAL);
        KeyMappingHelper.registerKeyMapping(ACTION_PALETTE);
        HudElementRegistry.attachElementAfter(VanillaHudElements.INFO_BAR,
                MkmMod.id("rpg_hud"), MkmRpgHud::extract);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MkmActionRouter.tick(client);
            boolean sheet = false, journal = false, palette = false;
            boolean combo = false, dodge = false, renewal = false;
            // Drain accumulated clicks; at most ONE packet per action per tick.
            while (OPEN_SHEET.consumeClick()) sheet = true;
            while (JOURNAL.consumeClick()) journal = true;
            while (ACTION_PALETTE.consumeClick()) palette = true;
            while (COMBO.consumeClick()) combo = true;
            while (DODGE.consumeClick()) dodge = true;
            while (RENEWAL.consumeClick()) renewal = true;

            if (client.player == null || client.gui.screen() != null) return;
            if (sheet) { openCharacterSheet(); return; }
            if (journal && ClientPlayNetworking.canSend(FordJournalRequest.TYPE)) {
                FordClientNetworking.requestJournal();
                return;
            }
            if (palette || (combo && MkmAccessibility.get().simpleInput())) {
                client.gui.setScreen(new MkmActionPaletteScreen());
                return;
            }
            if (renewal) MkmActionRouter.request(client, ClientActionGate.Action.RENEWAL, false);
            if (combo) MkmActionRouter.request(client, ClientActionGate.Action.COMBO, false);
            if (dodge) MkmActionRouter.request(client, ClientActionGate.Action.DODGE, false);
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (player.getItemInHand(hand).getItem() != MkmItems.CODEX) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) {
                openCharacterSheet();
            }
            return InteractionResult.SUCCESS;
        });
    }

    private static void openCharacterSheet() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.gui.setScreen(new CharacterSheetScreen());
        }
    }
}
