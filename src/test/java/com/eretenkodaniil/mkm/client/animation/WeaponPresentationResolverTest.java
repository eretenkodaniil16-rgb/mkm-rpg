package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.ActionTimeline;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

class WeaponPresentationResolverTest {
    @BeforeAll static void bootstrapRegistries() {
        // Pure JUnit does not launch Minecraft; item/tag registries need bootstrap
        // before touching the static Items class in deterministic fixtures.
        net.minecraft.server.Bootstrap.bootStrap();
    }
    private static ActionTimeline combo(int stage) {
        return new ActionTimeline(ActionTimeline.Kind.LIGHT_COMBO,
                ActionTimeline.Phase.WINDUP,stage,500,550);
    }
    private static ItemStack item(net.minecraft.world.item.Item item){
        return new ItemStack(item);
    }
    private static WeaponPresentationResolver.Selection select(
            int stage, ItemStack main,ItemStack off,HumanoidArm arm,boolean nativeMode) {
        return WeaponPresentationResolver.resolve(combo(stage),main,off,arm,nativeMode);
    }
    @Test void swordAndUnarmedRouteExclusivelyToTheirTrueProfiles(){
        for(int stage=1;stage<=3;stage++){
            var sword=select(stage,item(Items.IRON_SWORD),ItemStack.EMPTY,HumanoidArm.RIGHT,true);
            assertEquals(WeaponPresentationResolver.Family.SWORD,sword.mainFamily());
            assertEquals(InteractionHand.MAIN_HAND,sword.activeHand());
            assertEquals(stage==1?WeaponPresentationResolver.Renderer.MNAE
                    :WeaponPresentationResolver.Renderer.C3,sword.renderer());
            assertEquals("mkm:one_handed_sword",sword.profileId());
            assertEquals(stage==1?"mkm:mnae/sword_cut":null,sword.nativeClipId());
            var empty=select(stage,ItemStack.EMPTY,ItemStack.EMPTY,HumanoidArm.RIGHT,true);
            assertEquals("mkm:unarmed",empty.profileId());
            assertEquals(stage==1?"mkm:mnae/straight_punch":null,empty.nativeClipId());
        }
        assertEquals(WeaponPresentationResolver.Renderer.C3,
            select(1,item(Items.IRON_SWORD),ItemStack.EMPTY,HumanoidArm.RIGHT,false).renderer());
    }
    @Test void offhandOnlySwordNeverMasqueradesAsMainhandUnarmedOrSword(){
        var result=select(1,ItemStack.EMPTY,item(Items.DIAMOND_SWORD),HumanoidArm.RIGHT,true);
        assertEquals(WeaponPresentationResolver.Family.EMPTY,result.mainFamily());
        assertEquals(WeaponPresentationResolver.Family.SWORD,result.offFamily());
        assertEquals(WeaponPresentationResolver.Renderer.VANILLA,result.renderer());
        assertEquals("OFFHAND_ONLY",result.reason());
        assertNull(result.profileId());
        assertNull(result.nativeClipId());
    }
    @Test void axePickaxeAndBowNeverRouteToSwordEvenInC3Fallback(){
        for(var i: java.util.List.of(Items.IRON_AXE,Items.DIAMOND_PICKAXE,Items.BOW,Items.SHIELD)){
            var selection=select(2,item(i),ItemStack.EMPTY,HumanoidArm.RIGHT,false);
            assertEquals(WeaponPresentationResolver.Renderer.VANILLA,selection.renderer());
            assertNull(selection.profileId());
            assertNull(selection.nativeClipId());
            assertNotEquals(WeaponPresentationResolver.Family.SWORD,selection.mainFamily());
        }
    }
    @Test void leftHandedAndOffhandShieldPreserveActualPhysicalAttackingSide(){
        var left=select(1,item(Items.IRON_SWORD),item(Items.SHIELD),HumanoidArm.LEFT,true);
        assertEquals(HumanoidArm.LEFT,left.activeArm());
        assertEquals(InteractionHand.MAIN_HAND,left.activeHand());
        assertEquals(WeaponPresentationResolver.Family.SHIELD,left.offFamily());
        assertEquals(WeaponPresentationResolver.Renderer.MNAE,left.renderer());
        var emptyShield=select(1,ItemStack.EMPTY,item(Items.SHIELD),HumanoidArm.LEFT,true);
        assertEquals("mkm:mnae/straight_punch",emptyShield.nativeClipId());
    }
    @Test void dodgeAlwaysUsesLegacyChoreographyRegardlessOfWeaponFamily(){
        var dodge=new ActionTimeline(ActionTimeline.Kind.DODGE,
                ActionTimeline.Phase.DODGING,0,500,500);
        var a=WeaponPresentationResolver.resolve(dodge,item(Items.IRON_AXE),ItemStack.EMPTY,
                HumanoidArm.LEFT,true);
        assertEquals(WeaponPresentationResolver.Renderer.C3,a.renderer());
        assertEquals("DODGE_C3",a.reason());
        assertNull(a.nativeClipId());
    }
}
