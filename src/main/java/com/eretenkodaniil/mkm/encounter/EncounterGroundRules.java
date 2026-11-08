package com.eretenkodaniil.mkm.encounter;

import java.util.Set;

/**
 * Pure, registry-independent allowlist for natural Overworld mob footing.
 * Static policy is testable without running Minecraft block bootstrap.
 * Runtime call site obtains the actual registry ID from the server.
 */
public final class EncounterGroundRules {
    private static final Set<String> NATURAL_FOOTING = Set.of(
            "minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt",
            "minecraft:rooted_dirt", "minecraft:podzol", "minecraft:stone",
            "minecraft:andesite", "minecraft:tuff", "minecraft:gravel",
            "minecraft:sand", "minecraft:red_sand", "minecraft:moss_block",
            "minecraft:mud", "minecraft:clay", "minecraft:snow_block");
    private EncounterGroundRules() {}
    public static boolean isNaturalSurface(String registryId) {
        return NATURAL_FOOTING.contains(registryId);
    }
}
