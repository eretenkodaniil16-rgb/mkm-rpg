package com.eretenkodaniil.mkm.combat.fencing;

import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-owned school selection and passive source. Uses a synchronized,
 * transient attachment (no CharacterData migrations, no client-set packet).
 */
public final class FencingSchoolService {
    private FencingSchoolService() {}

    public static FencingSchoolState state(ServerPlayer player) {
        return player.getAttachedOrCreate(CombatAttachments.FENCING_SCHOOL);
    }

    public static FencingSchoolRules.Result select(ServerPlayer player, FencingSchool school) {
        ActionTimeline timeline = player.getAttachedOrCreate(CombatAttachments.TIMELINE);
        boolean ready = player.isAlive() && !player.isSpectator()
                && !player.isPassenger() && !player.isUsingItem() && timeline.idle();
        long tick = player.level().getGameTime();
        String heldId = BuiltInRegistries.ITEM.getKey(
                player.getMainHandItem().getItem()).toString();
        FencingSchoolRules.Result result = FencingSchoolRules.select(
                state(player), school, EquipmentRules.profile(heldId, EquipmentRules.Slot.MAIN_HAND).family(), ready, tick);
        if (result.applied()) {
            player.setAttached(CombatAttachments.FENCING_SCHOOL, result.state());
        }
        return result;
    }

    /**
     * Called ONLY from server-side EquipmentSnapshots. It is an additional
     * named STATUS source composed BEFORE Character's one final clamp.
     */
    public static java.util.Map<com.eretenkodaniil.mkm.character.attribute.PlayerModifierId, Double>
            activeDeltas(ServerPlayer player) {
        FencingSchoolState current = state(player);
        if (!player.isAlive() || player.isSpectator()) return java.util.Map.of();
        String mainhand = BuiltInRegistries.ITEM.getKey(
                player.getMainHandItem().getItem()).toString();
        return FencingSchoolRules.effectiveSchool(current,
                EquipmentRules.profile(mainhand, EquipmentRules.Slot.MAIN_HAND).family(),
                player.level().getGameTime())
                .map(FencingSchool::additiveDeltas).orElse(java.util.Map.of());
    }

    /** Death ends a volatile fencing stance; no passive persists on respawn. */
    public static void onPlayerTick(ServerPlayer player) {
        if (!player.isAlive() && state(player).school() != FencingSchool.NONE) {
            player.setAttached(CombatAttachments.FENCING_SCHOOL, new FencingSchoolState());
        }
    }
}
