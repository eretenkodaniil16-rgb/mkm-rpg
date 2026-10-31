package com.eretenkodaniil.mkm.character;

import net.minecraft.server.level.ServerPlayer;

public final class CharacterEvents {
    private CharacterEvents() {}

    public static void onPlayerLoggedIn(ServerPlayer player) {
        CharacterService.ensureAndSync(player);
    }
}
