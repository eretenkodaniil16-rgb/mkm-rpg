package com.eretenkodaniil.mkm.environment;

import com.eretenkodaniil.mkm.character.CharacterRewardRules;
import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginAttachments;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginData;
import com.eretenkodaniil.mkm.registry.MkmBlocks;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Real in-world right click: one existing server-authoritative XP path.
 * This never spawns encounter mobs, heals enemies, triggers an attack,
 * handles C2S custom payloads or invents any additional Character ledger.
 */
public final class RuneSlateInteraction {
    private static final String REWARD_ID = "mkm:showcase/watchtower_rune";

    private RuneSlateInteraction() {}

    public static void initialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND
                    || level.getBlockState(hit.getBlockPos()).getBlock() != MkmBlocks.RUNE_SLATE)
                return InteractionResult.PASS;
            // Accept the client interaction packet, but change XP and effects on the server only.
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer serverPlayer)
                    || !(level instanceof ServerLevel serverLevel)
                    || !serverPlayer.isAlive() || serverPlayer.isSpectator())
                return InteractionResult.FAIL;
            if (!serverLevel.getAttachedOrCreate(WatchtowerAttachments.SITES)
                    .verifiedRune(hit.getBlockPos())) {
                serverPlayer.sendSystemMessage(Component.translatable("message.mkm.showcase.unverified"));
                return InteractionResult.SUCCESS;
            }
            CharacterOriginData origin = serverPlayer.getAttached(CharacterOriginAttachments.PROFILE);
            if (origin == null || origin.schemaVersion() != CharacterOriginData.SCHEMA_VERSION
                    || !origin.selected()) {
                serverPlayer.sendSystemMessage(Component.translatable("message.mkm.showcase.select"));
                return InteractionResult.SUCCESS;
            }
            RuneLoreRules.Discovery description =
                    RuneLoreRules.discover(origin.raceId(), origin.classId());
            if (description == null) {
                serverPlayer.sendSystemMessage(Component.translatable("message.mkm.showcase.unknown"));
                return InteractionResult.SUCCESS;
            }
            var result = CharacterService.grantExperienceOnce(
                    serverPlayer, REWARD_ID, RuneLoreRules.REWARD_XP);
            if (!result.confirmed()) {
                serverPlayer.sendSystemMessage(Component.translatable("message.mkm.showcase.refused"));
                return InteractionResult.SUCCESS;
            }
            serverPlayer.sendSystemMessage(Component.translatable(description.loreKey()));
            if (result.status() == CharacterRewardRules.Status.ALREADY_GRANTED) {
                serverPlayer.sendSystemMessage(Component.translatable("message.mkm.showcase.revisited"));
                return InteractionResult.SUCCESS;
            }
            serverPlayer.sendSystemMessage(Component.translatable(description.classKey()));
            // One-shot level-independent class effect. Repeating the click never refreshes it.
            if (description.nightVision()) {
                serverPlayer.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 50, 0));
            } else {
                serverPlayer.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 15, 0));
            }
            // Small low-cost server cue; client can still switch off ambient rune effects.
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    hit.getBlockPos().getX() + 0.5, hit.getBlockPos().getY() + 1.1,
                    hit.getBlockPos().getZ() + 0.5, 12, 0.28, 0.18, 0.28, 0.015);
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.mkm.showcase.reward", result.creditedExperience()));
            return InteractionResult.SUCCESS;
        });
    }
}
