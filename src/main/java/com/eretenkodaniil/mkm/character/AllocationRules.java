package com.eretenkodaniil.mkm.character;

import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure server-side build transaction. Requests contain absolute cumulative allocations, not
 * claimed balances or mutable stat totals. Replays are no-ops; rollback and overspending fail.
 */
public final class AllocationRules {
    private AllocationRules() {}

    /** Returns the new immutable character or null for an invalid/duplicate request. */
    public static CharacterData tryAllocate(CharacterData current, int[] requestedTotals) {
        if (current.schemaVersion() != CharacterData.CURRENT_SCHEMA_VERSION
                || requestedTotals == null || requestedTotals.length != CharacterData.ATTRIBUTE_COUNT
                || current.spentPoints() > current.earnedPoints()) return null;

        PrimaryAttribute[] attributes = PrimaryAttribute.values();
        int[] increases = new int[attributes.length];
        int totalIncrease = 0;
        for (int i = 0; i < attributes.length; i++) {
            int previous = current.allocatedPoints().get(i);
            int requested = requestedTotals[i];
            int delta = requested - previous;
            if (delta < 0 || requested > ProgressionRules.ALLOCATION_ATTRIBUTE_CAP
                    || (delta > 0 && (long) current.attribute(attributes[i]) + delta
                            > ProgressionRules.ALLOCATION_ATTRIBUTE_CAP)) return null;
            increases[i] = delta;
            totalIncrease += delta;
        }

        if (totalIncrease == 0 || totalIncrease > current.availablePoints()) return null;
        CharacterData updated = current;
        List<Integer> allocations = new ArrayList<>(attributes.length);
        for (int i = 0; i < attributes.length; i++) {
            if (increases[i] != 0) {
                updated = updated.withAttribute(attributes[i],
                        current.attribute(attributes[i]) + increases[i]);
            }
            allocations.add(requestedTotals[i]);
        }
        return updated.withAllocatedPoints(allocations);
    }
}
