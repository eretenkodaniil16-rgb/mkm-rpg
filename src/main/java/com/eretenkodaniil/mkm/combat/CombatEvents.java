package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.equipment.EquipmentSnapshots;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import java.util.Map;
import java.util.WeakHashMap;
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
    private static final Map<LivingEntity, Boolean> PENDING_HEAVY = new WeakHashMap<>();

    private CombatEvents() {}

    public static float modifyIncoming(LivingEntity target, DamageSource source, float amount) {
        if (!source.is(DamageTypes.PLAYER_ATTACK)
                || !(source.getEntity() instanceof ServerPlayer attacker)
                || source.getDirectEntity() != attacker
                || amount <= 0F) {
            return amount;
        }

        PlayerModifierSnapshot modifiers = EquipmentSnapshots.snapshot(
                attacker, attacker.getAttachedOrCreate(CharacterAttachments.CHARACTER));
        boolean heavy = attacker.isShiftKeyDown() && CombatService.tryHeavy(attacker);
        if (heavy) {
            PENDING_HEAVY.put(target, Boolean.TRUE);
        }
        float scaled = CombatRules.scaleIncomingMelee(
                amount,
                modifiers.get(PlayerModifierId.PHYSICAL_POWER),
                modifiers.get(PlayerModifierId.MELEE_DAMAGE),
                heavy);
        // Separate MKM resistance before Minecraft handles shields and armor.
        // No duplicate vanilla armor calculation and no secret hit roll.
        if (target instanceof ServerPlayer defender) {
            PlayerModifierSnapshot defense = EquipmentSnapshots.snapshot(
                    defender, defender.getAttachedOrCreate(CharacterAttachments.CHARACTER));
            return PhysicalMitigationRules.apply(
                    scaled, modifiers, defense, EquipmentSnapshots.heldPhysicalType(attacker));
        }
        return scaled;
    }

    public static void afterDamage(
            LivingEntity entity, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
        boolean heavy = PENDING_HEAVY.remove(entity) != null;
        if (blocked || damageTaken <= 0F || !(entity instanceof Mob mob)
                || !(source.getEntity() instanceof ServerPlayer player)
                || source.getDirectEntity() != player
                || !source.is(DamageTypes.PLAYER_ATTACK)) {
            return;
        }

        PlayerModifierSnapshot modifiers = EquipmentSnapshots.snapshot(
                player, player.getAttachedOrCreate(CharacterAttachments.CHARACTER));
        long tick = player.level().getGameTime();
        PoiseRules.Result result = PoiseRules.onHit(
                mob.getAttachedOrCreate(CombatAttachments.POISE), tick,
                damageTaken, modifiers.get(PlayerModifierId.STAGGER_POWER), heavy);
        mob.setAttached(CombatAttachments.POISE, result.state());
        if (result.staggered()) {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 3));
            player.sendOverlayMessage(Component.literal("MKM  STAGGER!"));
        }
    }

    public static void afterDeath(LivingEntity entity, DamageSource source) {
        PENDING_HEAVY.remove(entity);
    }
}
