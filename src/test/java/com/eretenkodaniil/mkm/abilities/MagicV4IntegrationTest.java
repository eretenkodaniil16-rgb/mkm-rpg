package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterRewardRules;
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
                AbilityGrantPolicy.fromSaved(c, origin("mkm:mage")), "mkm:mage_ward"));
        assertFalse(AbilityGrantPolicy.executable(
                AbilityGrantPolicy.fromSaved(c, origin("mkm:warrior")), "mkm:mage_ward"));
        assertTrue(AbilityGrantPolicy.executable(
                AbilityGrantPolicy.fromSaved(c, new CharacterOriginData()), "mkm:renewal"));
        assertFalse(AbilityGrantPolicy.executable(
                AbilityGrantPolicy.fromSaved(c, new CharacterOriginData()), "mkm:mage_ward"));
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

    @Test void rareTomeProvidesLimitedOffClassLicenseNotFreeMagePrivilege() {
        var warrior = new AbilityGrantPolicy.Grants(
                "mkm:warrior", 20, Set.of("mkm:mage_ward"));
        var noBook = new AbilityGrantPolicy.Grants(
                "mkm:warrior", 20, Set.of());
        assertTrue(AbilityGrantPolicy.executable(warrior, "mkm:mage_ward"));
        assertFalse(AbilityGrantPolicy.executable(noBook, "mkm:mage_ward"));
        assertEquals(1.5D, SpellLicenseService.offClassManaMultiplier(
                "mkm:warrior", true), 1e-9);
        assertEquals(1D, SpellLicenseService.offClassManaMultiplier(
                "mkm:mage", true), 1e-9);
        assertEquals(1D, SpellLicenseService.offClassManaMultiplier(
                "mkm:warrior", false), 1e-9);
        assertTrue(AbilityGrantPolicy.executable(new AbilityGrantPolicy.Grants(
                "mkm:mage",1,Set.of()), "mkm:mage_ward"));
    }

    @Test void grantLevelAndResearchMetadataCannotCreateUnapprovedDamage() {
        var mage = new AbilityGrantPolicy.Grants("mkm:mage",100,Set.of("mkm:arcane_bolt"));
        assertTrue(AbilityGrantPolicy.granted(mage,"mkm:arcane_bolt"));
        assertFalse(AbilityGrantPolicy.executable(mage,"mkm:arcane_bolt"));
        assertEquals(1, SpellProgression.get("mkm:renewal").tier());
        assertEquals(SpellSchool.RESTORATION,
                SpellProgression.get("mkm:renewal").school());
        assertEquals(SpellSchool.ABJURATION,
                SpellProgression.get("mkm:mage_ward").school());
        assertFalse(AbilityGrantPolicy.executable(
                AbilityGrantPolicy.fromSaved(new CharacterData(), origin("mkm:mage")),
                "mkm:arcane_barrier"));
        assertTrue(SpellProgression.meetsLevel("mkm:mage_ward",1));
        assertEquals(10, SpellProgression.maximumTierAtLevel(100));
        assertThrows(IllegalArgumentException.class,()->ManaCapacityPolicy.levelBonus(101));
    }

    @Test void existingCharacterV4QuestReceiptsSurviveMagicScalingWithoutRepeatXp() {
        var reward = CharacterRewardRules.grantOnce(
                new CharacterData(), "mkm:magic_qa/one_time", 200L);
        assertEquals(CharacterRewardRules.Status.GRANTED, reward.status());
        CharacterData awarded = reward.character();
        assertEquals(4, awarded.schemaVersion());
        assertTrue(awarded.hasClaimedReward("mkm:magic_qa/one_time"));
        double manaBonus = ManaCapacityPolicy.extra(awarded);
        assertTrue(manaBonus > 0D);
        var snapshot = OriginModifierRules.snapshot(awarded,
                Map.of(PlayerModifierId.MAX_MANA, manaBonus), origin("mkm:mage"));
        assertTrue(snapshot.get(PlayerModifierId.MAX_MANA) >= 100D);
        assertEquals(java.util.List.of("mkm:magic_qa/one_time"),
                awarded.claimedRewardIds());
        var second = CharacterRewardRules.grantOnce(
                awarded, "mkm:magic_qa/one_time", 200L);
        assertEquals(CharacterRewardRules.Status.ALREADY_GRANTED, second.status());
        assertSame(awarded, second.character());
        assertEquals(awarded.experience(), second.character().experience());
    }

    @Test void expiredFutureOriginSchemaFailsClosed() {
        var current = new CharacterData();
        var future = new CharacterOriginData(999,1,"mkm:human","mkm:mage");
        assertNull(AbilityGrantPolicy.fromSaved(current,future));
        // A future schema can also have an unselected class. It must NOT
        // silently become a v1 legacy profile and revive old cast access.
        var unselectedFuture = new CharacterOriginData(999,0,"","");
        assertNull(AbilityGrantPolicy.fromSaved(current,unselectedFuture));
    }
}
