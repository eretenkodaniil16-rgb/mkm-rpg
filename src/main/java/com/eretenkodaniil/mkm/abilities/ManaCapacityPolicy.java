package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.ProgressionRules;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.ModifierDefinitions;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;

/**
 * Add exactly one flat, level-based MP bonus to Character Core's equipment-
 * aware derived MAX_MANA. WILLPOWER and equipment are already in the snapshot;
 * never recompute those contributions here or add the level bonus twice.
 *
 * A future Character-level modifier aggregator may fold this term into the
 * pre-clamp modifier set. Until then Magic applies only this single policy
 * to its own resource ledger/HUD, preserving CharacterData v3/v4 unchanged.
 */
public final class ManaCapacityPolicy {
    public static final double MANA_PER_LEVEL = 1.0D;

    private ManaCapacityPolicy() {}

    public static double maximum(PlayerModifierSnapshot snapshot, int characterLevel) {
        if (snapshot == null || characterLevel < ProgressionRules.MIN_LEVEL
                || characterLevel > ProgressionRules.MAX_LEVEL) {
            throw new IllegalArgumentException("Invalid mana level/snapshot");
        }
        double derived = snapshot.get(PlayerModifierId.MAX_MANA);
        if (!Double.isFinite(derived)) throw new IllegalArgumentException("Nonfinite mana");
        double sum = derived + MANA_PER_LEVEL * (characterLevel - 1);
        return ModifierDefinitions.get(PlayerModifierId.MAX_MANA).clamp(sum);
    }
}
