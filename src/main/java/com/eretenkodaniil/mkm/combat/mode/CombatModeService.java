package com.eretenkodaniil.mkm.combat.mode;

import com.eretenkodaniil.mkm.MkmMod;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.CombatRules;
import com.eretenkodaniil.mkm.combat.CombatState;
import com.eretenkodaniil.mkm.combat.fencing.FencingSchool;
import com.eretenkodaniil.mkm.combat.fencing.FencingRiposteRules;
import com.eretenkodaniil.mkm.combat.fencing.FencingSchoolRules;
import com.eretenkodaniil.mkm.combat.fencing.FencingSchoolService;
import com.eretenkodaniil.mkm.combat.net.CombatModeStatus;
import com.eretenkodaniil.mkm.combat.net.CombatModeContact;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import com.eretenkodaniil.mkm.equipment.EquipmentSnapshots;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;

/**
 * Opt-in single-server-authority sword combat-mode vertical slice for QA5/v4.
 * No client-provided entity id, damage, target, duration, position or hit tick.
 * A sword attack goes through player.attack(target) EXACTLY once on contact.
 */
public final class CombatModeService {
    private static final boolean QA_OPT_IN = Boolean.getBoolean("mkm.combat.experimental");
    private static final boolean QA_LOG = Boolean.getBoolean("mkm.combat.qaLog");
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

    private static final class Session {
        CombatModeRules.State state = new CombatModeRules.State();
        CombatPacketBudget.State budget = new CombatPacketBudget.State();
        long nextToggleTick;
        Object world;
        CombatModeRules.Strike resolving = CombatModeRules.Strike.NONE;
        boolean hitConfirmed;
        int expectedTargetId = -1;
        float confirmedDamage;
        long riposteUntil;
    }

    private CombatModeService() {}

    private static Session session(ServerPlayer player) {
        Session s = SESSIONS.computeIfAbsent(player, ignored -> new Session());
        if (s.world != null && s.world != player.level()) {
            s.state = new CombatModeRules.State();
            // The world's gameTime may jump backwards after a dimension switch;
            // per-tick budget and toggle clock belong to that previous world.
            s.budget = new CombatPacketBudget.State();
            s.nextToggleTick = 0L;
            sendStatus(player, false);
            s.riposteUntil = 0L;
            s.resolving = CombatModeRules.Strike.NONE;
            s.hitConfirmed = false;
            s.confirmedDamage = 0F;
            s.expectedTargetId = -1;
        }
        s.world = player.level();
        return s;
    }

    public static void onLogin(ServerPlayer player) {
        SESSIONS.remove(player); // force explicit opt-in per connection
    }

    public static void onDisconnect(ServerPlayer player) {
        SESSIONS.remove(player);
    }

    private static boolean acceptPacket(Session s, long now) {
        var next = CombatPacketBudget.accept(s.budget, now);
        if (!CombatPacketBudget.accepted(s.budget, next)) return false;
        s.budget = next;
        return true;
    }

    public static boolean busy(ServerPlayer player) {
        Session s = SESSIONS.get(player);
        return s != null && s.state.busy();
    }

    public static void toggle(ServerPlayer player, boolean enabled) {
        Session s = session(player);
        long now = player.level().getGameTime();
        if (!acceptPacket(s, now) || now < s.nextToggleTick) return;
        s.nextToggleTick = now + 5L; // key-repeat/flood guard, not a combat cooldown
        boolean admitted = enabled && QA_OPT_IN && player.isAlive()
                && !player.isSpectator();
        s.state = CombatModeRules.mode(s.state, admitted);
        sendStatus(player, admitted);
        player.sendOverlayMessage(Component.literal(admitted
                ? "MKM COMBAT MODE ON | F6 exits | sword + LMB hold/release"
                : (enabled ? "MKM combat QA disabled by server; vanilla LMB active"
                        : "MKM combat mode OFF | vanilla LMB active")));
    }

    private static void sendStatus(ServerPlayer player, boolean enabled) {
        if (ServerPlayNetworking.canSend(player, CombatModeStatus.TYPE)) {
            ServerPlayNetworking.send(player, new CombatModeStatus(enabled));
        }
    }

    private static boolean eligible(ServerPlayer player, String id) {
        return QA_OPT_IN && player.isAlive() && !player.isSpectator()
                && !player.isPassenger() && !player.isUsingItem()
                && player.getAttachedOrCreate(CombatAttachments.TIMELINE).idle()
                && EquipmentRules.profile(id, EquipmentRules.Slot.MAIN_HAND).family()
                        == EquipmentRules.Family.SWORD;
    }

    private static String mainhand(ServerPlayer player) {
        return BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
    }

