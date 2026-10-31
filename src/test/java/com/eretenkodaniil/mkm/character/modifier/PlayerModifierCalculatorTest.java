package com.eretenkodaniil.mkm.character.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
import org.junit.jupiter.api.Test;

final class PlayerModifierCalculatorTest {
    private static final double EPSILON = 1.0E-9;

    @Test
    void neutralCharacterUsesBaseValues() {
        CharacterData neutral = new CharacterData();
        PlayerModifierSnapshot snapshot = PlayerModifierCalculator.calculate(neutral);

        assertEquals(PlayerModifierId.values().length, ModifierDefinitions.all().size());
        assertEquals(PlayerModifierId.values().length, snapshot.all().size());

        for (PlayerModifierId id : PlayerModifierId.values()) {
            assertEquals(ModifierDefinitions.get(id).baseValue(), snapshot.get(id), EPSILON, id.name());
        }
    }

    @Test
    void strengthChangesPhysicalPathWithoutChangingMagicPower() {
        CharacterData strong = new CharacterData().withAttribute(PrimaryAttribute.STRENGTH, 30);
        PlayerModifierSnapshot snapshot = PlayerModifierCalculator.calculate(strong);

        assertTrue(snapshot.get(PlayerModifierId.PHYSICAL_POWER) > 1.0D);
        assertTrue(snapshot.get(PlayerModifierId.MELEE_DAMAGE) > 1.0D);
        assertEquals(1.0D, snapshot.get(PlayerModifierId.MAGIC_POWER), EPSILON);
    }

    @Test
    void enduranceAndWillpowerImproveResourceEconomy() {
        CharacterData data = new CharacterData()
                .withAttribute(PrimaryAttribute.ENDURANCE, 30)
                .withAttribute(PrimaryAttribute.WILLPOWER, 30);
        PlayerModifierSnapshot snapshot = PlayerModifierCalculator.calculate(data);

        assertTrue(snapshot.get(PlayerModifierId.MAX_STAMINA) > 100.0D);
        assertTrue(snapshot.get(PlayerModifierId.STAMINA_COST) < 1.0D);
        assertTrue(snapshot.get(PlayerModifierId.STAMINA_REGEN_DELAY) < 1.5D);
        assertTrue(snapshot.get(PlayerModifierId.MAX_MANA) > 100.0D);
        assertTrue(snapshot.get(PlayerModifierId.MANA_COST) < 1.0D);
        assertTrue(snapshot.get(PlayerModifierId.MANA_REGEN_DELAY) < 2.0D);
    }

    @Test
    void extremeAttributesStayInsideDefinitionBounds() {
        CharacterData maxed = new CharacterData();
        for (PrimaryAttribute attribute : PrimaryAttribute.values()) {
            maxed = maxed.withAttribute(attribute, CharacterData.MAX_ATTRIBUTE);
        }

        PlayerModifierSnapshot snapshot = PlayerModifierCalculator.calculate(maxed);
        for (PlayerModifierId id : PlayerModifierId.values()) {
            ModifierDefinition definition = ModifierDefinitions.get(id);
            double value = snapshot.get(id);
            assertTrue(value >= definition.minValue() - EPSILON, id.name() + " below min");
            assertTrue(value <= definition.maxValue() + EPSILON, id.name() + " above max");
        }
    }

    @Test
    void scalingCurveDiminishesAtHighValues() {
        assertEquals(0.0D, AttributeScaling.effectiveDelta(CharacterData.DEFAULT_ATTRIBUTE), EPSILON);
        double firstTen = AttributeScaling.effectiveDelta(20);
        double nextTen = AttributeScaling.effectiveDelta(30) - firstTen;
        assertTrue(firstTen > nextTen);
    }
}
