package com.eretenkodaniil.mkm.mixin;

import com.eretenkodaniil.mkm.combat.CombatEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Only direct player melee is modified; vanilla shields/armor then process the scaled damage. */
@Mixin(LivingEntity.class)
abstract class LivingEntityCombatMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float mkm$physicalDamage(float incoming, ServerLevel level, DamageSource source) {
        return CombatEvents.modifyIncoming((LivingEntity) (Object) this, source, incoming);
    }
}
