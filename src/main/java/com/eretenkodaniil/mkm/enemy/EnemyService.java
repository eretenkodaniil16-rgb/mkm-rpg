package com.eretenkodaniil.mkm.enemy;

import com.eretenkodaniil.mkm.quests.presentation.NpcVisualSignals;
import com.eretenkodaniil.mkm.registry.MkmItems;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;

/**
 * Server-only indexed load/unload lifecycle. No scans of every server entity.
 * No custom damage packets, invulnerability overrides or replacement GoalSelector.
 */
public final class EnemyService {
    private static final int MAX_TRACKED_PER_LEVEL = 512;
    private static final String DEATH_LOOT_TAG = "mkm.enemy.loot_claimed.v1";
    private static final Map<ServerLevel, Map<UUID, EnemyRuntime>> LOADED = new IdentityHashMap<>();
    private static final long[] LATENCY_BUCKET_CEIL_US = {25L, 50L, 100L, 250L, 500L, 1000L, 2000L, 5000L};
    private static final long[] LATENCY_COUNTS = new long[LATENCY_BUCKET_CEIL_US.length + 1];
    private static long tickSamples;
    private static long totalTickNanos;
    private static long peakTickNanos;
    private static int peakTracked;
    private static long totalAllocatedBytes;
    private static long allocationSamples;
    private static final com.sun.management.ThreadMXBean ALLOCATION_MX_BEAN =
            ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean
                    && bean.isThreadAllocatedMemorySupported() ? bean : null;

    private static long allocatedOnThread() {
        if (ALLOCATION_MX_BEAN == null || !ALLOCATION_MX_BEAN.isThreadAllocatedMemoryEnabled())
            return -1L;
        return ALLOCATION_MX_BEAN.getThreadAllocatedBytes(Thread.currentThread().threadId());
    }

    private EnemyService() {}

