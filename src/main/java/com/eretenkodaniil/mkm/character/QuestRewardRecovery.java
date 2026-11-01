package com.eretenkodaniil.mkm.character;

/**
 * A pure, server-side bridge between World E2 quest state and Character Core's durable XP
 * receipt. This class does not read quest attachments or mutate either subsystem.
 *
 * World must validate the actual quest stage, actor, Warden and distance independently.
 * Legacy XP floors alone are not evidence that the receipt was committed.
 */
public final class QuestRewardRecovery {
    public enum Decision {
        /** Quest is claimable and its reward must pass through grantExperienceOnce. */
        GRANT_ONCE,
        /** Receipt is already committed: transition claimable quest to rewarded without paying. */
        FINALIZE_QUEST,
        /** Both quest completion and receipt agree: no action or top-up. */
        COMPLETE,
        /** Legacy completed quest lacks a receipt: admin audit; never guess a payout. */
        MANUAL_REVIEW,
        /** Invalid stage, reward ID or unsupported character schema. */
        REJECT
    }

    private QuestRewardRecovery() {}

    /**
     * Only one stage flag should be true. legacyXpFloor is accepted to make E2 migration
     * explicit: a nonnegative floor MUST NOT trigger a new payout or XP top-up.
     */
    public static Decision decide(CharacterData character, String rewardId,
                                  boolean readyToClaim, boolean alreadyRewarded,
                                  long legacyXpFloor) {
        if (character == null || character.schemaVersion() != CharacterData.CURRENT_SCHEMA_VERSION
                || !CharacterRewardRules.validRewardId(rewardId)
                || readyToClaim == alreadyRewarded || legacyXpFloor < -1L) {
            return Decision.REJECT;
        }
        boolean receipt = character.hasClaimedReward(rewardId);
        if (readyToClaim) {
            return receipt ? Decision.FINALIZE_QUEST : Decision.GRANT_ONCE;
        }
        return receipt ? Decision.COMPLETE : Decision.MANUAL_REVIEW;
    }
}
