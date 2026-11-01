package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.ActionTimeline;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Registry-free family routing tests. Minecraft ItemTags classification is
 * separately verified in actual client runtime: JUnit never bootstraps Items.
 */
class WeaponPresentationResolverTest {
    private static ActionTimeline combo(int stage) {
        return new ActionTimeline(ActionTimeline.Kind.LIGHT_COMBO,
                ActionTimeline.Phase.WINDUP,stage,500,550);
    }
    private static WeaponPresentationResolver.Selection select(
            int stage, WeaponPresentationResolver.Family main,
            WeaponPresentationResolver.Family off, HumanoidArm arm, boolean nativeMode){
        return WeaponPresentationResolver.resolveFamilies(combo(stage),
                main,off,arm,nativeMode);
    }
    @Test void swordAndUnarmedRouteExclusivelyToTheirTrueProfiles(){
        for(int stage=1;stage<=3;stage++){
            var sword=select(stage,WeaponPresentationResolver.Family.SWORD,WeaponPresentationResolver.Family.EMPTY,HumanoidArm.RIGHT,true);
            assertEquals(WeaponPresentationResolver.Family.SWORD,sword.mainFamily());
            assertEquals(InteractionHand.MAIN_HAND,sword.activeHand());
            assertEquals(stage==1?WeaponPresentationResolver.Renderer.MNAE
                    :WeaponPresentationResolver.Renderer.C3,sword.renderer());
            assertEquals("mkm:one_handed_sword",sword.profileId());
            assertEquals(stage==1?"mkm:mnae/sword_cut":null,sword.nativeClipId());
            var empty=select(stage,WeaponPresentationResolver.Family.EMPTY,WeaponPresentationResolver.Family.EMPTY,HumanoidArm.RIGHT,true);
            assertEquals("mkm:unarmed",empty.profileId());
            assertEquals(stage==1?"mkm:mnae/straight_punch":null,empty.nativeClipId());
        }
        assertEquals(WeaponPresentationResolver.Renderer.C3,
            select(1,WeaponPresentationResolver.Family.SWORD,WeaponPresentationResolver.Family.EMPTY,HumanoidArm.RIGHT,false).renderer());
    }
    @Test void offhandOnlySwordNeverMasqueradesAsMainhandUnarmedOrSword(){
        var result=select(1,WeaponPresentationResolver.Family.EMPTY,WeaponPresentationResolver.Family.SWORD,HumanoidArm.RIGHT,true);
        assertEquals(WeaponPresentationResolver.Family.EMPTY,result.mainFamily());
        assertEquals(WeaponPresentationResolver.Family.SWORD,result.offFamily());
        assertEquals(WeaponPresentationResolver.Renderer.VANILLA,result.renderer());
        assertEquals("OFFHAND_ONLY",result.reason());
        assertNull(result.profileId());
        assertNull(result.nativeClipId());
    }
    @Test void axePickaxeBowShieldAndOtherNeverRouteToSwordEvenInC3Fallback(){
        for(var family:java.util.List.of(WeaponPresentationResolver.Family.AXE,WeaponPresentationResolver.Family.PICKAXE,
                WeaponPresentationResolver.Family.BOW,WeaponPresentationResolver.Family.SHIELD,WeaponPresentationResolver.Family.OTHER)){
            for(int step=1;step<=3;step++){
                var selected=select(step,family,WeaponPresentationResolver.Family.EMPTY,HumanoidArm.RIGHT,false);
                assertEquals(WeaponPresentationResolver.Renderer.VANILLA,selected.renderer());
                assertNull(selected.profileId());
                assertNull(selected.nativeClipId());
            }
        }
    }
    @Test void leftHandedAndOffhandShieldPreserveActualPhysicalAttackingSide(){
        var left=select(1,WeaponPresentationResolver.Family.SWORD,WeaponPresentationResolver.Family.SHIELD,HumanoidArm.LEFT,true);
        assertEquals(HumanoidArm.LEFT,left.activeArm());
        assertEquals(InteractionHand.MAIN_HAND,left.activeHand());
        assertEquals(WeaponPresentationResolver.Family.SHIELD,left.offFamily());
        assertEquals(WeaponPresentationResolver.Renderer.MNAE,left.renderer());
        var emptyShield=select(1,WeaponPresentationResolver.Family.EMPTY,WeaponPresentationResolver.Family.SHIELD,HumanoidArm.LEFT,true);
        assertEquals("mkm:mnae/straight_punch",emptyShield.nativeClipId());
    }
    @Test void dodgeAlwaysUsesLegacyChoreographyRegardlessOfWeaponFamily(){
        var dodge=new ActionTimeline(ActionTimeline.Kind.DODGE,
                ActionTimeline.Phase.DODGING,0,500,500);
        var a=WeaponPresentationResolver.resolveFamilies(dodge,WeaponPresentationResolver.Family.AXE,WeaponPresentationResolver.Family.EMPTY,
                HumanoidArm.LEFT,true);
        assertEquals(WeaponPresentationResolver.Renderer.C3,a.renderer());
        assertEquals("DODGE_C3",a.reason());
        assertNull(a.nativeClipId());
    }
}
