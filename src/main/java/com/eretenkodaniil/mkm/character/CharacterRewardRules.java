package com.eretenkodaniil.mkm.character;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Server-side, deterministic reward redemption. The receipt and XP/level-credit change are
 * recorded in one replacement of mkm:character; no standalone mutable XP or reward service.
 *
 * This guarantees idempotence for a consistently saved player record. A quest adapter MUST
 * redeem here before transitioning its own separate quest state to REWARDED.
 */
public final class CharacterRewardRules {
    public static final int MAX_CLAIMED_REWARDS = 4096;
    public static final int MAX_REWARD_ID_LENGTH = 96;
    public static final long MAX_SINGLE_QUEST_XP = 1_000_000L;
    private static final Pattern ID_PATTERN =
            Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public enum Status { GRANTED, ALREADY_GRANTED, INVALID_REQUEST, LEDGER_FULL }

    public record Result(Status status, CharacterData character, long creditedExperience) {
        public Result {
            if (status == null || character == null || creditedExperience < 0L) {
                throw new IllegalArgumentException("Invalid reward result");
            }
        }

        public boolean confirmed() {
            return status == Status.GRANTED || status == Status.ALREADY_GRANTED;
        }
    }

    private CharacterRewardRules() {}

    /** A stable namespaced content key, e.g. mkm:ford_warden/complete (per player). */
    public static boolean validRewardId(String id) {
        if (id == null || id.isEmpty() || id.length() > MAX_REWARD_ID_LENGTH
                || !ID_PATTERN.matcher(id).matches()) return false;
        int colon = id.indexOf(':');
        String path = id.substring(colon + 1);
        return !path.startsWith("/") && !path.endsWith("/") && !path.contains("//")
                && !path.equals(".") && !path.equals("..")
                && !path.startsWith("./") && !path.startsWith("../")
                && !path.contains("/./") && !path.contains("/../")
                && !path.endsWith("/.") && !path.endsWith("/..");
    }

    /** Pure transition: on all rejected/duplicate cases returns the original immutable object. */
    public static Result grantOnce(CharacterData current, String rewardId, long amount) {
        if (current == null) {
            throw new IllegalArgumentException("A character is required");
        }
        if (current.schemaVersion() != CharacterData.CURRENT_SCHEMA_VERSION
                || !validRewardId(rewardId) || amount <= 0L || amount > MAX_SINGLE_QUEST_XP
                || current.spentPoints() > current.earnedPoints()) {
            return new Result(Status.INVALID_REQUEST, current, 0L);
        }
        if (current.hasClaimedReward(rewardId)) {
            return new Result(Status.ALREADY_GRANTED, current, 0L);
        }
        if (current.claimedRewardIds().size() >= MAX_CLAIMED_REWARDS) {
            // NEVER prune old receipts: eviction would permit a second payout.
            return new Result(Status.LEDGER_FULL, current, 0L);
        }
        long newExperience = ProgressionRules.experienceAfterReward(current.experience(), amount);
        CharacterData withExperience = current.withExperience(newExperience);
        List<String> claims = new ArrayList<>(current.claimedRewardIds());
        claims.add(rewardId);
        CharacterData updated = withExperience.withClaimedRewardIds(claims);
        return new Result(Status.GRANTED, updated, newExperience - current.experience());
    }
}
