package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.client.gui.CharacterSheetScreen;
import com.eretenkodaniil.mkm.combat.net.DodgeRequest;
import com.eretenkodaniil.mkm.combat.DodgeDirection;
import com.eretenkodaniil.mkm.registry.MkmItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(OPEN_SHEET);
        KeyMappingHelper.registerKeyMapping(DODGE);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_SHEET.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    openCharacterSheet();
                }
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
