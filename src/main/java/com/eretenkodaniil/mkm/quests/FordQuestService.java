package com.eretenkodaniil.mkm.quests;

import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.character.ProgressionRules;
import com.eretenkodaniil.mkm.quests.net.FordQuestNetworking;
import com.eretenkodaniil.mkm.quests.net.FordChoiceRequest;
import java.util.UUID;
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
            // On the client let the normal interact packet reach the server.
            // Only the server consumes the tagged villager interaction, preventing trading.
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (hand == InteractionHand.MAIN_HAND
                    && !serverPlayer.isSpectator() && serverPlayer.isAlive()
                    && serverPlayer.distanceToSqr(entity) <= TALK_RANGE_SQUARED) {
                showDialogue(serverPlayer, entity);
            }
            return InteractionResult.SUCCESS;
        });
    }

    private static void showDialogue(ServerPlayer player, Entity entity) {
        if (!(entity instanceof Villager npc) || !requireWarden(player)) return;
        FordQuestState state = get(player);
        if (!state.wardenId().isBlank() && !state.wardenId().equals(npc.getUUID().toString())) return;
        if (!FordQuestNetworking.showDialogue(player, npc)) talk(player);
    }

    public static void handleChoice(ServerPlayer player, FordChoiceRequest request) {
        if (player.isSpectator() || !player.isAlive()
                || !(player.level() instanceof ServerLevel level)) return;
        Entity entity = level.getEntity(request.entityId());
        if (!(entity instanceof Villager npc) || !npc.isAlive()
                || !npc.entityTags().contains(NPC_TAG)
                || player.distanceToSqr(npc) > TALK_RANGE_SQUARED) return;
        FordQuestState state = get(player);
        if (!state.wardenId().isBlank() && !state.wardenId().equals(npc.getUUID().toString())) return;
        if (request.expectedStage() != FordQuestNetworking.stage(state)) return;
        switch (request.choice()) {
            case FordChoiceRequest.INSPECT -> { if (request.expectedStage() == 0) inspect(player); else return; }
            case FordChoiceRequest.ACCEPT -> { if (request.expectedStage() == 0) accept(player); else return; }
            case FordChoiceRequest.LEAVE -> { leave(player); return; }
            case FordChoiceRequest.CLAIM -> { if (request.expectedStage() == 2) claim(player); else return; }
            case FordChoiceRequest.LOCATE -> { if (request.expectedStage() == 1) recover(player); else return; }
            default -> { return; }
        }
        showDialogue(player, npc);
    }

    public static boolean isEncounterLoaded(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        Entity e = findEncounter(player, get(player), level);
        return e != null && e.isAlive();
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
        if (!level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(48.0),
                existing -> existing.isAlive() && existing.entityTags().contains(NPC_TAG)).isEmpty()) {
            message(player, "MKM: Смотрительница уже существует в пределах 48 блоков. Новая не создана.");
            return false;
        }
        // Avoid spawning Ilra inside the player or obstructed terrain. This is a
        // one-time GM operation, not a background spawn scan.
        int[][] offsets = {{2,0},{-2,0},{0,2},{0,-2},{2,2},{-2,2},{2,-2},{-2,-2}};
        for (int[] offset : offsets) {
            double x = Math.floor(player.getX()) + offset[0] + 0.5;
            double y = Math.floor(player.getY());
            double z = Math.floor(player.getZ()) + offset[1] + 0.5;
            BlockPos feet = BlockPos.containing(x, y, z);
            if (!level.getBlockState(feet.below()).isSolid()) {
                continue;
            }
            Villager npc = new Villager(EntityTypes.VILLAGER, level);
            npc.snapTo(x, y, z, 0.0F, 0.0F);
            if (!level.noCollision(npc)) {
                continue;
            }
            npc.addTag(NPC_TAG);
            npc.setCustomName(Component.literal("Ильра — смотрительница брода"));
            npc.setCustomNameVisible(true);
            npc.setNoAi(true);
            npc.setPermanentlyInvulnerable(true);
            npc.setPersistenceRequired();
            if (level.addFreshEntity(npc)) {
                message(player, "MKM: Ильра создана в двух блоках от вас. Нажмите ПКМ для диалога.");
                return true;
            }
        }
        message(player, "MKM: Для NPC нет свободного места рядом с вами. Встаньте на ровную площадку.");
        return false;
    }

    private static Villager nearbyWarden(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        List<Villager> nearby = level.getEntitiesOfClass(
                Villager.class, player.getBoundingBox().inflate(6.0),
                npc -> npc.isAlive() && npc.entityTags().contains(NPC_TAG)
                        && player.distanceToSqr(npc) <= TALK_RANGE_SQUARED
                        && (get(player).wardenId().isBlank()
                            || get(player).wardenId().equals(npc.getUUID().toString())));
        return nearby.isEmpty() ? null : nearby.getFirst();
    }

    private static boolean requireWarden(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive() || nearbyWarden(player) == null) {
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
        Husk raider = npc == null ? null : spawnRaider(player, npc);
        if (raider == null) {
            message(player, "MKM: Нужна сложность выше Peaceful и безопасная площадка в пределах 8 блоков.");
            return;
        }
        // The UUID is captured from a real, successfully spawned server entity.
        update(player, accepted.withEncounter(raider.getUUID().toString(), npc.getUUID().toString()));
        message(player, "[Ильра] Налётчик появился неподалёку. Одолей его и возвращайся!");
    }

    private static String ownerTag(ServerPlayer player) {
        return OWNER_PREFIX + player.getUUID();
    }

    private static Husk spawnRaider(ServerPlayer player, Villager npc) {
        if (!(player.level() instanceof ServerLevel level) || level.getDifficulty() == Difficulty.PEACEFUL) {
            return null;
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
            raider.snapTo(x, y, z, 0.0F, 0.0F);
            if (!level.noCollision(raider)) {
                continue;
            }
            raider.addTag(RAIDER_TAG);
            raider.addTag(ownerTag(player));
            raider.setPersistenceRequired();
            raider.setCustomName(Component.literal("Разоритель брода"));
            raider.setCustomNameVisible(true);
            if (level.addFreshEntity(raider)) {
                return raider;
            }
        }
        return null;
    }

    /**
     * Safely locate the exact server-owned encounter entity. Never infer that an
     * unloaded entity is dead; never create a replacement merely because a search
     * within 48 blocks returned empty. This intentionally fails closed.
     */
    public static void recover(ServerPlayer player) {
        if (!requireWarden(player)) return;
        FordQuestState quest = get(player);
        if (!quest.stage().equals(FordQuestState.ACTIVE)) {
            message(player, "MKM: Встреча уже не активна.");
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) return;
        Entity raider = findEncounter(player, quest, level);
        if (raider != null && raider.isAlive()) {
            message(player, "MKM: Разоритель существует: X " + raider.blockPosition().getX()
                    + ", Y " + raider.blockPosition().getY()
                    + ", Z " + raider.blockPosition().getZ()
                    + ". Противник НЕ создан повторно.");
            return;
        }
        message(player, "MKM: Противник сейчас не загружен или потерян. "
                + "Вернитесь в район встречи для загрузки чанка и повторите поиск. "
                + "Новый моб автоматически НЕ создаётся во избежание дубликатов.");
    }

    private static Entity findEncounter(ServerPlayer player, FordQuestState quest, ServerLevel level) {
        if (!quest.encounterId().isBlank()) {
            try {
                Entity matching = level.getEntity(UUID.fromString(quest.encounterId()));
                return matching instanceof Husk && matching.entityTags().contains(RAIDER_TAG)
                        && matching.entityTags().contains(ownerTag(player)) ? matching : null;
            } catch (IllegalArgumentException badId) {
                return null;
            }
        }
        // Only for pre-v3 saves with no tracked entity UUID: bind a *single* loaded
        // tagged raider rather than synthesizing another one.
        Villager npc = nearbyWarden(player);
        if (npc == null) return null;
        List<Husk> matches = level.getEntitiesOfClass(Husk.class, npc.getBoundingBox().inflate(48.0),
                husk -> husk.isAlive() && husk.entityTags().contains(RAIDER_TAG)
                        && husk.entityTags().contains(ownerTag(player)));
        if (matches.size() != 1) return null;
        Entity found = matches.getFirst();
        update(player, quest.withEncounter(found.getUUID().toString(), npc.getUUID().toString()));
        return found;
    }

    /**
     * The authored objective is that the owned raider is eliminated, not that
     * the owner must deal the final hit. Other players/environment can help.
     * Offline owners receive a world-persistent death tombstone on login.
     */
    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Husk) || !entity.entityTags().contains(RAIDER_TAG)
                || !(entity.level() instanceof ServerLevel level)) return;
        UUID ownerId = null;
        for (String tag : entity.entityTags()) {
            if (!tag.startsWith(OWNER_PREFIX)) continue;
            try {
                ownerId = UUID.fromString(tag.substring(OWNER_PREFIX.length()));
                break;
            } catch (IllegalArgumentException ignored) {
                return; // malformed ownership: fail closed
            }
        }
        if (ownerId == null) return;
        String deadId = entity.getUUID().toString();
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            applyEncounterDeath(owner, deadId);
        } else {
            var existing = level.getAttachedOrCreate(FordQuestAttachments.ENCOUNTER_OUTCOMES);
            var updated = existing.recordDeath(deadId, ownerId.toString());
            if (existing != updated) level.setAttached(FordQuestAttachments.ENCOUNTER_OUTCOMES, updated);
        }
    }

    private static boolean applyEncounterDeath(ServerPlayer owner, String deadId) {
        FordQuestState previous = get(owner);
        if (!FordQuestRules.matchesEncounter(previous, deadId)) return false;
        FordQuestState result = FordQuestRules.onOwnedRaiderDefeated(previous);
        if (result == previous) return false;
        update(owner, result);
        message(owner, "MKM: Разоритель брода устранён. Вернись к Ильре.");
        return true;
    }

    private static void replayOfflineDeaths(ServerPlayer owner) {
        String ownerId = owner.getUUID().toString();
        for (ServerLevel level : owner.level().getServer().getAllLevels()) {
            FordEncounterOutcomes pending = level.getAttachedOrCreate(FordQuestAttachments.ENCOUNTER_OUTCOMES);
            for (var entry : pending.deadRaiders().entrySet()) {
                if (entry.getValue().equals(ownerId) && applyEncounterDeath(owner, entry.getKey())) {
                    level.setAttached(FordQuestAttachments.ENCOUNTER_OUTCOMES, pending.remove(entry.getKey()));
                    return; // this authored quest has only one active encounter
                }
            }
        }
    }

    /**
     * Save a repairable cumulative XP floor in the same quest state as 'rewarded'.
     * Character Core remains the only writer of XP. Login/respawn repairs shortfalls.
     * This is not a general cross-file transaction protocol.
     */
    public static void claim(ServerPlayer player) {
        if (!requireWarden(player)) {
            return;
        }
        FordQuestState previous = get(player);
        long before = CharacterService.get(player).experience();
        FordQuestState rewarded = FordQuestRules.claimAtExperience(
                previous, before, ProgressionRules.experienceForLevel(ProgressionRules.MAX_LEVEL));
        if (rewarded == previous) {
            message(player, "MKM: Награда пока недоступна или уже получена.");
            return;
        }
        update(player, rewarded);
        repairReward(player);
        message(player, "[Ильра] Спасибо! Награда — до " + FordQuestRules.rewardExperience(previous)
                + " MKM XP. Завершение сохранено, повторная выдача запрещена.");
    }

    public static void onLogin(ServerPlayer player) {
        replayOfflineDeaths(player);
        repairReward(player);
    }

    /** Also run during an authorized claim, never from an untrusted client payload. */
    private static void repairReward(ServerPlayer player) {
        FordQuestState quest = get(player);
        long missing = FordQuestRules.missingExperience(quest, CharacterService.get(player).experience());
        if (missing > 0L) {
            CharacterService.addExperience(player, missing);
        }
    }

    public static void status(ServerPlayer player) {
        FordQuestState quest = get(player);
        message(player, "MKM Ford Quest | stage=" + quest.stage()
                + " | tracks=" + quest.foundTracks()
                + " | recoveries=" + quest.recoveryCount() + "/" + FordQuestRules.MAX_RECOVERIES
                + " | encounter=" + (quest.encounterId().isBlank() ? "legacy/none" : quest.encounterId())
                + " | xp_floor=" + quest.rewardXpFloor()
                + " | version=" + quest.schemaVersion());
    }
}
