package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginService;
import com.eretenkodaniil.mkm.character.origin.OriginShowcaseRules;
import com.eretenkodaniil.mkm.character.origin.OriginProgressService;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.eretenkodaniil.mkm.equipment.EquipmentSnapshots;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.Locale;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Runtime stamina and movement authority. Player combat attachments are volatile and
 * owner-synchronized; clients never choose cost, cooldown or displacement.
 */
public final class CombatService {
    private static final long SYNC_INTERVAL_TICKS = 5L;

    private CombatService() {}

    public static void onLogin(ServerPlayer player) {
        if (player.getAttached(CombatAttachments.PLAYER) == null) {
            player.setAttached(CombatAttachments.PLAYER, new CombatState());
        }
        if (player.getAttached(CombatAttachments.TIMELINE) == null) {
            player.setAttached(CombatAttachments.TIMELINE, new ActionTimeline());
        }
    }

    public static void onPlayerTick(ServerPlayer player) {
        long now = player.level().getGameTime();
        tickAction(player, now);
        if (now % SYNC_INTERVAL_TICKS != 0L) {
            return;
        }

        PlayerModifierSnapshot modifiers = modifiers(player);
        float max = (float) modifiers.get(PlayerModifierId.MAX_STAMINA);
        float regen = (float) modifiers.get(PlayerModifierId.STAMINA_REGENERATION);
        CombatState current = player.getAttachedOrCreate(CombatAttachments.PLAYER);
        // Do not send attachment updates when only the bookkeeping timestamp changed.
        if (Math.abs(current.stamina() - max) < 0.001F) {
            return;
        }
        CombatState next = CombatRules.regenerate(current, now, max, regen);
        if (Math.abs(next.stamina() - current.stamina()) > 0.001F) {
            player.setAttached(CombatAttachments.PLAYER, next);
        }
    }

    /**
     * Enter the next combo stage on demand. R cannot deliver arbitrary client
     * damage: stamina, timing and the eventual target are server-controlled.
     */
    public static void tryCombo(ServerPlayer player, byte kind) {
        if (kind != 1 || player.isSpectator() || !player.isAlive()
                || player.isPassenger()) {
            return;
        }

        long now = player.level().getGameTime();
        ActionTimeline previous = player.getAttachedOrCreate(CombatAttachments.TIMELINE);
        ActionTimeline next = ActionTimelineRules.beginCombo(previous, now);
        if (next == null) return;

        PlayerModifierSnapshot stats = modifiers(player);
        CombatState current = fresh(player, stats, now);
        float cost = (float) (CombatRules.COMBO_BASE_COST
                * stats.get(PlayerModifierId.STAMINA_COST));
        CombatState paid = CombatRules.combo(current, now, cost, regenerationDelay(stats));
        if (paid == null) return;

        player.setAttached(CombatAttachments.PLAYER, paid);
        player.setAttached(CombatAttachments.TIMELINE, next);
        player.sendOverlayMessage(Component.literal(
                "MKM Combo " + next.comboStep() + "/3 - WINDUP"));
    }

    private static void tickAction(ServerPlayer player, long now) {
        ActionTimeline current = player.getAttachedOrCreate(CombatAttachments.TIMELINE);
        if (!player.isAlive() || player.isSpectator()) {
            if (!current.idle()) player.setAttached(CombatAttachments.TIMELINE, new ActionTimeline());
            return;
        }
        ActionTimeline next = ActionTimelineRules.advance(current, now);
        if (next == current) return;
        player.setAttached(CombatAttachments.TIMELINE, next);

        // A single ACTIVE transition is the only point where the combo can hit.
        if (current.phase() == ActionTimeline.Phase.WINDUP
                && next.phase() == ActionTimeline.Phase.ACTIVE) {
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            hitServerSelectedMob(player);
        }
    }

