package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * First Action-RPG damage/stagger integration: Minecraft resolves hits and protection;
 * MKM scales valid direct player melee strikes and adds transient poise pressure.
 */
public final class CombatEvents {
    private static final Map<DamageContainer, Boolean> HEAVY_HITS = new WeakHashMap<>();

    private CombatEvents() {}

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player
                || event.getAmount() <= 0.0F) {
            return;
        }

        PlayerModifierSnapshot modifiers = PlayerModifierCalculator.calculate(
                player.getData(CharacterAttachments.CHARACTER));
        boolean heavy = player.isShiftKeyDown() && CombatService.tryHeavy(player);
        if (heavy) {
            HEAVY_HITS.put(event.getContainer(), Boolean.TRUE);
        }
        event.setAmount(CombatRules.scaleIncomingMelee(
                event.getAmount(),
                modifiers.get(PlayerModifierId.PHYSICAL_POWER),
                modifiers.get(PlayerModifierId.MELEE_DAMAGE), heavy));
    }

    public static void onDamagePre(LivingDamageEvent.Pre event) {
        boolean heavy = HEAVY_HITS.remove(event.getContainer()) != null;
        if (!(event.getEntity() instanceof Mob target)
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || event.getSource().getDirectEntity() != attacker
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || event.getNewDamage() <= 0.0F) {
            return;
        }

        PlayerModifierSnapshot modifiers = PlayerModifierCalculator.calculate(
                attacker.getData(CharacterAttachments.CHARACTER));
        long now = attacker.level().getGameTime();
        PoiseRules.Result result = PoiseRules.onHit(
                target.getData(CombatAttachments.POISE),
                now,
                event.getNewDamage(),
                modifiers.get(PlayerModifierId.STAGGER_POWER),
                heavy);
        target.setData(CombatAttachments.POISE, result.state());
        if (result.staggered()) {
            // Prototype stagger: strong temporary slowdown. A true attack-animation interrupt
            // will require validated AI/animation hooks rather than an unsafe entity freeze.
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 3));
            attacker.sendOverlayMessage(Component.literal("MKM  STAGGER!"));
        }
    }
}
