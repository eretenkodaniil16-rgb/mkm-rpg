package com.eretenkodaniil.mkm.client.animation;

import static org.junit.jupiter.api.Assertions.*;
import com.eretenkodaniil.mkm.client.animation.WeaponPresentationResolver.*;
import org.junit.jupiter.api.Test;

/** Code-level A/B and main-/offhand routing independent from Minecraft boot. */
final class WeaponPresentationResolverTest {
    private static Choice choose(Family main, Family off, int stage, boolean nativeMode) {
        return WeaponPresentationResolver.resolve(main,off,Hand.MAIN,false,stage,false,nativeMode);
    }
    @Test void equipmentFamilyTableNeverChoosesSwordForUnsupportedMainhand() {
        for(Family family: new Family[]{
                Family.AXE, Family.PICKAXE, Family.OTHER_TOOL,
                Family.BOW,Family.SHIELD,Family.OTHER}) {
            for(int stage=1;stage<=3;stage++) {
                for(boolean nativeOn:new boolean[]{false,true}) {
                    Choice choice=choose(family,Family.EMPTY,stage,nativeOn);
                    assertNull(choice.c3Profile(),family+" incorrectly mapped to sword");
                    assertNull(choice.nativeClip());
                    assertEquals(Reason.UNSUPPORTED_WEAPON,choice.reason());
                }
            }
        }
    }
    @Test void offhandOnlySwordOrShieldNeverMovesAsUnarmedMainhand() {
        for(Family off:new Family[]{Family.SWORD,Family.SHIELD,Family.AXE}) {
            for(int stage=1;stage<=3;stage++) {
                Choice choice=choose(Family.EMPTY,off,stage,true);
                assertNull(choice.c3Profile());
                assertNull(choice.nativeClip());
                assertEquals(Reason.OFFHAND_OCCUPIED,choice.reason());
            }
        }
    }
    @Test void swordMainAndEmptyMainBothHaveTrueF8ABOnlyAtComboOne() {
        for(Family main:new Family[]{Family.SWORD,Family.EMPTY}) {
            Choice legacy=choose(main,Family.EMPTY,1,false);
            Choice nativeOn=choose(main,Family.EMPTY,1,true);
            assertEquals(legacy.c3Profile(),nativeOn.c3Profile());
            assertNull(legacy.nativeClip());
            assertNotNull(nativeOn.nativeClip());
            assertEquals(Reason.MNAE_DEMO,nativeOn.reason());
            for(int stage=2;stage<=3;stage++) {
                Choice later=choose(main,Family.EMPTY,stage,true);
                assertNotNull(later.c3Profile());
                assertNull(later.nativeClip());
                assertEquals(Reason.NOT_FIRST_COMBO,later.reason());
            }
        }
    }
    @Test void gripUnsupportedPreventsDualWieldAndOffhandAttack() {
        assertEquals(Reason.OFFHAND_OCCUPIED,
                choose(Family.SWORD,Family.SHIELD,1,true).reason());
        var offAttack=WeaponPresentationResolver.resolve(Family.EMPTY,Family.SWORD,
                Hand.OFF,false,1,false,true);
        assertNull(offAttack.nativeClip());
        assertNull(offAttack.c3Profile());
        assertEquals(Reason.OFFHAND_ATTACK_UNSUPPORTED,offAttack.reason());
    }
    @Test void leftDominantMirrorsLeadingArmNotActionFamily() {
        Choice right=WeaponPresentationResolver.resolve(Family.SWORD,Family.EMPTY,
                Hand.MAIN,false,1,false,true);
        Choice left=WeaponPresentationResolver.resolve(Family.SWORD,Family.EMPTY,
                Hand.MAIN,true,1,false,true);
        assertEquals(right.nativeClip(),left.nativeClip());
        assertEquals(Arm.RIGHT,right.leadingArm());
        assertEquals(Arm.LEFT,left.leadingArm());
    }
    @Test void actualF8ToggleChangesOnlyNativeFirstHitSelection() {
        // Save and restore static toggle to avoid leaking state to unrelated tests.
        boolean initial=NativeAnimationToggle.enabled();
        try {
            var a=choose(Family.SWORD,Family.EMPTY,1,NativeAnimationToggle.enabled());
            NativeAnimationToggle.flip();
            var b=choose(Family.SWORD,Family.EMPTY,1,NativeAnimationToggle.enabled());
            assertNotEquals(a.nativeClip(),b.nativeClip());
            assertEquals(a.c3Profile(),b.c3Profile());
            assertEquals(Reason.NOT_FIRST_COMBO,
                    choose(Family.SWORD,Family.EMPTY,2,true).reason());
        } finally {
            if(NativeAnimationToggle.enabled()!=initial) NativeAnimationToggle.flip();
        }
    }

    @Test void dodgeIsExplicitLegacyRegardlessF8AndHeldItems() {
        for(Family main:Family.values())for(boolean mode:new boolean[]{false,true}) {
            Choice choice=WeaponPresentationResolver.resolve(main,Family.SWORD,
                    Hand.MAIN,true,0,true,mode);
            assertEquals(AnimationController.FIRST_PROFILE,choice.c3Profile());
            assertNull(choice.nativeClip());
            assertEquals(Reason.DODGE_LEGACY_C3,choice.reason());
        }
    }
}
