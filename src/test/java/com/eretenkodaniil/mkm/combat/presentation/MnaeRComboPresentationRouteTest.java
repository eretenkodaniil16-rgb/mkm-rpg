package com.eretenkodaniil.mkm.combat.presentation;

import static org.junit.jupiter.api.Assertions.*;
import com.eretenkodaniil.mkm.combat.presentation.AttackPresentationEvent.*;
import org.junit.jupiter.api.Test;

/** No renderer classes or Minecraft client side dependencies required. */
final class MnaeRComboPresentationRouteTest {
    private static WeaponPresentationResolver.HandContext ctx(
            String main,String off,Arm preferred,Hand active) {
        return WeaponPresentationResolver.inspect(
                new WeaponPresentationResolver.Hands(main,off,preferred),active);
    }
    private static MnaeRComboPresentationRoute.Decision route(
            String main,String off,int step,boolean nativeEnabled) {
        return MnaeRComboPresentationRoute.select(
                ctx(main,off,Arm.RIGHT,Hand.MAIN_HAND),step,false,nativeEnabled);
    }
    @Test void f8AffectsExactlyFirstStageNotServerTiming() {
        var a=route("minecraft:iron_sword","minecraft:air",1,false);
        var b=route("minecraft:iron_sword","minecraft:air",1,true);
        assertEquals(MnaeRComboPresentationRoute.Renderer.LEGACY_C3,a.renderer());
        assertEquals("mkm:one_handed_sword",a.legacyProfile());
        assertEquals(MnaeRComboPresentationRoute.Renderer.NATIVE_MNAE,b.renderer());
        assertEquals("mkm:mnae/sword_cut",b.nativeClip());
        for(int step=2;step<=3;step++){
            var later=route("minecraft:iron_sword","minecraft:air",step,true);
            assertEquals(MnaeRComboPresentationRoute.Renderer.LEGACY_C3,later.renderer());
            assertNull(later.nativeClip());
        }
    }
    @Test void allUnsupportedMainItemsNeverProduceC3SwordAttack() {
        for(String item:new String[]{"minecraft:iron_axe","minecraft:mace",
                "minecraft:iron_pickaxe","minecraft:bow","minecraft:crossbow",
                "minecraft:shield","minecraft:stick","othermod:steel_sword"}) {
            for(int step=1;step<=3;step++){
                for(boolean enabled:new boolean[]{false,true}){
                    var decision=route(item,"minecraft:air",step,enabled);
                    assertEquals(MnaeRComboPresentationRoute.Renderer.VANILLA,
                            decision.renderer(),item+" stage "+step);
                    assertEquals(MnaeRComboPresentationRoute.Reason.UNSUPPORTED_FAMILY,
                            decision.reason());
                    assertNull(decision.nativeClip());
                }
            }
        }
    }
    @Test void emptyMainHandPlusOffhandSwordDoesNotForgePunch() {
        for(String off:new String[]{"minecraft:iron_sword","minecraft:iron_axe",
                "minecraft:iron_pickaxe"}) {
            assertEquals(MnaeRComboPresentationRoute.Reason.OFFHAND_GRIP_UNVERIFIED,
                    route("minecraft:air",off,1,true).reason());
            assertEquals(MnaeRComboPresentationRoute.Renderer.VANILLA,
                    route("minecraft:air",off,1,false).renderer());
        }
        assertEquals("mkm:mnae/straight_punch",
                route("minecraft:air","minecraft:air",1,true).nativeClip());
        assertEquals("mkm:mnae/straight_punch",
                route("minecraft:air","minecraft:shield",1,true).nativeClip());
    }
    @Test void offhandAttackAndLeftDominantAreDifferentConcepts() {
        var left=ctx("minecraft:iron_sword","minecraft:air",Arm.LEFT,Hand.MAIN_HAND);
        assertEquals(Arm.LEFT,MnaeRComboPresentationRoute.select(left,1,false,true).attackingArm());
        var off=ctx("minecraft:air","minecraft:iron_sword",Arm.LEFT,Hand.OFF_HAND);
        assertEquals(Arm.RIGHT,off.attackingArm());
        assertEquals(MnaeRComboPresentationRoute.Reason.NOT_MAIN_HAND,
                MnaeRComboPresentationRoute.select(off,1,false,true).reason());
    }
    @Test void dodgeExplicitLegacyAndNeverInfersSwordAttackFamily() {
        var axe=ctx("minecraft:iron_axe","minecraft:air",Arm.LEFT,Hand.MAIN_HAND);
        var d=MnaeRComboPresentationRoute.select(axe,0,true,true);
        assertEquals(MnaeRComboPresentationRoute.Renderer.LEGACY_C3,d.renderer());
        assertEquals(MnaeRComboPresentationRoute.Reason.DODGE_STAYS_C3,d.reason());
        assertNull(d.nativeClip());
    }
}