    /** 0 press / 1 release / 2 cancel; sequence incremented only by client. */
    public static void intent(ServerPlayer player, byte phase, int serial) {
        if (phase < 0 || phase > 2 || serial < 0) return;
        Session s = session(player);
        long now = player.level().getGameTime();
        if (!acceptPacket(s, now)) return;
        var old = s.state;
        String id = mainhand(player);
        boolean allowed = eligible(player, id);
        if (phase == 2) {
            if ((old.phase() == CombatModeRules.Phase.HELD
                    || old.phase() == CombatModeRules.Phase.QUEUED)
                    && old.latestSequence() == serial) s.state = CombatModeRules.cancel(old);
            return;
        }
        if (phase == 0) {
            s.state = CombatModeRules.press(old, serial, now, id, allowed);
            if (s.state != old) player.sendOverlayMessage(Component.literal("MKM | HOLD sword strike…"));
            return;
        }
        var next = CombatModeRules.release(old, serial, now, allowed);
        if (next == old) return;
        // One debit, server-only. Failed spend has NO strike.
        if (next.phase() == CombatModeRules.Phase.QUEUED) {
            PlayerModifierSnapshot stats = EquipmentSnapshots.snapshot(player,
                    player.getAttachedOrCreate(CharacterAttachments.CHARACTER));
            float cost = (float) ((next.strike() == CombatModeRules.Strike.CHARGED ? 22D : 8D)
                    * stats.get(PlayerModifierId.STAMINA_COST));
            long regenDelay = Math.max(1L, Math.round(20D
                    * stats.get(PlayerModifierId.STAMINA_REGEN_DELAY)));
            float max = (float) stats.get(PlayerModifierId.MAX_STAMINA);
            CombatState fresh = CombatRules.regenerate(
                    player.getAttachedOrCreate(CombatAttachments.PLAYER), now, max,
                    (float) stats.get(PlayerModifierId.STAMINA_REGENERATION));
            CombatState paid = CombatRules.combo(fresh, now, cost, regenDelay);
            if (paid == null) {
                s.state = CombatModeRules.cancel(next);
                player.sendOverlayMessage(Component.literal("MKM | NOT ENOUGH STAMINA"));
                return;
            }
            player.setAttached(CombatAttachments.PLAYER, paid);
            s.state = next;
            if (QA_LOG) MkmMod.LOGGER.info(
                    "MKM_COMBAT_QA commit actor={} serial={} kind={} contactTick={} staminaCost={}",
                    player.getId(), next.latestSequence(), next.strike(), next.impactAt(), cost);
            player.sendOverlayMessage(Component.literal(next.strike()
                    == CombatModeRules.Strike.CHARGED
                    ? "MKM CHARGED | committed: 7t windup"
                    : "MKM QUICK | committed: 4t windup"));
        } else {
            s.state = next;
        }
    }

    /** Process server actions once; never invoked from a client animation event. */
    public static void tick(ServerPlayer player) {
        Session s = SESSIONS.get(player);
        if (s == null) return;
        s = session(player); // resets client ACK and sequence on dimension transitions
        if (!s.state.enabled()) return;
        long now = player.level().getGameTime();
        var previous = s.state;
        s.state = CombatModeRules.expire(previous, now);
        if (s.state != previous) {
            player.sendOverlayMessage(Component.literal("MKM | charge expired; no attack"));
        }
        String id = mainhand(player);
        if (s.state.busy() && (!eligible(player, id)
                || !s.state.itemId().equals(id) || s.world != player.level())) {
            s.state = CombatModeRules.cancel(s.state);
            return;
        }
        if (!CombatModeRules.readyToImpact(s.state, now, id, eligible(player, id))) return;
        CombatModeRules.Strike kind = s.state.strike();
        s.state = CombatModeRules.cancel(s.state); // consume before attack
        s.resolving = kind;
        s.hitConfirmed = false;
        s.confirmedDamage = 0F;
        try {
            LivingEntity victim = findTarget(player);
            s.expectedTargetId = victim == null ? -1 : victim.getId();
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            if (victim != null) {
                // Only authoritative native hit. No second DamageSource or MNAE attack.
                player.attack(victim);
            }
            // Exactly ONE S2C contact receipt after the native hit callback returns.
            // A rendered impact marker cannot cause another damage event.
            if (ServerPlayNetworking.canSend(player, CombatModeContact.TYPE)) {
                ServerPlayNetworking.send(player, new CombatModeContact(
                        s.expectedTargetId,
                        kind == CombatModeRules.Strike.CHARGED ? (byte) 2 : (byte) 1,
                        now, s.hitConfirmed, s.confirmedDamage));
            }
            if (QA_LOG) MkmMod.LOGGER.info(
                    "MKM_COMBAT_QA resolve actor={} target={} kind={} confirmed={}",
                    player.getId(), s.expectedTargetId, kind, s.hitConfirmed);
            if (!s.hitConfirmed) player.sendOverlayMessage(Component.literal(
                    "MKM " + kind + " | missed/blocked/no health damage"));
        } finally {
            s.resolving = CombatModeRules.Strike.NONE;
            s.expectedTargetId = -1;
        }
    }

