package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.presentation.AttackInputPriority;
import com.eretenkodaniil.mkm.combat.presentation.AttackPresentationEvent;
import com.eretenkodaniil.mkm.combat.presentation.AttackSequenceRules;
import com.eretenkodaniil.mkm.equipment.WeaponCombatProfiles;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Owner-local, cosmetic-only feed for the future MNAE bridge.
 * Called from vanilla Minecraft.startAttack BEFORE vanilla executes its logic,
 * without cancelling or replacing the vanilla method.
 */
public final class VanillaAttackPresentationFeed {
    private static final int MAX_EVENTS = 16;
    private static final ArrayDeque<AttackPresentationEvent> EVENTS = new ArrayDeque<>();
    private static AttackSequenceRules.State lastSequence;
    private static int lastPlayerId = -1;

    private VanillaAttackPresentationFeed() {}

    public static void observeNativeAttack(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;
        // Native startAttack also handles block breaking; never reinterpret mining as combat.
        HitResult crosshair = client.hitResult;
        if (crosshair == null) return;
        ActionTimeline timeline = client.player.getAttached(CombatAttachments.TIMELINE);
        var input = client.player.isShiftKeyDown()
                ? AttackInputPriority.Intent.SHIFT_LMB
                : AttackInputPriority.Intent.LMB;
        var context = new AttackInputPriority.Context(
                client.gui.screen() != null,
                client.player.isSpectator(),
                crosshair.getType() == HitResult.Type.BLOCK,
                client.player.isUsingItem(),
                timeline != null && !timeline.idle());
        AttackInputPriority.Decision decision = AttackInputPriority.classify(input, context);
        if (decision != AttackInputPriority.Decision.VANILLA_SWING
                && decision != AttackInputPriority.Decision.EXISTING_HEAVY) return;

        if (client.player.getId() != lastPlayerId) {
            reset();
            lastPlayerId = client.player.getId();
        }
        String weaponId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(client.player.getMainHandItem().getItem()).toString();
        WeaponCombatProfiles.Profile profile = WeaponCombatProfiles.forItem(weaponId);
        long tick = Math.max(0L, client.level.getGameTime());
        AttackSequenceRules.Selection selection = AttackSequenceRules.next(
                lastSequence, profile.family(), tick, profile.maxSequenceSteps(),
                profile.fastChainWindowTicks(), profile.alternatePauseWindowTicks());
        lastSequence = selection.state();
        String animationId = WeaponCombatProfiles.animationFor(
                profile, selection.state().step(), selection.alternatePause(), selection.finisher());
        boolean left = client.player.getMainArm() == HumanoidArm.LEFT;
        AttackPresentationEvent event = new AttackPresentationEvent(
                decision == AttackInputPriority.Decision.EXISTING_HEAVY
                        ? AttackPresentationEvent.ActionKind.HEAVY
                        : AttackPresentationEvent.ActionKind.VANILLA_SWING,
                profile.family(),
                AttackPresentationEvent.Hand.MAIN_HAND,
                left ? AttackPresentationEvent.Arm.LEFT : AttackPresentationEvent.Arm.RIGHT,
                selection.state().step(), animationId, tick, selection.state().sequenceId(),
                AttackPresentationEvent.Outcome.LOCAL_PREDICTED);
        if (EVENTS.size() == MAX_EVENTS) EVENTS.removeFirst();
        EVENTS.addLast(event);
    }

    /** MNAE can drain once per client render/update; unconsumed events stay bounded. */
    public static List<AttackPresentationEvent> drain() {
        List<AttackPresentationEvent> events = new ArrayList<>(EVENTS);
        EVENTS.clear();
        return List.copyOf(events);
    }

    public static void reset() {
        EVENTS.clear();
        lastSequence = null;
        lastPlayerId = -1;
    }
}
