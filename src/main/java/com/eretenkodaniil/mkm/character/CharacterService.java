package com.eretenkodaniil.mkm.character;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Server-facing operations for character state.
 */
public final class CharacterService {
    private CharacterService() {
    }

    public static CharacterData get(Player player) {
        return player.getData(CharacterAttachments.CHARACTER);
    }

    /**
     * Guarantees that a default attachment exists and explicitly synchronizes it to its owner.
     */
    public static CharacterData ensureAndSync(ServerPlayer player) {
        CharacterData data = get(player);
        player.syncData(CharacterAttachments.CHARACTER);
        return data;
    }

    public static CharacterData setExperience(ServerPlayer player, long experience) {
        CharacterData current = get(player);
        CharacterData updated = current.withExperience(Math.max(0L, experience));
        player.setData(CharacterAttachments.CHARACTER, updated);
        return updated;
    }

    public static CharacterData addExperience(ServerPlayer player, long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException("Experience amount must be non-negative");
        }

        CharacterData current = get(player);
        long currentExperience = current.experience();
        long updatedExperience = amount > Long.MAX_VALUE - currentExperience
                ? Long.MAX_VALUE
                : currentExperience + amount;
        return setExperience(player, updatedExperience);
    }
}
