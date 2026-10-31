package com.eretenkodaniil.mkm.character.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
import java.util.EnumMap;
import org.junit.jupiter.api.Test;

final class PlayerModifierCalculatorTest {
    private static final double EPSILON = 1.0E-9;
    private static final int NEUTRAL = 10;

    @Test
    void neutralCharacterUsesBaseValues() {
        PlayerModifierSnapshot snapshot = calculateWith();

        assertEquals(PlayerModifierId.values().length, ModifierDefinitions.all().size());
        assertEquals(PlayerModifierId.values().length, snapshot.all().size());

        for (PlayerModifierId id : PlayerModifierId.values()) {
            assertEquals(ModifierDefinitions.get(id).baseValue(), snapshot.get(id), EPSILON, id.name());
        }
    }

    @Test
    void strengthChangesPhysicalPathWithoutChangingMagicPower() {
        PlayerModifierSnapshot snapshot = calculateWith(PrimaryAttribute.STRENGTH, 30);

        assertTrue(snapshot.get(PlayerModifierId.PHYSICAL_POWER) > 1.0D);
        assertTrue(snapshot.get(PlayerModifierId.MELEE_DAMAGE) > 1.0D);
        assertEquals(1.0D, snapshot.get(PlayerModifierId.MAGIC_POWER), EPSILON);
    }

    @Test
    void enduranceAndWillpowerImproveResourceEconomy() {
        EnumMap<PrimaryAttribute, Integer> values = neutralValues();
        values.put(PrimaryAttribute.ENDURANCE, 30);
        values.put(PrimaryAttribute.WILLPOWER, 30);
        PlayerModifierSnapshot snapshot = PlayerModifierCalculator.calculate(
                attribute -> values.get(attribute));

        assertTrue(snapshot.get(PlayerModifierId.MAX_STAMINA) > 100.0D);
        assertTrue(snapshot.get(PlayerModifierId.STAMINA_COST) < 1.0D);
        assertTrue(snapshot.get(PlayerModifierId.STAMINA_REGEN_DELAY) < 1.5D);
        assertTrue(snapshot.get(PlayerModifierId.MAX_MANA) > 100.0D);
        assertTrue(snapshot.get(PlayerModifierId.MANA_COST) < 1.0D);
        assertTrue(snapshot.get(PlayerModifierId.MANA_REGEN_DELAY) < 2.0D);
    }

    @Test
    void extremeAttributesStayInsideDefinitionBounds() {
        PlayerModifierSnapshot snapshot = PlayerModifierCalculator.calculate(attribute -> 100);

        for (PlayerModifierId id : PlayerModifierId.values()) {
            ModifierDefinition definition = ModifierDefinitions.get(id);
            double value = snapshot.get(id);
            assertTrue(value >= definition.minValue() - EPSILON, id.name() + " below min");
            assertTrue(value <= definition.maxValue() + EPSILON, id.name() + " above max");
        }
    }

    @Test
    void scalingCurveDiminishesAtHighValues() {
        assertEquals(0.0D, AttributeScaling.effectiveDelta(NEUTRAL), EPSILON);
        double firstTen = AttributeScaling.effectiveDelta(20);
        double nextTen = AttributeScaling.effectiveDelta(30) - firstTen;
        assertTrue(firstTen > nextTen);
    }

    private static PlayerModifierSnapshot calculateWith() {
        return PlayerModifierCalculator.calculate(attribute -> NEUTRAL);
    }

    private static PlayerModifierSnapshot calculateWith(PrimaryAttribute attribute, int value) {
        EnumMap<PrimaryAttribute, Integer> values = neutralValues();
        values.put(attribute, value);
        return PlayerModifierCalculator.calculate(key -> values.get(key));
    }

    private static EnumMap<PrimaryAttribute, Integer> neutralValues() {
        EnumMap<PrimaryAttribute, Integer> values = new EnumMap<>(PrimaryAttribute.class);
        for (PrimaryAttribute attribute : PrimaryAttribute.values()) {
            values.put(attribute, NEUTRAL);
        }
        return values;
    }
}
