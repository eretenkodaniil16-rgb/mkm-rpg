package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.ProgressionRules;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginData;
import com.eretenkodaniil.mkm.character.origin.OriginModifierRules;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MagicV4IntegrationTest {
    private static CharacterOriginData origin(String clazz) {
        return new CharacterOriginData(1, 1, "mkm:human", clazz);
    }

    @Test void authoritativeMageUnlockAndWarriorDenied() {
        var c = new CharacterData();
        assertEquals(4, c.schemaVersion());
        assertTrue(AbilityGrantPolicy.executable(
                AbilityGrantPolicy.fromSaved(c, origin("mkm:mage")), "mkm:arcane_barrier"));
        assertFalse(AbilityGrantPolicy.executable(
                AbilityGrantPolicy.fromSaved(c, origin("mkm:warrior")), "mkm:arcane_barrier"));
        assertTrue(AbilityGrantPolicy.executable(
                AbilityGrantPolicy.fromSaved(c, new CharacterOriginData()), "mkm:renewal"));
        assertFalse(AbilityGrantPolicy.executable(
                AbilityGrantPolicy.fromSaved(c, new CharacterOriginData()), "mkm:arcane_barrier"));
        assertFalse(AbilityGrantPolicy.executable(
                AbilityGrantPolicy.fromSaved(c, origin("mkm:mage")), "mkm:arcane_bolt"));
        assertFalse(AbilityGrantPolicy.executable(
                AbilityGrantPolicy.fromSaved(c, origin("mkm:mage")), "mkm:repulsion_wave"));
    }

    @Test void levelIntWillpowerAndOriginBonusUseOneFinalClamp() {
        var origin = origin("mkm:mage");
        for (int level : new int[]{1,20,50,100}) {
            CharacterData c = new CharacterData()
                    .withExperience(ProgressionRules.experienceForLevel(level));
            var s = OriginModifierRules.snapshot(c,
                    Map.of(PlayerModifierId.MAX_MANA, ManaCapacityPolicy.extra(c)), origin);
            assertEquals(Math.min(300D, 106D + ManaCapacityPolicy.levelBonus(level)),
                    s.get(PlayerModifierId.MAX_MANA), 0.00001D);
            assertEquals(level, c.level());
            assertEquals(4, c.schemaVersion());
        }
        var invested = new CharacterData()
                .withAttribute(PrimaryAttribute.WILLPOWER,40)
                .withAttribute(PrimaryAttribute.INTELLIGENCE,40)
                .withExperience(ProgressionRules.experienceForLevel(100));
        var s = OriginModifierRules.snapshot(invested,
                Map.of(PlayerModifierId.MAX_MANA,ManaCapacityPolicy.extra(invested)), origin);
        assertEquals(100D + 6D + 45D + 30D + ManaCapacityPolicy.levelBonus(100),
                s.get(PlayerModifierId.MAX_MANA),0.001D);
        assertEquals(4,invested.schemaVersion());
    }

    @Test void grantLevelAndResearchMetadataCannotCreateUnapprovedDamage() {
        var mage = new AbilityGrantPolicy.Grants("mkm:mage",100,Set.of("mkm:arcane_bolt"));
        assertTrue(AbilityGrantPolicy.granted(mage,"mkm:arcane_bolt"));
        assertFalse(AbilityGrantPolicy.executable(mage,"mkm:arcane_bolt"));
        assertEquals(1, SpellProgression.get("mkm:renewal").tier());
        assertEquals(SpellSchool.RESTORATION,
                SpellProgression.get("mkm:renewal").school());
        assertEquals(SpellSchool.ABJURATION,
                SpellProgression.get("mkm:arcane_barrier").school());
        assertTrue(SpellProgression.meetsLevel("mkm:arcane_barrier",1));
        assertEquals(10, SpellProgression.maximumTierAtLevel(100));
        assertThrows(IllegalArgumentException.class,()->ManaCapacityPolicy.levelBonus(101));
    }

    @Test void expiredFutureOriginSchemaFailsClosed() {
        var current = new CharacterData();
        var future = new CharacterOriginData(999,1,"mkm:human","mkm:mage");
        assertNull(AbilityGrantPolicy.fromSaved(current,future));
    }
}