    public static void initialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> onLoad(entity, level));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> onUnload(entity, level));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LOADED.clear();
            resetPerformance();
        });
    }

    public static void onLoad(Entity entity, ServerLevel level) {
        if (!(entity instanceof PathfinderMob mob)) return;
        EnemyDefinition definition = EnemyCatalog.byTags(entity.entityTags());
        if (definition == null) return;
        Map<UUID, EnemyRuntime> perLevel = LOADED.computeIfAbsent(level, ignored -> new HashMap<>());
        if (perLevel.size() >= MAX_TRACKED_PER_LEVEL && !perLevel.containsKey(mob.getUUID())) return;
        perLevel.putIfAbsent(mob.getUUID(), new EnemyRuntime(mob, definition));
    }

    private static void onUnload(Entity entity, ServerLevel level) {
        Map<UUID, EnemyRuntime> perLevel = LOADED.get(level);
        if (perLevel == null) return;
        perLevel.remove(entity.getUUID());
        if (perLevel.isEmpty()) LOADED.remove(level);
    }

    public static void onServerTick(MinecraftServer server) {
        long beforeAllocated = allocatedOnThread();
        long started = System.nanoTime();
        int active = 0;
        for (ServerLevel level : server.getAllLevels()) {
            Map<UUID, EnemyRuntime> perLevel = LOADED.get(level);
            if (perLevel == null) continue;
            long now = level.getGameTime();
            Iterator<EnemyRuntime> iterator = perLevel.values().iterator();
            while (iterator.hasNext()) {
                EnemyRuntime runtime = iterator.next();
                PathfinderMob mob = runtime.mob();
                if (mob.isRemoved() || !mob.isAlive()) {
                    iterator.remove();
                    continue;
                }
                active++;
                runtime.tick(level, now);
            }
            if (perLevel.isEmpty()) LOADED.remove(level);
        }
        long elapsed = Math.max(0L, System.nanoTime() - started);
        long afterAllocated = allocatedOnThread();
        // Only measure real active cohorts; idle server time cannot dilute averages.
        if (active > 0) {
            tickSamples++;
            totalTickNanos += elapsed;
            peakTickNanos = Math.max(peakTickNanos, elapsed);
            peakTracked = Math.max(peakTracked, active);
            long elapsedUs = elapsed / 1000L;
            int bucket = 0;
            while (bucket < LATENCY_BUCKET_CEIL_US.length
                    && elapsedUs > LATENCY_BUCKET_CEIL_US[bucket]) bucket++;
            LATENCY_COUNTS[bucket]++;
            if (beforeAllocated >= 0L && afterAllocated >= beforeAllocated) {
                totalAllocatedBytes += afterAllocated - beforeAllocated;
                allocationSamples++;
            }
        }
    }

    /** Only existing CharacterEvents.onEntityDeath may credit MKM kill XP. */
    public static long killXp(LivingEntity victim) {
        EnemyDefinition definition = EnemyCatalog.byTags(victim.entityTags());
        if (definition == null) return 25L;
        return EnemyLevelRules.forLevel(definition,
                EnemyLevelRules.readLevel(victim.entityTags())).xp();
    }

    public static void afterDamage(LivingEntity entity, DamageSource source,
            float baseDamage, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F || !(entity instanceof PathfinderMob mob)
                || !(source.getEntity() instanceof ServerPlayer aggressor)
                || !(entity.level() instanceof ServerLevel level)) return;
        EnemyDefinition definition = EnemyCatalog.byTags(mob.entityTags());
        if (definition == null) return;
        Map<UUID, EnemyRuntime> known = LOADED.get(level);
        if (known == null) return;
        long now = level.getGameTime();
        EnemyRuntime controller = known.get(mob.getUUID());
        if (controller != null) controller.onAttackedBy(aggressor, now);
        // Damage-triggered, bounded local ally alert. Never every-tick O(n^2).
        if (controller != null && EnemyTactics.shouldRepath(now, mob.getId(), 20)) {
            int informed = 0;
            for (PathfinderMob nearby : level.getEntitiesOfClass(PathfinderMob.class,
                    mob.getBoundingBox().inflate(12.0),
                    candidate -> candidate != mob && candidate.isAlive()
                            && candidate.entityTags().contains(definition.persistentTag()))) {
                EnemyRuntime ally = known.get(nearby.getUUID());
                if (ally != null && ally.mob().getTarget() == null) {
                    ally.onAttackedBy(aggressor, now);
                    if (++informed >= 8) break;
                }
            }
        }
        NpcVisualSignals.emit(mob, definition.rigProfile(), NpcVisualSignals.Cue.REACT);
    }

    public static void afterDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof PathfinderMob mob)
                || !(entity.level() instanceof ServerLevel level)) return;
        EnemyDefinition definition = EnemyCatalog.byTags(mob.entityTags());
        if (definition == null || mob.entityTags().contains(DEATH_LOOT_TAG)) return;
        // Mark before effects: repeated callbacks on this entity are a strict no-op.
        mob.addTag(DEATH_LOOT_TAG);
        NpcVisualSignals.emit(mob, definition.rigProfile(), NpcVisualSignals.Cue.DEATH);
        // Prototype-specific guaranteed material token. Vanilla loot stays intact.
        // A complete world-save transaction across item spawn and entity removal
        // is not offered by Minecraft; see the QA contract.
        mob.spawnAtLocation(level, new ItemStack(
                definition == EnemyCatalog.ROAD_RAIDER
                        ? MkmItems.RAIDER_INSIGNIA : MkmItems.STALKER_FANG));
        onUnload(mob, level);
    }

    /** Reset histogram without touching loaded mobs or gameplay state. GM-only command. */
    public static void resetPerformance() {
        tickSamples = 0L;
        totalTickNanos = 0L;
        peakTickNanos = 0L;
        peakTracked = 0;
        totalAllocatedBytes = 0L;
        allocationSamples = 0L;
        Arrays.fill(LATENCY_COUNTS, 0L);
    }

    private static String p95UpperBound() {
        if (tickSamples == 0L) return "n/a";
        long target = (long) Math.ceil(tickSamples * 0.95);
        long cumulative = 0L;
        for (int i = 0; i < LATENCY_COUNTS.length; i++) {
            cumulative += LATENCY_COUNTS[i];
            if (cumulative >= target) {
                return i == LATENCY_BUCKET_CEIL_US.length
                        ? ">5000us" : "<=" + LATENCY_BUCKET_CEIL_US[i] + "us";
            }
        }
        return "n/a";
    }

    public static String performanceLine() {
        int active = 0;
        for (Map<UUID, EnemyRuntime> map : LOADED.values()) active += map.size();
        double averageUs = tickSamples == 0 ? 0.0 : totalTickNanos / (1000.0 * tickSamples);
        String allocation = allocationSamples == 0 ? "unavailable" :
                String.format(java.util.Locale.ROOT, "%.0f B/tick",
                        (double) totalAllocatedBytes / allocationSamples);
        return String.format(java.util.Locale.ROOT,
                "MKM Enemy AI | loaded=%d peak=%d samples=%d avg=%.1fus p95 upper=%s max=%.1fus alloc=%s playerScan=%s (module only; NOT full MSPT)",
                active, peakTracked, tickSamples, averageUs, p95UpperBound(),
                peakTickNanos / 1000.0, allocation,
                EnemyRuntime.directPlayerScanEnabled() ? "direct-experimental" : "vanilla-spatial");
    }
}
