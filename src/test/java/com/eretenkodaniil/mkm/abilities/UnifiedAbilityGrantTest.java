package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.abilities.AbilityGrantPolicy.Actor;
import com.eretenkodaniil.mkm.abilities.AbilityGrantPolicy.Grants;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnifiedAbilityGrantTest {
    private static Grants player(String clazz, String race) {
        return new Grants(Actor.PLAYER, clazz, race, Set.of(), Set.of(), Set.of());
    }

    @Test void classPreviewGrantsAreDeclarativeAndDoNotBypassEffectAllowlist() {
        assertTrue(AbilityGrantPolicy.granted(player(AbilityArchetypes.WARRIOR, null),
                "mkm:iron_will"));
        assertFalse(AbilityGrantPolicy.executable(player(AbilityArchetypes.WARRIOR, null),
                "mkm:iron_will")); // passive never castable
        assertFalse(AbilityGrantPolicy.granted(player(AbilityArchetypes.WARRIOR, null),
                "mkm:arcane_bolt"));
        assertTrue(AbilityGrantPolicy.granted(player(AbilityArchetypes.MAGE, null),
                "mkm:arcane_bolt"));
        assertFalse(AbilityGrantPolicy.executable(player(AbilityArchetypes.MAGE, null),
                "mkm:arcane_bolt")); // no agreed Combat magic damage hook
        assertTrue(AbilityGrantPolicy.executable(Grants.currentPlayer(), "mkm:renewal"));
    }

    @Test void racialActiveAndPassiveCanBeGrantedButAreNotExecutableYet() {
        assertTrue(AbilityGrantPolicy.granted(player(null, AbilityArchetypes.STONEBORN),
                "mkm:stone_resilience"));
        assertTrue(AbilityGrantPolicy.granted(player(null, AbilityArchetypes.WINDBORN),
                "mkm:ancestral_focus"));
        assertFalse(AbilityGrantPolicy.executable(player(null, AbilityArchetypes.WINDBORN),
                "mkm:ancestral_focus"));
        assertFalse(AbilityGrantPolicy.executable(player(null, AbilityArchetypes.STONEBORN),
                "mkm:stone_resilience"));
        assertFalse(AbilityGrantPolicy.granted(player("mkm:unknown", null),
                "mkm:iron_will"));
    }

    @Test void learnedEquipmentAndAiSourcesAreIsolatedAndImmutable() {
        Grants character = new Grants(Actor.PLAYER, null, null,
                Set.of("mkm:arcane_bolt"), Set.of("mkm:iron_will"),
                Set.of("mkm:renewal"));
        assertTrue(AbilityGrantPolicy.granted(character, "mkm:arcane_bolt"));
        assertTrue(AbilityGrantPolicy.granted(character, "mkm:iron_will"));
        assertFalse(AbilityGrantPolicy.granted(character, "mkm:not_in_catalog"));
        Grants hostile = Grants.taggedHostileRenewal();
        assertTrue(AbilityGrantPolicy.executable(hostile, "mkm:renewal"));
        assertFalse(AbilityGrantPolicy.granted(hostile, "mkm:arcane_bolt"));
        assertFalse(AbilityGrantPolicy.executable(
                new Grants(Actor.HOSTILE_AI, AbilityArchetypes.MAGE,
                        AbilityArchetypes.WINDBORN, Set.of("mkm:renewal"),
                        Set.of("mkm:renewal"), Set.of()), "mkm:renewal"));
    }
}
