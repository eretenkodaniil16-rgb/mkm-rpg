package com.eretenkodaniil.mkm.character;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Character lifecycle hooks that belong on the common NeoForge event bus.
 */
public final class CharacterEvents {
    private CharacterEvents() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CharacterService.ensureAndSync(player);
        }
    }
}
