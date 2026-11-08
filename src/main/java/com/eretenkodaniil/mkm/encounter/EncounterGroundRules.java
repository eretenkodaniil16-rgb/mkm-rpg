package com.eretenkodaniil.mkm.encounter;

import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Conservative authored-mob footing whitelist. Never use arbitrary player
 * construction, inventories or custom structure blocks as spawn platforms.
 * This is not an ownership/claim API; private builds made from natural
 * blocks still require operator-side scene checks.
 */
public final class EncounterGroundRules {
    private static final Set<Block> NATURAL_FOOTING = Set.of(
            Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
            Blocks.PODZOL, Blocks.STONE, Blocks.ANDESITE, Blocks.TUFF,
            Blocks.GRAVEL, Blocks.SAND, Blocks.RED_SAND, Blocks.MOSS_BLOCK,
            Blocks.MUD, Blocks.CLAY, Blocks.SNOW_BLOCK);
    private EncounterGroundRules() {}
    public static boolean isNaturalSurface(Block block) {
        return NATURAL_FOOTING.contains(block);
    }
}
