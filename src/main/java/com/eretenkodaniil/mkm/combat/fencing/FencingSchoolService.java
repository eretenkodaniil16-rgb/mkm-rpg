package com.eretenkodaniil.mkm.combat.fencing;

import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.CombatModifierContributions;
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
                state(player), school, EquipmentRules.presentationFamily(heldId), ready, tick);
        if (result.applied()) {
            player.setAttached(CombatAttachments.FENCING_SCHOOL, result.state());
        }
        return result;
    }

    /**
     * Called ONLY from server-side EquipmentSnapshots. It is an additional
     * named STATUS source composed BEFORE Character's one final clamp.
     */
    public static Optional<CombatModifierContributions.Contribution> activeContribution(
            ServerPlayer player) {
        FencingSchoolState current = state(player);
        if (!player.isAlive() || player.isSpectator()) {
            return Optional.empty();
        }
        String mainhand = BuiltInRegistries.ITEM.getKey(
                player.getMainHandItem().getItem()).toString();
        return FencingSchoolRules.effectiveSchool(current,
                        EquipmentRules.presentationFamily(mainhand),
                        player.level().getGameTime())
                .flatMap(FencingSchool::contribution);
    }

    /** Death ends a volatile fencing stance; no passive persists on respawn. */
    public static void onPlayerTick(ServerPlayer player) {
        if (!player.isAlive() && state(player).school() != FencingSchool.NONE) {
            player.setAttached(CombatAttachments.FENCING_SCHOOL, new FencingSchoolState());
        }
    }
}
