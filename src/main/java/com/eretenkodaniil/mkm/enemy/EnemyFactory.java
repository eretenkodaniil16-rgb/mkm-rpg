package com.eretenkodaniil.mkm.enemy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The one spawn-time materialization adapter shared by World and the GM harness.
 * All damage and inventory behavior remains vanilla; no duplicate combat services.
 */
public final class EnemyFactory {
    public static final String ENCOUNTER_TAG = "mkm.encounter.ford_ambush.v1";
    public static final String GUARD_TAG = "mkm.enemy.raider.guard.v1";
    public static final String FLANK_TAG = "mkm.enemy.raider.flank.v1";
    public static final String ELITE_STALKER_TAG = "mkm.enemy.stalker.elite.v1";

    private EnemyFactory() {}

    /** Prepare entity only. The caller owns safe placement, UUID reservation and insertion. */
    public static PathfinderMob prepare(ServerLevel level, EnemyDefinition definition,
            int requestedLevel, boolean guard, boolean elite) {
        PathfinderMob mob = definition.host() == EnemyDefinition.Host.HUSK
                ? new Husk(EntityTypes.HUSK, level) : new Spider(EntityTypes.SPIDER, level);
        var stats = EnemyLevelRules.forLevel(definition, requestedLevel);
        mob.addTag(definition.persistentTag());
        mob.addTag(EnemyLevelRules.levelTag(stats.level()));
        mob.setPersistenceRequired();
        attr(mob, Attributes.MAX_HEALTH, stats.health());
        attr(mob, Attributes.ATTACK_DAMAGE, stats.attack());
        attr(mob, Attributes.MOVEMENT_SPEED, stats.speed());
        mob.setHealth((float) stats.health());
        if (definition == EnemyCatalog.ROAD_RAIDER) {
            if (guard) {
                mob.addTag(GUARD_TAG);
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            } else {
                mob.addTag(FLANK_TAG);
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            }
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            mob.setCustomName(Component.literal(guard ? "MKM Road Raider Guard" : "MKM Road Raider Flanker"));
        } else {
            if (elite) mob.addTag(ELITE_STALKER_TAG);
            mob.setCustomName(Component.literal(elite ? "MKM Cave Stalker — Shadow Alpha" : "MKM Cave Stalker"));
        }
        mob.setCustomNameVisible(elite);
        return mob;
    }

    private static void attr(PathfinderMob mob,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> key, double value) {
        AttributeInstance instance = mob.getAttribute(key);
        if (instance != null) instance.setBaseValue(value);
    }
}
