package com.eretenkodaniil.mkm.quests;

import com.eretenkodaniil.mkm.character.CharacterService;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.List;

/**
 * First authored encounter. No worldgen, per-tick scan, damage override, or custom item/XP system.
 * NPC and raider identities use vanilla persistent entity tags; player progress uses Fabric attachment.
 */
public final class FordQuestService {
    private static final String NPC_TAG = "mkm.ford.warden.v1";
    private static final String RAIDER_TAG = "mkm.ford.raider.v1";
    private static final String OWNER_PREFIX = "mkm.ford.owner.";
    private static final double TALK_RANGE_SQUARED = 36.0;

    private FordQuestService() {}

    public static void initialize() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!entity.entityTags().contains(NPC_TAG)) {
                return InteractionResult.PASS;
            }
            // Consume on both sides to prevent vanilla villager trading or spectator bypass.
            if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
                if (!serverPlayer.isSpectator() && serverPlayer.distanceToSqr(entity) <= TALK_RANGE_SQUARED) {
                    talk(serverPlayer);
                }
            }
            return InteractionResult.SUCCESS;
        });
    }

    public static FordQuestState get(ServerPlayer player) {
        return player.getAttachedOrCreate(FordQuestAttachments.FORD_QUEST);
    }

    private static void update(ServerPlayer player, FordQuestState state) {
        player.setAttached(FordQuestAttachments.FORD_QUEST, state);
    }

    private static void message(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal(text));
    }

    /** Only gamemasters can place this single, static pilot NPC. */
    public static boolean placeWarden(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        Villager npc = new Villager(EntityTypes.VILLAGER, level);
        npc.moveTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
        npc.addTag(NPC_TAG);
        npc.setCustomName(Component.literal("Ильра — смотрительница брода"));
        npc.setCustomNameVisible(true);
        npc.setNoAi(true);
        npc.setInvulnerable(true);
        npc.setPersistenceRequired();
        boolean placed = level.addFreshEntity(npc);
        if (placed) {
            message(player, "MKM: Ильра создана. Нажмите ПКМ на NPC. Для демонстрации нужна площадка рядом с ним.");
        }
        return placed;
    }

    private static Villager nearbyWarden(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        List<Villager> nearby = level.getEntitiesOfClass(
                Villager.class, player.getBoundingBox().inflate(6.0),
                npc -> npc.isAlive() && npc.entityTags().contains(NPC_TAG)
                        && player.distanceToSqr(npc) <= TALK_RANGE_SQUARED);
        return nearby.isEmpty() ? null : nearby.getFirst();
    }

    private static boolean requireWarden(ServerPlayer player) {
        if (player.isSpectator() || nearbyWarden(player) == null) {
            message(player, "MKM: Подойдите к смотрительнице Ильре (не далее 6 блоков).");
            return false;
        }
        return true;
    }

    /** Chat dialogue for the MVP; each command is a server-validated player choice. */
    public static void talk(ServerPlayer player) {
        if (!requireWarden(player)) {
            return;
        }
        FordQuestState state = get(player);
        switch (state.stage()) {
            case FordQuestState.NOT_STARTED -> {
                message(player, "[Ильра] Караван пропал у старого брода. Сможешь остановить налётчика?");
                message(player, "[Выбор] /mkmworld accept — помогу; /mkmworld inspect — изучить следы (PER 12); /mkmworld leave — уйти.");
            }
            case FordQuestState.ACTIVE -> {
                message(player, "[Ильра] Разбойник ещё у брода. Найди и победи его. Если он пропал, /mkmworld recover.");
                if (state.foundTracks()) {
                    message(player, "[Наблюдение] По следам видно: это иссушённый мертвец, не боящийся солнечного света.");
                }
            }
            case FordQuestState.RETURN_TO_WARDEN -> {
                message(player, "[Ильра] Путь снова открыт. Прими награду: /mkmworld claim.");
            }
            case FordQuestState.REWARDED ->
                    message(player, "[Ильра] Ты уже помог броду. Торговля и строительство в этом мире остаются обычными.");
            default -> message(player, "MKM: Неизвестная версия состояния задания. Изменения заблокированы во избежание порчи сохранения.");
        }
    }

    public static void leave(ServerPlayer player) {
        if (requireWarden(player)) {
            message(player, "[Ильра] Возвращайся, когда решишься. Задание пока не принято.");
        }
    }

    public static void inspect(ServerPlayer player) {
        if (!requireWarden(player)) {
            return;
        }
        FordQuestState previous = get(player);
        int perception = CharacterService.get(player).perception();
        FordQuestState result = FordQuestRules.inspect(previous, perception);
        if (result != previous) {
            update(player, result);
            message(player, "[PER " + perception + " >= 12] Следы выдают хуска: он устойчив к солнцу. При награде +25 MKM XP.");
        } else if (previous.foundTracks()) {
            message(player, "MKM: Ты уже изучил следы.");
        } else {
            message(player, "[PER " + perception + " / 12] Не удаётся различить следы или проверка уже недоступна.");
        }
    }

    public static void accept(ServerPlayer player) {
        if (!requireWarden(player)) {
            return;
        }
        FordQuestState previous = get(player);
        FordQuestState accepted = FordQuestRules.accept(previous);
        if (accepted == previous) {
            message(player, "MKM: Это задание уже принято или завершено.");
            return;
        }
        Villager npc = nearbyWarden(player);
        if (npc == null || !spawnRaider(player, npc)) {
            message(player, "MKM: Нужны мирная сложность ОТКЛ и свободная ровная площадка в пределах 8 блоков от NPC.");
            return;
        }
        update(player, accepted);
        message(player, "[Ильра] Налётчик появился неподалёку. Одолей его и возвращайся!");
    }

    private static String ownerTag(ServerPlayer player) {
        return OWNER_PREFIX + player.getUUID();
    }

    private static boolean spawnRaider(ServerPlayer player, Villager npc) {
        if (!(player.level() instanceof ServerLevel level) || level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        // Deterministic local positions. Only checked on player interaction, never every tick.
        int[][] offsets = {{7,0},{-7,0},{0,7},{0,-7},{6,5},{-6,5},{6,-5},{-6,-5}};
        for (int[] offset : offsets) {
            double x = Math.floor(npc.getX()) + offset[0] + 0.5;
            double y = Math.floor(npc.getY());
            double z = Math.floor(npc.getZ()) + offset[1] + 0.5;
            BlockPos feet = BlockPos.containing(x, y, z);
            if (!level.getBlockState(feet.below()).isSolid()) {
                continue;
            }
            Husk raider = new Husk(EntityTypes.HUSK, level);
            raider.moveTo(x, y, z, 0.0F, 0.0F);
            if (!level.noCollision(raider)) {
                continue;
            }
            raider.addTag(RAIDER_TAG);
            raider.addTag(ownerTag(player));
            raider.setPersistenceRequired();
            raider.setCustomName(Component.literal("Разоритель брода"));
            raider.setCustomNameVisible(true);
            if (level.addFreshEntity(raider)) {
                return true;
            }
        }
        return false;
    }

    public static void recover(ServerPlayer player) {
        if (!requireWarden(player)) {
            return;
        }
        if (!get(player).stage().equals(FordQuestState.ACTIVE)) {
            message(player, "MKM: Восстановление встречи доступно только при активном задании.");
            return;
        }
        Villager npc = nearbyWarden(player);
        if (!(player.level() instanceof ServerLevel level) || npc == null) {
            return;
        }
        // Local safety check: never spawn another owned raider if the original is nearby.
        boolean exists = !level.getEntitiesOfClass(Husk.class, npc.getBoundingBox().inflate(48.0),
                husk -> husk.isAlive() && husk.entityTags().contains(RAIDER_TAG)
                        && husk.entityTags().contains(ownerTag(player))).isEmpty();
        if (exists) {
            message(player, "MKM: Твой противник ещё существует поблизости. Новый не создаётся.");
        } else if (spawnRaider(player, npc)) {
            message(player, "MKM: Потерянная встреча восстановлена.");
        } else {
            message(player, "MKM: Не удалось создать противника. Проверьте сложность и окружающую площадку.");
        }
    }

    /** Combat is vanilla + MKM CombatService. This listener only consumes confirmed deaths. */
    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Husk) || !entity.entityTags().contains(RAIDER_TAG)
                || !(source.getEntity() instanceof ServerPlayer killer)
                || !entity.entityTags().contains(ownerTag(killer))) {
            return;
        }
        FordQuestState previous = get(killer);
        FordQuestState result = FordQuestRules.onOwnedRaiderDefeated(previous);
        if (result != previous) {
            update(killer, result);
            message(killer, "MKM: Налётчик побеждён. Вернись к Ильре за наградой.");
        }
    }

    /** Mark claimed first: repeated requests/relog cannot mint extra XP. See crash-window limitation. */
    public static void claim(ServerPlayer player) {
        if (!requireWarden(player)) {
            return;
        }
        FordQuestState previous = get(player);
        FordQuestState rewarded = FordQuestRules.claim(previous);
        if (rewarded == previous) {
            message(player, "MKM: Награда пока недоступна или уже получена.");
            return;
        }
        long xp = FordQuestRules.rewardExperience(previous);
        update(player, rewarded);
        CharacterService.addExperience(player, xp);
        message(player, "[Ильра] Спасибо! Получено " + xp + " MKM XP (не vanilla XP).");
    }

    public static void status(ServerPlayer player) {
        FordQuestState quest = get(player);
        message(player, "MKM Ford Quest | stage=" + quest.stage()
                + " | tracks=" + quest.foundTracks()
                + " | version=" + quest.schemaVersion());
    }
}
