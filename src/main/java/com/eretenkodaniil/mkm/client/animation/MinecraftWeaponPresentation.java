package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.ActionTimeline;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Minecraft-facing item-tag adapter for the pure presentation policy. */
public final class MinecraftWeaponPresentation {
    private MinecraftWeaponPresentation() {}

    public static WeaponPresentationResolver.Family family(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return WeaponPresentationResolver.Family.EMPTY;
        if (stack.is(ItemTags.SWORDS)) return WeaponPresentationResolver.Family.SWORD;
        if (stack.is(ItemTags.AXES)) return WeaponPresentationResolver.Family.AXE;
        if (stack.is(ItemTags.PICKAXES)) return WeaponPresentationResolver.Family.PICKAXE;
        if (stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES))
            return WeaponPresentationResolver.Family.OTHER_TOOL;
        if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW))
            return WeaponPresentationResolver.Family.BOW;
        if (stack.is(Items.SHIELD)) return WeaponPresentationResolver.Family.SHIELD;
        return WeaponPresentationResolver.Family.OTHER;
    }

    public static WeaponPresentationResolver.Choice resolve(
            ActionTimeline timeline, ItemStack main, ItemStack off,
            boolean leftDominant, boolean nativeMode) {
        return WeaponPresentationResolver.resolve(
                family(main), family(off), WeaponPresentationResolver.Hand.MAIN,
                leftDominant, timeline.comboStep(),
                timeline.phase() == ActionTimeline.Phase.DODGING, nativeMode);
    }
}
