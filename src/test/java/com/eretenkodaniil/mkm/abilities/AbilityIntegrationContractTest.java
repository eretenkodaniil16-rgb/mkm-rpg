package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityIntegrationContractTest {
    @Test void defaultRenewalNumbersComeFromExistingModifierSnapshot() {
        var snapshot = PlayerModifierCalculator.calculate(new CharacterData());
        var terms = RenewalParameters.from(snapshot);
        assertEquals(snapshot.get(PlayerModifierId.MAX_MANA), terms.maximumMana(), 1e-8);
        assertEquals(snapshot.get(PlayerModifierId.MANA_REGENERATION),
                terms.manaRegeneration(), 1e-8);
        assertEquals(snapshot.get(PlayerModifierId.MANA_COST) * 20, terms.cost(), 1e-8);
        assertEquals(60L, terms.cooldownTicks());
        assertEquals(6F, terms.healing(), 1e-6);
        assertEquals(40L, terms.regenerationDelayTicks());
    }

    @Test void willpowerFeedsManaAndHealingOnlyThroughCharacterCore() {
        var neutral = PlayerModifierCalculator.calculate(new CharacterData());
        var withWillpower = PlayerModifierCalculator.calculate(
                new CharacterData().withAttribute(PrimaryAttribute.WILLPOWER, 40));
        var normal = RenewalParameters.from(neutral);
        var boosted = RenewalParameters.from(withWillpower);
        assertTrue(boosted.maximumMana() > normal.maximumMana());
        assertTrue(boosted.manaRegeneration() > normal.manaRegeneration());
        assertTrue(boosted.cost() < normal.cost());
        assertTrue(boosted.healing() > normal.healing());
    }

    @Test void intelligenceDoesNotInventWillpowerManaCoefficients() {
        var neutral = PlayerModifierCalculator.calculate(new CharacterData());
        var intFocused = PlayerModifierCalculator.calculate(
                new CharacterData().withAttribute(PrimaryAttribute.INTELLIGENCE, 40));
        assertTrue(intFocused.get(PlayerModifierId.MAGIC_POWER)
                > neutral.get(PlayerModifierId.MAGIC_POWER));
        var normal = RenewalParameters.from(neutral);
        var intelligent = RenewalParameters.from(intFocused);
        assertEquals(normal.maximumMana(), intelligent.maximumMana(), 1e-8);
        assertEquals(normal.cost(), intelligent.cost(), 1e-8);
    }

    @Test void learnedAndEquipmentGrantsDoNotExecuteUnimplementedSpells() {
        assertTrue(AbilityAccessRules.hasGrant("mkm:renewal", Set.of(), Set.of()));
        assertFalse(AbilityAccessRules.hasGrant("mkm:arcane_bolt", Set.of(), Set.of()));
        assertTrue(AbilityAccessRules.hasGrant(
                "mkm:arcane_bolt", Set.of("mkm:arcane_bolt"), Set.of()));
        assertTrue(AbilityAccessRules.hasGrant(
                "mkm:arcane_bolt", Set.of(), Set.of("mkm:arcane_bolt")));
        assertFalse(AbilityAccessRules.executableNow("mkm:arcane_bolt"));
        assertTrue(AbilityAccessRules.executableNow("mkm:renewal"));
        assertFalse(AbilityAccessRules.hasGrant("mkm:malicious", Set.of("mkm:malicious"), Set.of()));
        assertFalse(AbilityAccessRules.hasGrant("mkm:renewal", null, Set.of()));
    }

    private static ArcaneBoltTargetRules.TargetFacts facts(
            boolean canAttack, boolean clear, boolean first, double distanceSquared) {
        return new ArcaneBoltTargetRules.TargetFacts(
                true, true, false, true, canAttack, clear, first, distanceSquared);
    }

    @Test void boltGeometryAcceptsValidFirstTargetAtRangeBoundary() {
        assertTrue(ArcaneBoltTargetRules.eligible(facts(true, true, true, 144), 1));
        assertTrue(ArcaneBoltTargetRules.eligible(facts(true, true, true, 196), 1.5));
        assertFalse(ArcaneBoltTargetRules.eligible(facts(true, true, true, 144.01), 1));
        assertFalse(ArcaneBoltTargetRules.eligible(facts(true, true, true, 0), 1));
    }

    @Test void boltRejectsObstructionsPvpProtectionAndDifferentDimension() {
        assertFalse(ArcaneBoltTargetRules.eligible(facts(false, true, true, 9), 1));
        assertFalse(ArcaneBoltTargetRules.eligible(facts(true, false, true, 9), 1));
        assertFalse(ArcaneBoltTargetRules.eligible(facts(true, true, false, 9), 1));
        var first = facts(true, true, true, 9);
        assertFalse(ArcaneBoltTargetRules.eligible(new ArcaneBoltTargetRules.TargetFacts(
                true, true, false, false, true, true, true, 9), 1));
        assertFalse(ArcaneBoltTargetRules.eligible(new ArcaneBoltTargetRules.TargetFacts(
                true, true, true, true, true, true, true, 9), 1));
        assertFalse(ArcaneBoltTargetRules.eligible(new ArcaneBoltTargetRules.TargetFacts(
                true, false, false, true, true, true, true, 9), 1));
        assertFalse(ArcaneBoltTargetRules.eligible(new ArcaneBoltTargetRules.TargetFacts(
                false, true, false, true, true, true, true, 9), 1));
    }

    @Test void boltRejectsNonFiniteAndOutOfRangeInputs() {
        assertFalse(ArcaneBoltTargetRules.eligible(facts(true, true, true, Double.NaN), 1));
        assertFalse(ArcaneBoltTargetRules.eligible(facts(true, true, true, -1), 1));
        assertFalse(ArcaneBoltTargetRules.eligible(facts(true, true, true, 9), Double.NaN));
        assertFalse(ArcaneBoltTargetRules.eligible(facts(true, true, true, 9), 0));
        assertFalse(ArcaneBoltTargetRules.eligible(null, 1));
    }
}
