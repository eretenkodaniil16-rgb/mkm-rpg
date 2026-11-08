package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.combat.mode.CombatModeRules;
import com.eretenkodaniil.mkm.combat.mode.CombatModeService;
import com.eretenkodaniil.mkm.combat.net.HitConfirm;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.equipment.EquipmentSnapshots;
import com.eretenkodaniil.mkm.equipment.PhysicalDamageType;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Combat hooks. Fabric's generic damage event cannot change amount, therefore the
 * direct melee damage scaling is delegated to the minimal LivingEntityCombatMixin.
 */
public final class CombatEvents {
    private static final PendingHitProvenance<LivingEntity, DamageSource> PENDING_HEAVY =
            new PendingHitProvenance<>();

    private CombatEvents() {}

    public static float modifyIncoming(LivingEntity target, DamageSource source, float amount) {
        if (!Float.isFinite(amount) || amount <= 0.0F) return amount;

        if (source.is(DamageTypes.PLAYER_ATTACK)
                && source.getEntity() instanceof ServerPlayer attacker
                && source.getDirectEntity() == attacker) {
            PENDING_HEAVY.begin(target);
            PlayerModifierSnapshot offense = stats(attacker);
            CombatModeRules.Strike modeHit = CombatModeService.currentImpact(attacker);
            // Opt-in native impact already spent stamina exactly once on RELEASE.
            // Never run separate Shift+LMB heavy cost during a queued charge.
            boolean heavy = modeHit == CombatModeRules.Strike.CHARGED
                    || (modeHit == CombatModeRules.Strike.NONE
                        && attacker.isShiftKeyDown() && CombatService.tryHeavy(attacker));
            if (heavy) PENDING_HEAVY.record(target, source, target.level().getGameTime());
            float scaled = CombatRules.scaleIncomingMelee(
                    amount, offense.get(PlayerModifierId.PHYSICAL_POWER),
                    offense.get(PlayerModifierId.MELEE_DAMAGE), heavy);
            if (target instanceof ServerPlayer defender) {
                return PhysicalMitigationRules.apply(
                        scaled, offense, stats(defender),
                        EquipmentSnapshots.heldPhysicalType(attacker));
            }
            return scaled;
        }

        // Defense only: preserve vanilla ranged attack attribution and damage.
        // Computing an offensive bow bonus at impact would permit a weapon-swap exploit.
        if (target instanceof ServerPlayer defender) {
            if (source.is(DamageTypes.MOB_ATTACK)) {
                PhysicalDamageType type = source.getEntity() instanceof LivingEntity attacker
                        && source.getDirectEntity() == attacker
                        ? EquipmentSnapshots.heldPhysicalType(attacker)
                        : PhysicalDamageType.BLUNT;
                return PhysicalMitigationRules.apply(
                        amount, 0.0D, stats(defender), type);
            }
            if (source.is(DamageTypes.ARROW) || source.is(DamageTypes.TRIDENT)) {
                return PhysicalMitigationRules.apply(
                        amount, 0.0D, stats(defender), PhysicalDamageType.PIERCING);
            }
        }
        return amount;
    }

    public static void afterDamage(
            LivingEntity entity, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
        boolean heavy = PENDING_HEAVY.consume(entity, source, entity.level().getGameTime());
        if (blocked) {
            if (entity instanceof ServerPlayer defender) {
                CombatModeService.onVanillaShieldBlock(defender, source, baseDamage);
                ShieldGuardService.onBlocked(defender, baseDamage);
            }
            return;
        }
        if (!Float.isFinite(damageTaken) || damageTaken <= 0.0F
                || !(entity instanceof Mob || entity instanceof ServerPlayer)) {
            return;
        }

        boolean playerMelee = source.is(DamageTypes.PLAYER_ATTACK)
                && source.getEntity() instanceof ServerPlayer
                && source.getDirectEntity() == source.getEntity();
        boolean mobMelee = source.is(DamageTypes.MOB_ATTACK)
                && source.getEntity() instanceof Mob
                && source.getDirectEntity() == source.getEntity();
        if (!playerMelee && !(mobMelee && entity instanceof ServerPlayer)) return;

        double staggerPower = playerMelee
                ? stats((ServerPlayer) source.getEntity()).get(PlayerModifierId.STAGGER_POWER)
                : 1.0D;
        double staggerResistance = entity instanceof ServerPlayer defender
                ? stats(defender).get(PlayerModifierId.STAGGER_RESISTANCE)
                : 1.0D;

        long tick = entity.level().getGameTime();
        PoiseRules.Result result = PoiseRules.onHit(
                entity.getAttachedOrCreate(CombatAttachments.POISE), tick,
                damageTaken, staggerPower, staggerResistance, heavy && playerMelee);
        entity.setAttached(CombatAttachments.POISE, result.state());
        if (playerMelee && source.getEntity() instanceof ServerPlayer actualAttacker) {
            CombatModeService.onConfirmedDamage(actualAttacker, entity, damageTaken, blocked);
        }
        // C3 hit VFX only on verified positive, unblocked damage, while D3 owns provenance.
        if (playerMelee && entity instanceof Mob mob
                && source.getEntity() instanceof ServerPlayer player) {
            ActionTimeline action = player.getAttachedOrCreate(CombatAttachments.TIMELINE);
            if (action.kind() == ActionTimeline.Kind.LIGHT_COMBO
                    && action.phase() == ActionTimeline.Phase.ACTIVE
                    && HitFeedbackRules.validConfirmedHit(mob.getId(), damageTaken)
                    && ServerPlayNetworking.canSend(player, HitConfirm.TYPE)) {
                ServerPlayNetworking.send(player,
                        new HitConfirm(mob.getId(), damageTaken, result.staggered()));
            }
        }
        if (result.staggered()) {
            // A soft stagger, not a server-side attack interrupt or cinematic hit animation.
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 3));
            if (source.getEntity() instanceof ServerPlayer attacker) {
                attacker.sendOverlayMessage(Component.literal("MKM  STAGGER!"));
            } else if (entity instanceof ServerPlayer defender) {
                defender.sendOverlayMessage(Component.literal("MKM  STAGGERED"));
            }
        }
    }

    private static PlayerModifierSnapshot stats(ServerPlayer player) {
        return EquipmentSnapshots.snapshot(
                player, player.getAttachedOrCreate(CharacterAttachments.CHARACTER));
    }

    public static void afterDeath(LivingEntity entity, DamageSource source) {
        PENDING_HEAVY.clear(entity);
    }
}
