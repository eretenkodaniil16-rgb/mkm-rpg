package com.eretenkodaniil.mkm.character;

import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
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

    /**
     * Authoritative mutation boundary for primary attributes. CharacterData performs the final
     * storage clamp, so all callers share the same invariant.
     */
    public static CharacterData setAttribute(ServerPlayer player, PrimaryAttribute attribute, int value) {
        CharacterData current = get(player);
        CharacterData updated = current.withAttribute(attribute, value);
        player.setData(CharacterAttachments.CHARACTER, updated);
        return updated;
    }

    public static CharacterData addAttribute(ServerPlayer player, PrimaryAttribute attribute, int amount) {
        CharacterData current = get(player);
        long candidate = (long) current.attribute(attribute) + amount;
        int safeValue;
        if (candidate > Integer.MAX_VALUE) {
            safeValue = Integer.MAX_VALUE;
        } else if (candidate < Integer.MIN_VALUE) {
            safeValue = Integer.MIN_VALUE;
        } else {
            safeValue = (int) candidate;
        }
        return setAttribute(player, attribute, safeValue);
    }
}
