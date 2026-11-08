package com.eretenkodaniil.mkm.encounter;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.enemy.EnemyCatalog;
import com.eretenkodaniil.mkm.enemy.EnemyDefinition;
import com.eretenkodaniil.mkm.enemy.EnemyLevelRules;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.Spider;
import com.eretenkodaniil.mkm.quests.presentation.NpcVisualSignals;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Authoritative, opt-in World Encounter Director pilot.
 * One explicitly placed occurrence per Overworld save; never a natural-spawn hook,
 * AI controller, damage modifier, tick scanner, chunk ticket or renderer.
 */
public final class EncounterDirector {
    public static final String PILOT_ID = "mkm:ford_ambush";
    private static final String REWARD_ID = "mkm:encounter/ford_ambush";
    private static final String MOB_TAG = "mkm.encounter.ford_ambush.v1";
    private static final String OWNER_TAG = "mkm.encounter.owner.";
    private static final String ELITE_TAG = "mkm.encounter.elite.v1";
    private static final int[][] OFFSETS = {
            {7,0},{-7,0},{0,7},{0,-7},{6,5},{-6,5},{6,-5},{-6,-5},
            {9,0},{-9,0},{0,9},{0,-9}
    };

    private EncounterDirector() {}

    private static void inform(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal("MKM Encounter: " + text));
    }

    public static boolean start(ServerPlayer player) {
        return startAt(player, player.blockPosition(), PILOT_ID, false, 0);
    }

    /** Player-owned narrative encounter, separate from the old GM-only pilot ID. */
    public static boolean startStoryAt(ServerPlayer player, BlockPos anchor, String approach) {
        return startAt(player, anchor, "mkm:ford_ambush/" + player.getUUID()
                + "_" + approach, true,
                approach.equals("study") ? 25 : 0);
    }

    private static boolean startAt(ServerPlayer player, BlockPos anchor, String eventKey,
                                   boolean story, int bonusXp) {
        if (!(player.level() instanceof ServerLevel level)
                || !player.isAlive() || player.isSpectator()) return false;
        if (story && player.distanceToSqr(anchor.getX() + .5,anchor.getY(),anchor.getZ() + .5)
                > 64.0 * 64.0) return false;
        EncounterDefinition config = EncounterDefinitions.ford();
        if (config == null) {
            inform(player, "JSON не загружен или отклонён сервером. Посмотрите лог /reload.");
            return false;
        }
        if (!level.dimension().identifier().toString().equals(config.dimension())
                || level.getDifficulty() == Difficulty.PEACEFUL) {
            inform(player, "Нужен обычный мир и сложность не Peaceful.");
            return false;
        }
        String biome = level.getBiome(anchor).unwrapKey()
                .map(key -> key.identifier().toString()).orElse("unknown");
        if (!config.accepts(biome, level.getDefaultClockTime())) {
            inform(player, "Требуется разрешённый биом и ночное время: " + biome
                    + ". Можно использовать /time set night.");
            return false;
        }
        EncounterLedger ledger = level.getAttachedOrCreate(EncounterAttachments.ENCOUNTERS);
        if (!ledger.canReserve(eventKey, config.roster().size())) {
            inform(player, "Встреча уже зарезервирована/завершена или превышен бюджет сущностей. "
                    + "Повторное появление запрещено.");
            return false;
        }

        // Plan the complete squad within already loaded chunks, without forcing
        // chunk loading and without modifying terrain or vanilla mob-spawn rules.
        List<Mob> prepared = new ArrayList<>();
        List<EncounterLedger.Member> roster = new ArrayList<>();
        for (EncounterDefinition.Spawn spec : config.roster()) {
            Mob mob = newMob(level, spec);
            if (mob == null) return false;
            boolean placed = false;
            for (int[] offset : OFFSETS) {
                BlockPos feet = anchor.offset(offset[0], 0, offset[1]);
                if (!level.hasChunkAt(feet) || !level.getBlockState(feet.below()).isSolid()) continue;
                mob.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0F, 0.0F);
                if (!level.noCollision(mob)
                        || prepared.stream().anyMatch(other -> other.getBoundingBox()
                                .intersects(mob.getBoundingBox()))) continue;
                placed = true;
                break;
            }
            if (!placed) {
                inform(player, "Нет трёх свободных и загруженных площадок рядом. "
                        + "Встаньте на ровной поверхности; сущности не создавались.");
                return false;
            }
            UUID uuid = UUID.randomUUID();
            mob.setUUID(uuid);
            mob.addTag(MOB_TAG);
            mob.addTag(OWNER_TAG + player.getUUID());
            mob.addTag("mkm.encounter.role." + spec.role());
            if (spec.elite()) mob.addTag(ELITE_TAG);
            mob.setPersistenceRequired();
            if (mob instanceof PathfinderMob prototype) applyPrototype(
                    prototype, spec, config.dangerTier());
            mob.setCustomName(Component.literal(spec.elite()
                    ? "Костяной дозорный — командир" : spec.entityId().endsWith("husk")
                            ? "Налётчик с тракта" : spec.entityId().endsWith("spider")
                            ? "Паук-охотник из развалин" : "Дозорный-лучник"));
            mob.setCustomNameVisible(spec.elite());
            prepared.add(mob);
            roster.add(new EncounterLedger.Member(uuid.toString(),
                    spec.entityId(), spec.role(), spec.elite(), false));
        }

        EncounterLedger.Entry reserved = new EncounterLedger.Entry(
                EncounterLedger.SCHEMA_VERSION, player.getUUID().toString(),
                anchor.getX(), anchor.getY(), anchor.getZ(), config.rewardXp() + bonusXp,
                EncounterLedger.RESERVED, roster);
        // Persist UUIDs BEFORE any entity is inserted. A failed/partial spawn
        // stays reserved for manual audit; it never causes automatic duplicates.
        level.setAttached(EncounterAttachments.ENCOUNTERS, ledger.reserve(eventKey, reserved));
        for (Mob mob : prepared) {
            if (!level.addFreshEntity(mob)) {
                MkmMod.LOGGER.error("Encounter {} partially spawned; reserved roster retained, owner {}",
                        eventKey, player.getUUID());
                inform(player, "Часть группы не появилась. Резервирование сохранено, "
                        + "автоматическая повторная генерация запрещена.");
                return false;
            }
            mob.setTarget(player); // Vanilla AI goal scheduler; AI team owns future strategies.
            NpcVisualSignals.emit(mob, NpcVisualSignals.Cue.THREAT);
        }
        EncounterLedger current = level.getAttachedOrCreate(EncounterAttachments.ENCOUNTERS);
        EncounterLedger.Entry latest = current.find(eventKey);
        // Preserve any death already confirmed while the group was materializing.
        level.setAttached(EncounterAttachments.ENCOUNTERS,
                current.replace(eventKey, latest, latest.start()));
        inform(player, story
                ? "Засада пробудилась: налётчик, паук-охотник и командир-лучник."
                : "Ночная засада: налётчик, паук-охотник и командир-скелет. "
                  + "Победите троих и вернитесь к месту начала за наградой.");
        return true;
    }

    private static Mob newMob(ServerLevel level, EncounterDefinition.Spawn spec) {
        switch (spec.entityId()) {
            case "minecraft:husk" -> {
                return new Husk(EntityTypes.HUSK, level);
            }
            case "minecraft:spider" -> {
                return new Spider(EntityTypes.SPIDER, level);
            }
            case "minecraft:skeleton" -> {
                Skeleton skeleton = new Skeleton(EntityTypes.SKELETON, level);
                // Explicit vanilla ranged equipment; commander helmet prevents
                // daylight burn if the player sleeps through the encounter.
                skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                if (spec.elite()) {
                    skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                }
                // The direct constructor initially assesses an empty hand;
                // explicitly select the vanilla bow goal after equipping.
                skeleton.reassessWeaponGoal();
                return skeleton;
            }
            default -> {
                return null; // explicit entity-family allowlist; AI mods cannot inject classes via JSON
            }
        }
    }

    /**
     * Reuse actual Enemy V1 families and capped level rules. World only
     * initializes the authored group's ecology; EnemyService owns AI runtime.
     * Set base attributes exactly once before initial entity insertion.
     */
    private static void applyPrototype(PathfinderMob mob, EncounterDefinition.Spawn spec, int tier) {
        EnemyDefinition enemy = spec.entityId().equals("minecraft:husk")
                ? EnemyCatalog.ROAD_RAIDER : spec.entityId().equals("minecraft:spider")
                ? EnemyCatalog.CAVE_STALKER : null;
        if (enemy == null) return;
        EnemyLevelRules.Stats stats=EnemyLevelRules.forLevel(enemy,tier);
        mob.addTag(enemy.persistentTag());
        mob.addTag(EnemyLevelRules.levelTag(stats.level()));
        AttributeInstance hp=mob.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance attack=mob.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance movement=mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (hp!=null) hp.setBaseValue(stats.health());
        if (attack!=null) attack.setBaseValue(stats.attack());
        if (movement!=null) movement.setBaseValue(stats.speed());
        mob.setHealth((float)stats.health());
    }

    /** AFTER_DEATH only. World roster membership, entity type and owner tag must agree. */
    public static void onDeath(LivingEntity victim, net.minecraft.world.damagesource.DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level) || !victim.entityTags().contains(MOB_TAG)) return;
        EncounterLedger ledger = level.getAttachedOrCreate(EncounterAttachments.ENCOUNTERS);
        String deadId = victim.getUUID().toString();
        // Death callbacks are rare. At most 128 saved occurrences per level;
        // look up a typed member by UUID without ever scanning unloaded chunks.
        String eventKey = null;
        EncounterLedger.Entry event = null;
        for (var candidate : ledger.encounters().entrySet()) {
            if (candidate.getValue().members().stream().anyMatch(member -> member.uuid().equals(deadId))) {
                eventKey = candidate.getKey();
                event = candidate.getValue();
                break;
            }
        }
        if (event == null) return;
        EncounterLedger.Entry verifiedEvent=event;
        boolean authorized = verifiedEvent.members().stream().anyMatch(member -> member.uuid().equals(deadId)
                && member.entityId().equals(victim.getType() == EntityTypes.HUSK
                        ? "minecraft:husk" : victim.getType() == EntityTypes.SPIDER
                        ? "minecraft:spider" : victim.getType() == EntityTypes.SKELETON
                        ? "minecraft:skeleton" : "unknown")
                && victim.entityTags().contains(OWNER_TAG + verifiedEvent.ownerId()));
        if (!authorized) return;
        EncounterLedger after = ledger.markDeath(eventKey, deadId);
        if (after == ledger) return; // duplicated or unrelated death callback
        level.setAttached(EncounterAttachments.ENCOUNTERS, after);
        NpcVisualSignals.emit(victim, NpcVisualSignals.Cue.DEATH);
        var changed = after.find(eventKey);
        ServerPlayer owner = null;
        try {
            owner = level.getServer().getPlayerList().getPlayer(UUID.fromString(event.ownerId()));
        } catch (IllegalArgumentException badOwner) {
            MkmMod.LOGGER.error("Encounter {} has invalid saved owner UUID {}", eventKey, event.ownerId());
        }
        if (owner != null) {
            boolean narrative = !eventKey.equals(PILOT_ID);
            inform(owner, changed.phase().equals(EncounterLedger.DEFEATED)
                    ? (narrative ? "Защитники башни повержены. Вернитесь к руне для завершения."
                            : "Засада уничтожена. Вернитесь к месту начала и выполните /mkmencounter claim.")
                    : "Противник уничтожен. Осталось: " + changed.aliveCount());
            if (narrative) com.eretenkodaniil.mkm.quests.net.FordQuestNetworking.sendJournal(owner);
        }
    }

    public static boolean claim(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || player.isSpectator())
            return false;
        EncounterLedger ledger = level.getAttachedOrCreate(EncounterAttachments.ENCOUNTERS);
        EncounterLedger.Entry event = ledger.find(PILOT_ID);
        if (event == null || !event.ownerId().equals(player.getUUID().toString())
                || !event.phase().equals(EncounterLedger.DEFEATED)) {
            inform(player, "Награда недоступна: встреча ещё не завершена, уже оплачена или принадлежит другому игроку.");
            return false;
        }
        if (player.distanceToSqr(event.x() + 0.5, event.y(), event.z() + 0.5) > 256.0) {
            inform(player, "Вернитесь в радиус 16 блоков от места засады за наградой.");
            return false;
        }
        // Same v4 Character attachment holds receipt, XP, level and allocation credits.
        // A crash between receipt and world CLAIMED stage is safely retryable.
        var result = CharacterService.grantExperienceOnce(player, REWARD_ID, event.rewardXp());
        if (!result.confirmed()) {
            inform(player, "Character M3 отказал в награде: " + result.status());
            return false;
        }
        EncounterLedger updated = level.getAttachedOrCreate(EncounterAttachments.ENCOUNTERS);
        level.setAttached(EncounterAttachments.ENCOUNTERS, updated.claim(PILOT_ID));
        inform(player, result.creditedExperience() > 0L
                ? "Награда получена: +" + result.creditedExperience() + " MKM XP."
                : "Награда уже была учтена или достигнут предел XP.");
        return true;
    }

    public static void status(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        EncounterLedger.Entry event = level.getAttachedOrCreate(EncounterAttachments.ENCOUNTERS)
                .find(PILOT_ID);
        if (event == null) {
            inform(player, "Ночная засада ещё не запускалась в этом мире.");
            return;
        }
        // Public status must not expose another player's encounter UUIDs,
        // roster or exact home coordinates (multiplayer griefing).
        if (!event.ownerId().equals(player.getUUID().toString())) {
            inform(player, "Встреча находится в состоянии " + event.phase()
                    + ". Подробности доступны только назначенному владельцу.");
            return;
        }
        inform(player, "state=" + event.phase() + ", rewardXp=" + event.rewardXp()
                + ", owner=" + event.ownerId() + ", anchor=" + event.x() + "/"
                + event.y() + "/" + event.z() + ", remaining=" + event.aliveCount());
        if (event.phase().equals(EncounterLedger.ACTIVE) || event.phase().equals(EncounterLedger.RESERVED)) {
            for (EncounterLedger.Member member : event.members()) {
                if (member.dead()) continue;
                Entity loaded = null;
                try { loaded = level.getEntity(UUID.fromString(member.uuid())); }
                catch (IllegalArgumentException badId) {
                    MkmMod.LOGGER.warn("Invalid member UUID in encounter ledger: {}", member.uuid());
                }
                inform(player, member.role() + " / " + member.entityId() + " / " + member.uuid()
                        + (loaded == null ? " — не загружен либо отсутствует (НЕ доказательство смерти)"
                            : " — загружен"));
            }
        }
    }
}
