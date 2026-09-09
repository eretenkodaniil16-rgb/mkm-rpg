package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.client.gui.CharacterSheetScreen;
import com.eretenkodaniil.mkm.registry.MkmItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Physical-client entry points for the first MKM character interface.
 *
 * <p>This class is never loaded on a dedicated server.</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = MkmMod.MOD_ID)
public final class MkmClient {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(MkmMod.MOD_ID, "general"));

    private static final KeyMapping OPEN_CHARACTER_SHEET = new KeyMapping(
            "key.mkm.open_character_sheet",
            GLFW.GLFW_KEY_K,
            CATEGORY) {
        @Override
        public void setDown(boolean down) {
            if (down) {
                openCharacterSheet();
            }
            super.setDown(down);
        }
    };

    private MkmClient() {
    }

    @SubscribeEvent
    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_CHARACTER_SHEET);
    }

    @SubscribeEvent
    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isCodex(event)) {
            openCharacterSheet();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isCodex(event)) {
            openCharacterSheet();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (isCodex(event)) {
            openCharacterSheet();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static boolean isCodex(PlayerInteractEvent event) {
        return event.getItemStack().getItem() == MkmItems.CODEX.get();
    }

    private static void openCharacterSheet() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.gui.setScreen(new CharacterSheetScreen());
        }
    }
}
