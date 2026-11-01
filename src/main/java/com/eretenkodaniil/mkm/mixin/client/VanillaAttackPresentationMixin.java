package com.eretenkodaniil.mkm.mixin.client;

import com.eretenkodaniil.mkm.client.animation.VanillaAttackPresentationFeed;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Observe the real vanilla LMB invocation without cancellation or extra attack.
 * No mining, block breaking, attack cooldown or item interaction is replaced.
 */
@Mixin(Minecraft.class)
abstract class VanillaAttackPresentationMixin {
    @Inject(method = "startAttack", at = @At("RETURN"))
    private void mkm$observeLmb(CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            VanillaAttackPresentationFeed.observeNativeAttack((Minecraft) (Object) this);
        }
    }
}
