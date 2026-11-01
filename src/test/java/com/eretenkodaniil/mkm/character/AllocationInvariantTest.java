package com.eretenkodaniil.mkm.character;

import static org.junit.jupiter.api.Assertions.*;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class AllocationInvariantTest {
    @Test
    void staleRevisionMustNotSpendEvenWhenRequestOtherwiseValid() {
        CharacterData before = new CharacterData().withExperience(300);
        CharacterData purchased = AllocationRules.tryAllocate(before,
                new int[] {1, 0, 0, 0, 0, 0, 0}, 0);
        assertNotNull(purchased);
        assertEquals(1, purchased.spentPoints());
        assertNull(AllocationRules.tryAllocate(purchased,
                new int[] {1, 1, 0, 0, 0, 0, 0}, 0),
                "a concurrent stale build must fail even if there is still a free point");
        CharacterData second = AllocationRules.tryAllocate(purchased,
                new int[] {1, 1, 0, 0, 0, 0, 0}, 1);
        assertNotNull(second);
        assertEquals(2, second.spentPoints());
        assertNull(AllocationRules.tryAllocate(second,
                new int[] {1, 1, 0, 0, 0, 0, 0}, 2));
    }

    @Test
    void maxLevelSpendRespectsAllCapsAndLedger() {
        CharacterData max = new CharacterData().withExperience(Long.MAX_VALUE);
        assertEquals(198, max.availablePoints());
        int[] allocation = {30, 30, 30, 30, 30, 18, 30};
        CharacterData spent = AllocationRules.tryAllocate(max, allocation, 0);
        assertNotNull(spent);
        assertEquals(198, spent.spentPoints());
        assertEquals(0, spent.availablePoints());
        for (PrimaryAttribute stat : PrimaryAttribute.values()) {
            assertTrue(spent.attribute(stat) <= ProgressionRules.ALLOCATION_ATTRIBUTE_CAP);
        }
    }

    @Test
    void deterministicMixedSequenceNeverMintsOrRefundsPoints() {
        Random random = new Random(0x4D4B4D);
        CharacterData current = new CharacterData().withExperience(Long.MAX_VALUE);
        int[] intended = new int[CharacterData.ATTRIBUTE_COUNT];
        for (int iteration = 0; iteration < 3000; iteration++) {
            int index = random.nextInt(intended.length);
            int[] proposed = Arrays.copyOf(intended, intended.length);
            proposed[index]++;
            CharacterData next = AllocationRules.tryAllocate(current, proposed, current.spentPoints());
            boolean expected = current.availablePoints() > 0
                    && current.attribute(PrimaryAttribute.values()[index])
                            < ProgressionRules.ALLOCATION_ATTRIBUTE_CAP;
            assertEquals(expected, next != null, "iteration " + iteration);
            if (next != null) {
                assertEquals(current.spentPoints() + 1, next.spentPoints());
                assertEquals(current.availablePoints() - 1, next.availablePoints());
                assertEquals(current.experience(), next.experience());
                assertEquals(current.attribute(PrimaryAttribute.values()[index]) + 1,
                        next.attribute(PrimaryAttribute.values()[index]));
                current = next;
                intended = proposed;
            }
            assertEquals(current.earnedPoints(), current.spentPoints() + current.availablePoints());
        }
        assertEquals(198, current.spentPoints());
    }
}
