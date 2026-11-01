package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.client.gui.CharacterSheetScreen;
import com.eretenkodaniil.mkm.client.animation.NativeAnimationToggle;
import com.eretenkodaniil.mkm.client.animation.HitFeedbackParticles;
import com.eretenkodaniil.mkm.combat.net.HitConfirm;
import com.eretenkodaniil.mkm.combat.net.DodgeRequest;
import com.eretenkodaniil.mkm.combat.net.ComboRequest;
import com.eretenkodaniil.mkm.combat.DodgeDirection;
import com.eretenkodaniil.mkm.registry.MkmItems;
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
    private static final KeyMapping COMBO = new KeyMapping(
            "key.mkm.combo", InputConstants.KEY_R, CATEGORY);
    private static final KeyMapping ANIMATION_AB = new KeyMapping(
            "key.mkm.animation_ab", InputConstants.KEY_F8, CATEGORY);

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(HitConfirm.TYPE,
                (payload, context) -> context.client().execute(
                        () -> HitFeedbackParticles.onConfirmedHit(context.client(), payload)));
        KeyMappingHelper.registerKeyMapping(OPEN_SHEET);
        KeyMappingHelper.registerKeyMapping(DODGE);
        KeyMappingHelper.registerKeyMapping(COMBO);
        KeyMappingHelper.registerKeyMapping(ANIMATION_AB);
        HudElementRegistry.attachElementAfter(VanillaHudElements.INFO_BAR,
                MkmMod.id("combat_hud"), MkmCombatHud::extract);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ANIMATION_AB.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    boolean nativeMode = NativeAnimationToggle.flip();
                    client.gui.setOverlayMessage(Component.literal(
                            nativeMode ? "MNAE M1: ON (two demo strikes)"
                                    : "C3.8: ON (legacy animations)"), true);
                }
            }
            while (OPEN_SHEET.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    openCharacterSheet();
                }
            }
            while (COMBO.consumeClick()) {
                if (client.player == null || client.gui.screen() != null
                        || !ClientPlayNetworking.canSend(ComboRequest.TYPE)) continue;
                ClientPlayNetworking.send(new ComboRequest((byte) 1));
            }
            while (DODGE.consumeClick()) {
                if (client.player == null || client.gui.screen() != null ||
                        !ClientPlayNetworking.canSend(DodgeRequest.TYPE)) {
                    continue;
                }

                // Use the movement snapshot Minecraft itself processes in
                // KeyboardInput.tick. KeyMapping.isDown() sampling here could
                // default G+A / G+S / G+D to a forward-only dodge.
                var keys = client.player.input.keyPresses;
                byte direction = DodgeDirection.fromMovement(
                        keys.forward(), keys.backward(), keys.left(), keys.right());
                ClientPlayNetworking.send(new DodgeRequest(direction));
            }
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
