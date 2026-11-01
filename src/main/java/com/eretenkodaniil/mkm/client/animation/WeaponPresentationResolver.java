package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.ActionTimeline;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Objects;

/**
 * Only presentation routing; cannot attack, deal damage or consume stamina.
 * R combo targets MAIN_HAND. Offhand inventory must not silently become the
 * attacking item. Active physical arm comes from the avatar main-arm setting.
 */
public final class WeaponPresentationResolver {
    public enum Family { EMPTY, SWORD, AXE, PICKAXE, BOW, SHIELD, OTHER }
    public enum Renderer { MNAE, C3, VANILLA }
    public record Selection(Family mainFamily, Family offFamily, InteractionHand activeHand,
                            HumanoidArm activeArm, Renderer renderer, String profileId,
                            String nativeClipId, String reason, int comboStep) {
        public Selection {
            Objects.requireNonNull(mainFamily); Objects.requireNonNull(offFamily);
            Objects.requireNonNull(activeHand); Objects.requireNonNull(activeArm);
            Objects.requireNonNull(renderer); Objects.requireNonNull(reason);
        }
        public String selectedRenderer() { return renderer.name(); }
    }

    private WeaponPresentationResolver() {}

    public static Family family(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Family.EMPTY;
        if (stack.is(ItemTags.SWORDS)) return Family.SWORD;
        if (stack.is(ItemTags.AXES)) return Family.AXE;
        if (stack.is(ItemTags.PICKAXES)) return Family.PICKAXE;
        if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) return Family.BOW;
        if (stack.is(Items.SHIELD)) return Family.SHIELD;
        return Family.OTHER;
    }

    /**
     * Authoritative R timeline has no offhand-attack variant: MAIN_HAND is
     * explicitly the source. Never infer an offhand attack from its inventory.
     */
    public static Selection resolve(ActionTimeline timeline, ItemStack main, ItemStack off,
                                    HumanoidArm mainArm, boolean nativeEnabled) {
        Family m = family(main), o = family(off);
        HumanoidArm arm = mainArm == null ? HumanoidArm.RIGHT : mainArm;
        if (timeline == null || timeline.idle()) {
            return new Selection(m,o,InteractionHand.MAIN_HAND,arm,Renderer.VANILLA,
                    null,null,"IDLE",0);
        }
        int stage = timeline.comboStep();
        if (timeline.phase() == ActionTimeline.Phase.DODGING) {
            return new Selection(m,o,InteractionHand.MAIN_HAND,arm,Renderer.C3,
                    AnimationController.FIRST_PROFILE,null,"DODGE_C3",stage);
        }
        if (timeline.kind() != ActionTimeline.Kind.LIGHT_COMBO
                || stage < 1 || stage > 3) {
            return new Selection(m,o,InteractionHand.MAIN_HAND,arm,Renderer.VANILLA,
                    null,null,"UNSUPPORTED_ACTION",stage);
        }
        if (m == Family.EMPTY && o != Family.EMPTY && o != Family.SHIELD) {
            // Offhand weapon must not swing invisibly alongside an unarmed jab.
            return new Selection(m,o,InteractionHand.MAIN_HAND,arm,Renderer.VANILLA,
                    null,null,"OFFHAND_ONLY",stage);
        }
        if (m != Family.EMPTY && m != Family.SWORD) {
            return new Selection(m,o,InteractionHand.MAIN_HAND,arm,Renderer.VANILLA,
                    null,null,"NO_AUTHORED_"+m.name(),stage);
        }
        String profile=m==Family.SWORD
                ? AnimationController.FIRST_PROFILE : AnimationController.UNARMED_PROFILE;
        if (!nativeEnabled) {
            return new Selection(m,o,InteractionHand.MAIN_HAND,arm,Renderer.C3,
                    profile,null,"AB_LEGACY",stage);
        }
        if (stage != 1) {
            return new Selection(m,o,InteractionHand.MAIN_HAND,arm,Renderer.C3,
                    profile,null,"NOT_FIRST_COMBO",stage);
        }
        return new Selection(m,o,InteractionHand.MAIN_HAND,arm,Renderer.MNAE,
                profile,m==Family.SWORD ? "mkm:mnae/sword_cut"
                        : "mkm:mnae/straight_punch","NATIVE_DEMO",stage);
    }
}
