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
        return player.getAttachedOrCreate(CharacterAttachments.CHARACTER);
    }

    /**
     * Guarantees that a default attachment exists and explicitly synchronizes it to its owner.
     */
    public static CharacterData ensureAndSync(ServerPlayer player) {
        CharacterData data = player.getAttached(CharacterAttachments.CHARACTER);
        if (data == null) {
            data = new CharacterData();
            player.setAttached(CharacterAttachments.CHARACTER, data);
        }
        return data;
    }

    public static CharacterData setExperience(ServerPlayer player, long experience) {
        CharacterData current = get(player);
        CharacterData updated = current.withExperience(Math.max(0L, experience));
        player.setAttached(CharacterAttachments.CHARACTER, updated);
        return updated;
    }

    public static CharacterData addExperience(ServerPlayer player, long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException("Experience amount must be non-negative");
        }

        CharacterData current = get(player);
        // Do not let repeatable combat XP rewrite an unknown future save format.
        if (current.schemaVersion() != CharacterData.CURRENT_SCHEMA_VERSION) return current;
        long newExperience = ProgressionRules.experienceAfterReward(current.experience(), amount);
        if (newExperience == current.experience()) return current;
        return setExperience(player, newExperience);
    }

    /**
     * Authoritative mutation boundary for primary attributes. CharacterData performs the final
     * storage clamp, so all callers share the same invariant.
     */
    public static CharacterData setAttribute(ServerPlayer player, PrimaryAttribute attribute, int value) {
        CharacterData current = get(player);
        CharacterData updated = current.withAttribute(attribute, value);
        player.setAttached(CharacterAttachments.CHARACTER, updated);
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
    /**
     * Runs on the logical server. No other player's state, credits or total attributes are
     * controlled by an incoming packet. A duplicate absolute request is a strict no-op.
     */
    public static boolean allocate(ServerPlayer player, int[] requestedTotals, int expectedSpent) {
        if (!player.isAlive() || player.isSpectator()) return false;
        CharacterData current = get(player);
        CharacterData updated = AllocationRules.tryAllocate(current, requestedTotals, expectedSpent);
        if (updated == null) return false;
        player.setAttached(CharacterAttachments.CHARACTER, updated);
        return true;
    }
    /**
     * Authorized SERVER-ONLY quest / encounter reward. Always call before marking a quest
     * rewarded in its separate attachment. Duplicate receipts return ALREADY_GRANTED with
     * no setAttached, preserving already credited XP and points.
     *
     * For a future async quest system callers must marshal back onto the logical server
     * thread first; Fabric callbacks and command handlers already run on that thread.
     */
    public static CharacterRewardRules.Result grantExperienceOnce(
            ServerPlayer player, String rewardId, long amount) {
        if (player == null || !player.isAlive() || player.isSpectator()) {
            throw new IllegalArgumentException("Reward recipient must be a living non-spectator");
        }
        CharacterData current = get(player);
        CharacterRewardRules.Result result = CharacterRewardRules.grantOnce(current, rewardId, amount);
        if (result.status() == CharacterRewardRules.Status.GRANTED) {
            player.setAttached(CharacterAttachments.CHARACTER, result.character());
        }
        return result;
    }

    /** Receipt presence is server-only; the client does not receive redemption history. */
    public static boolean hasExperienceReward(ServerPlayer player, String rewardId) {
        if (!CharacterRewardRules.validRewardId(rewardId)) return false;
        return get(player).hasClaimedReward(rewardId);
    }

}