    /**
     * The client never submits an entity ID. For C2.0 only a living hostile/
     * neutral mob in line of sight, close range and inside the forward cone
     * is eligible. This keeps vanilla attack resolution and collision rules.
     */
    private static void hitServerSelectedMob(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 facing = player.getLookAngle().normalize();
        Mob nearest = null;
        double nearestSquared = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(
                player, player.getBoundingBox().inflate(3.5D),
                candidate -> candidate instanceof Mob && candidate.isAlive())) {
            Mob mob = (Mob) entity;
            if (!player.hasLineOfSight(mob)) continue;
            Vec3 toTarget = mob.getBoundingBox().getCenter().subtract(eye);
            double distanceSquared = toTarget.lengthSqr();
            if (distanceSquared > 3.5D * 3.5D || distanceSquared < 0.0001D) continue;
            if (facing.dot(toTarget.normalize()) < 0.75D) continue;
            if (distanceSquared < nearestSquared) {
                nearest = mob;
                nearestSquared = distanceSquared;
            }
        }
        if (nearest != null) {
            // Vanilla applies weapon damage, armor, shields, attack strength,
            // enchantments, i-frames and our existing melee damage mixin.
            player.attack(nearest);
        }
    }

    /**
     * Warrior-only brace, owned by Combat. Reuses CombatRules.heavy for
     * server stamina debit and the existing timeline interlock. No extra damage
     * event and no pose/animation state is stored in CharacterData.
     */
    public static boolean tryWarriorBrace(ServerPlayer player) {
        boolean warrior = OriginShowcaseRules.allowedClassAction(
                CharacterOriginService.get(player), "mkm:warrior");
        boolean shield = player.getMainHandItem().is(Items.SHIELD)
                || player.getOffhandItem().is(Items.SHIELD);
        long now = player.level().getGameTime();
        long ready = player.getAttachedOrCreate(CombatAttachments.BRACE_READY_TICK);
        if (!WarriorBraceRules.eligible(warrior,
                player.isAlive() && !player.isSpectator() && !player.isPassenger(),
                player.onGround(), shield,
                player.getAttachedOrCreate(CombatAttachments.TIMELINE).idle(), now, ready)) {
            return false;
        }
        PlayerModifierSnapshot stats = modifiers(player);
        CombatState current = fresh(player, stats, now);
        float cost = (float) (WarriorBraceRules.BASE_COST
                * stats.get(PlayerModifierId.STAMINA_COST));
        CombatState paid = CombatRules.heavy(current, now, cost, regenerationDelay(stats));
        if (paid == null) return false;
        player.setAttached(CombatAttachments.PLAYER, paid);
        player.setAttached(CombatAttachments.BRACE_READY_TICK,
                now + WarriorBraceRules.COOLDOWN_TICKS);
        int duration = WarriorBraceRules.duration(OriginProgressService.mastered(player));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 0));
        player.sendOverlayMessage(Component.literal(
                "[Воин] Защитная стойка: сопротивление, -" + Math.round(cost) + " выносливости"));
        return true;
    }

    public static boolean tryHeavy(ServerPlayer player) {
        if (!player.getAttachedOrCreate(CombatAttachments.TIMELINE).idle()) return false;
        long now = player.level().getGameTime();
        PlayerModifierSnapshot modifiers = modifiers(player);
        CombatState current = fresh(player, modifiers, now);
        float cost = (float) (CombatRules.HEAVY_BASE_COST
                * modifiers.get(PlayerModifierId.STAMINA_COST));
        CombatState next = CombatRules.heavy(current, now, cost, regenerationDelay(modifiers));
        if (next == null) {
            return false;
        }
        player.setAttached(CombatAttachments.PLAYER, next);
        player.sendOverlayMessage(Component.literal(String.format(
                Locale.ROOT, "Heavy strike  -%.0f stamina", cost)));
        return true;
    }

    public static void tryDodge(ServerPlayer player, byte direction) {
        // A one-byte packet is just an intent. Never trust a client-supplied velocity or position.
        if (direction < 0 || direction > DodgeDirection.LAST
                || player.isSpectator() || player.isPassenger() || !player.onGround()
                || !player.getAttachedOrCreate(CombatAttachments.TIMELINE).idle()) {
            return;
        }
        long now = player.level().getGameTime();
        PlayerModifierSnapshot modifiers = modifiers(player);
        CombatState current = fresh(player, modifiers, now);
        float cost = (float) (CombatRules.DODGE_BASE_COST
                * modifiers.get(PlayerModifierId.DODGE_COST)
                * modifiers.get(PlayerModifierId.STAMINA_COST));
        CombatState next = CombatRules.dodge(current, now, cost, regenerationDelay(modifiers));
        if (next == null) {
            return;
        }

        // Clients simulate local-player movement. Updating only the server-side
        // velocity was nearly invisible to the controlling player.
        //
        // A bounded impulse is computed from SERVER facing and SERVER modifiers;
        // the client cannot choose a velocity, distance or stamina cost.
        Vec3 look = player.getLookAngle();
        DodgeMotion motion = DodgeMotion.fromFacing(
                look.x, look.z, direction, modifiers.get(PlayerModifierId.DODGE_DISTANCE));
        if (motion == null) {
            return;
        }
        double vertical = Math.max(0.06D, player.getDeltaMovement().y);
        player.setDeltaMovement(motion.x(), vertical, motion.z());
        // Send motion explicitly to the owner. Normal entity tracking does not
        // reliably transmit the player's own new velocity to their local client.
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.setAttached(CombatAttachments.PLAYER, next);
        player.setAttached(CombatAttachments.TIMELINE, ActionTimelineRules.beginDodge(
                player.getAttachedOrCreate(CombatAttachments.TIMELINE), now, direction));
        player.sendOverlayMessage(Component.literal(String.format(
                Locale.ROOT, "Dodge %s  -%.0f stamina", DodgeDirection.label(direction), cost)));
    }

    private static CombatState fresh(
            ServerPlayer player, PlayerModifierSnapshot modifiers, long now) {
        return CombatRules.regenerate(
                player.getAttachedOrCreate(CombatAttachments.PLAYER), now,
                (float) modifiers.get(PlayerModifierId.MAX_STAMINA),
                (float) modifiers.get(PlayerModifierId.STAMINA_REGENERATION));
    }

    private static long regenerationDelay(PlayerModifierSnapshot modifiers) {
        return Math.max(1L, Math.round(
                20.0D * modifiers.get(PlayerModifierId.STAMINA_REGEN_DELAY)));
    }

    private static PlayerModifierSnapshot modifiers(ServerPlayer player) {
        return EquipmentSnapshots.snapshot(
                player, player.getAttachedOrCreate(CharacterAttachments.CHARACTER));
    }
}
