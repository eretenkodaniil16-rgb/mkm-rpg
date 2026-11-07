package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.ProgressionRules;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ManaCapacityPolicyTest {
    @Test void levelGrowthAtNeutralWillpowerIsExactlyOnePointPerLevel() {
        var data = new CharacterData();
        var stats = PlayerModifierCalculator.calculate(data);
        assertEquals(100D, ManaCapacityPolicy.maximum(stats, 1), 1e-9);
        assertEquals(109D, ManaCapacityPolicy.maximum(stats, 10), 1e-9);
        assertEquals(149D, ManaCapacityPolicy.maximum(stats, 50), 1e-9);
        assertEquals(199D, ManaCapacityPolicy.maximum(stats, 100), 1e-9);
        assertEquals(ProgressionRules.MAX_LEVEL, 100);
    }

    @Test void willpowerEquipmentAndLevelAreIndependentAndOneFinalCapApplies() {
        var neutral = new CharacterData();
        var invested = neutral.withAttribute(PrimaryAttribute.WILLPOWER, 40);
        var base = PlayerModifierCalculator.calculate(neutral);
        var highWillpower = PlayerModifierCalculator.calculate(invested);
        // Character Core: raw WIL 40 -> effective delta 15 -> 45 mana.
        assertEquals(145D, highWillpower.get(PlayerModifierId.MAX_MANA), 1e-9);
        assertEquals(244D, ManaCapacityPolicy.maximum(highWillpower, 100), 1e-9);
        assertEquals(199D, ManaCapacityPolicy.maximum(base, 100), 1e-9);
        var withEquipmentDelta = PlayerModifierCalculator.calculate(
                invested, Map.of(PlayerModifierId.MAX_MANA, 100D));
        assertEquals(245D, withEquipmentDelta.get(PlayerModifierId.MAX_MANA), 1e-9);
        assertEquals(300D, ManaCapacityPolicy.maximum(withEquipmentDelta, 100), 1e-9);

        assertEquals(RenewalParameters.from(highWillpower, 50).maximumMana(),
                ManaCapacityPolicy.maximum(highWillpower, 50), 1e-9);
        assertEquals(RenewalParameters.from(highWillpower).maximumMana(),
                ManaCapacityPolicy.maximum(highWillpower, 1), 1e-9);
    }

    @Test void higherLevelDoesNotArtificiallyIncreaseRegenOrChangeRenewalCosts() {
        var stats = PlayerModifierCalculator.calculate(new CharacterData());
        var first = RenewalParameters.from(stats, 1);
        var last = RenewalParameters.from(stats, 100);
        assertEquals(99D, last.maximumMana() - first.maximumMana(), 1e-9);
        assertEquals(first.manaRegeneration(), last.manaRegeneration(), 1e-9);
        assertEquals(first.regenerationDelayTicks(), last.regenerationDelayTicks());
        assertEquals(first.cost(), last.cost(), 1e-9);
        assertEquals(first.cooldownTicks(), last.cooldownTicks());
        assertEquals(first.healing(), last.healing(), 1e-9);
    }

    @Test void levelAndDerivedManaAreAlwaysServerClamped() {
        var stats = PlayerModifierCalculator.calculate(new CharacterData());
        assertThrows(IllegalArgumentException.class,
                () -> ManaCapacityPolicy.maximum(stats, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ManaCapacityPolicy.maximum(stats, 101));
        assertThrows(IllegalArgumentException.class,
                () -> ManaCapacityPolicy.maximum(null, 1));
    }
}
