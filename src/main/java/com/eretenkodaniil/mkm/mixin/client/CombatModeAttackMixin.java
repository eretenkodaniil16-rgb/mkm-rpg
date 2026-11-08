package com.eretenkodaniil.mkm.mixin.client;

import com.eretenkodaniil.mkm.client.CombatModeClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Only an explicitly server-ACKed sword-vs-entity press is redirected.
 * BLOCK hitResult, GUI clicks and ordinary vanilla mode bypass unchanged.
 */
@Mixin(Minecraft.class)
abstract class CombatModeAttackMixin {
    /**
     * Minecraft continues breaking blocks while attack is held by a separate
     * vanilla loop. Suppress that loop ONLY during an already accepted
     * sword-vs-LivingEntity press; leave every original BLOCK press untouched.
     */
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void mkm$preventHeldEntityAttackFromMining(boolean attackKeyDown, CallbackInfo ci) {
        if (attackKeyDown && CombatModeClient.isHoldingEntityStrike()) ci.cancel();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void mkm$experimentalSwordPress(CallbackInfoReturnable<Boolean> cir) {
        if (CombatModeClient.interceptAttack((Minecraft) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
