package com.eretenkodaniil.mkm.character;

import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/** Real combat-earned MKM experience. Independent of vanilla XP and loot. */
public final class CharacterEvents {
    /** Weak identity keys prevent repeated death callbacks awarding the same victim twice. */
    private static final WeakHashMap<LivingEntity, Boolean> REWARDED_DEATHS = new WeakHashMap<>();
    private static final long HOSTILE_KILL_XP = 25L;

    private CharacterEvents() {}

    public static void onPlayerLoggedIn(ServerPlayer player) {
        CharacterService.ensureAndSync(player);
    }

    public static void onEntityDeath(LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel)
                || !(victim instanceof Enemy)
                || !(source.getEntity() instanceof ServerPlayer player)
                || player == victim
                || REWARDED_DEATHS.putIfAbsent(victim, Boolean.TRUE) != null) return;
        CharacterData before = CharacterService.get(player);
        CharacterData after = CharacterService.addExperience(player, HOSTILE_KILL_XP);
        if (after.experience() == before.experience()) return; // max level: no gratuitous sync
        if (after.level() > before.level()) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    "message.mkm.level_up", after.level(), after.availablePoints()));
        } else {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    "message.mkm.xp_gained", after.experience() - before.experience()));
        }
    }
}