    public static CombatModeRules.Strike currentImpact(ServerPlayer attacker) {
        Session s = SESSIONS.get(attacker);
        return s == null ? CombatModeRules.Strike.NONE : s.resolving;
    }

    /** Prevent vanilla sword sweep secondaries inheriting charged target multiplier. */
    public static boolean isSelectedImpactTarget(ServerPlayer attacker, LivingEntity defender) {
        Session s = SESSIONS.get(attacker);
        return s != null && s.resolving != CombatModeRules.Strike.NONE
                && s.expectedTargetId == defender.getId();
    }

    private static Optional<FencingSchool> school(ServerPlayer player) {
        String id = mainhand(player);
        return FencingSchoolRules.effectiveSchool(
                FencingSchoolService.state(player),
                EquipmentRules.profile(id, EquipmentRules.Slot.MAIN_HAND).family(),
                player.level().getGameTime());
    }

    /** Called once by Fabric's AFTER_DAMAGE callback, not by a cosmetic packet. */
    public static void onConfirmedDamage(ServerPlayer attacker, LivingEntity victim,
                                         DamageSource actualSource, float healthDamage, boolean blocked) {
        Session s = SESSIONS.get(attacker);
        if (s == null || s.resolving == CombatModeRules.Strike.NONE || s.hitConfirmed
                || s.expectedTargetId != victim.getId()
                || blocked || !Float.isFinite(healthDamage) || healthDamage <= 0F) return;
        s.hitConfirmed = true; // one receipt, no duplicated status effect
        s.confirmedDamage = healthDamage;
        if (QA_LOG) MkmMod.LOGGER.info(
                "MKM_COMBAT_QA confirmed actor={} target={} kind={} actualHealthDamage={}",
                attacker.getId(), victim.getId(), s.resolving, healthDamage);
        CombatModeRules.Strike kind = s.resolving;
        FencingSchool style = school(attacker).orElse(FencingSchool.NONE);
        boolean riposte = FencingRiposteRules.riposte(style, kind, true,
                attacker.level().getGameTime(), s.riposteUntil);
        if (riposte) s.riposteUntil = 0L;

        // Real, visible on victim, no additional HP mutation or attack.
        if (kind == CombatModeRules.Strike.CHARGED) {
            victim.knockback(style == FencingSchool.LIECHTENAUER ? 0.95D : 0.60D,
                    attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ(),
                    actualSource, healthDamage);
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 32, 1));
        } else if (riposte) {
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0));
            victim.knockback(0.30D, attacker.getX() - victim.getX(),
                    attacker.getZ() - victim.getZ(), actualSource, 0F);
        } else if (style == FencingSchool.DESTREZA) {
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 0));
        }
        attacker.sendOverlayMessage(Component.literal(riposte ? "FIORE RIPOSTE!"
                : (kind == CombatModeRules.Strike.CHARGED ? "CHARGED IMPACT!"
                        : "QUICK HIT!") + " " + Math.round(healthDamage)));
    }

    /** Called only AFTER vanilla reports a genuine blocked shield hit. */
    public static void onVanillaShieldBlock(ServerPlayer defender, DamageSource source,
                                            float blockedDamage) {
        Session s = SESSIONS.get(defender);
        if (s == null || !Float.isFinite(blockedDamage) || blockedDamage <= 0F) return;
        long validUntil = FencingRiposteRules.openedUntil(
                school(defender).orElse(FencingSchool.NONE), s.state.enabled(),
                true, defender.getUseItem().is(Items.SHIELD), defender.level().getGameTime());
        if (validUntil == 0L) return;
        s.riposteUntil = validUntil;
        defender.sendOverlayMessage(Component.literal("FIORE BLOCK | RIPOSTE READY 40t"));
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 facing = player.getLookAngle().normalize();
        LivingEntity nearest = null;
        double best = CombatTargetRules.RANGE * CombatTargetRules.RANGE;
        for (Entity candidate : player.level().getEntities(player,
                player.getBoundingBox().inflate(CombatTargetRules.RANGE),
                entity -> entity instanceof LivingEntity living && living.isAlive())) {
            if (!(candidate instanceof LivingEntity living)) continue;
            Vec3 toward = living.getBoundingBox().getCenter().subtract(eye);
            double distance = toward.lengthSqr();
            if (distance >= best || !CombatTargetRules.valid(
                    distance, facing.dot(toward.normalize()), player.hasLineOfSight(living))) continue;
            nearest = living;
            best = distance;
        }
        return nearest;
    }
}
