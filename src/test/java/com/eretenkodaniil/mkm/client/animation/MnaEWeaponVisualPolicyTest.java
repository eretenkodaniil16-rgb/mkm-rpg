package com.eretenkodaniil.mkm.client.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MnaEWeaponVisualPolicyTest {
    @Test void swordAloneOnlySelectedR07Mainhand(){
        for(String material:new String[]{"wooden","stone","copper","iron","golden","diamond","netherite"})
            assertEquals(MnaEWeaponVisualPolicy.Presentation.SWORD,
                    MnaEWeaponVisualPolicy.select("minecraft:"+material+"_sword","minecraft:air").presentation());
    }
    @Test void offhandItemsBlockUnsupportedMixedPoses(){
        assertEquals(MnaEWeaponVisualPolicy.Presentation.VANILLA,
                MnaEWeaponVisualPolicy.select("minecraft:air","minecraft:diamond_sword").presentation());
        assertEquals(MnaEWeaponVisualPolicy.Presentation.VANILLA,
                MnaEWeaponVisualPolicy.select("minecraft:iron_sword","minecraft:shield").presentation());
        assertEquals(MnaEWeaponVisualPolicy.Presentation.VANILLA,
                MnaEWeaponVisualPolicy.select("minecraft:iron_sword","minecraft:apple").presentation());
    }
    @Test void axePickaxeAndBowNeverUseSword(){
        for(String item:new String[]{"minecraft:iron_axe","minecraft:stone_pickaxe",
                "minecraft:bow","minecraft:crossbow","minecraft:shield","minecraft:trident",
                "minecraft:netherite_hoe","mkm:rpg_blade"})
            assertEquals(MnaEWeaponVisualPolicy.Presentation.VANILLA,
                    MnaEWeaponVisualPolicy.select(item,"minecraft:air").presentation(),item);
        assertEquals(MnaEWeaponVisualPolicy.Presentation.UNARMED,
                MnaEWeaponVisualPolicy.select("minecraft:air","minecraft:air").presentation());
    }
}
