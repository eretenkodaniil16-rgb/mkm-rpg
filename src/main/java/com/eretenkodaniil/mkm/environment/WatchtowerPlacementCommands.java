package com.eretenkodaniil.mkm.environment;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.registry.MkmBlocks;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/**
 * Operator-selected, server-thread-only, no-overwrite pilot placement.
 * No background generation, force-loaded chunks, worldgen registry or mob spawning.
 */
public final class WatchtowerPlacementCommands {
    private WatchtowerPlacementCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mkmenv")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("watchtower")
                        .then(Commands.argument("variant", IntegerArgumentType.integer(1, 3))
                                .then(Commands.argument("x", IntegerArgumentType.integer(-29999000, 29999000))
                                        .then(Commands.argument("y", IntegerArgumentType.integer(-64, 400))
                                                .then(Commands.argument("z", IntegerArgumentType.integer(-29999000, 29999000))
                                                        .executes(ctx -> place(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "variant"),
                                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                                IntegerArgumentType.getInteger(ctx, "y"),
                                                                IntegerArgumentType.getInteger(ctx, "z")))))))));
    }

    public static int place(CommandSourceStack source, int variant, int x, int y, int z) {
        ServerLevel level = source.getLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return reject(source, "Overworld only");
        BlockPos origin = new BlockPos(x, y, z);
        if (level.isOutsideBuildHeight(y - WatchtowerSiteRules.MAX_SUPPORT_DEPTH)
                || level.isOutsideBuildHeight(y + WatchtowerSiteRules.HEIGHT - 1)) {
            return reject(source, "Outside world build-height limits");
        }
        // Strictly avoid loading new chunks and prevent border-crossing placements.
        for (int dx = 0; dx < WatchtowerSiteRules.WIDTH; dx++) {
            for (int dz = 0; dz < WatchtowerSiteRules.DEPTH; dz++) {
                BlockPos at = origin.offset(dx, 0, dz);
                if (!level.hasChunkAt(at) || !level.getWorldBorder().isWithinBounds(at)) {
                    return reject(source, "All site chunks must already be loaded and inside world border");
                }
            }
        }
        var id = MkmMod.id("ruined_watchtower_0" + variant);
        var templateOptional = level.getStructureTemplateManager().get(id);
        if (templateOptional.isEmpty()) return reject(source, "Template is missing: " + id);
        var template = templateOptional.get();
        if (template.getSize().getX() != 23 || template.getSize().getY() != 14
                || template.getSize().getZ() != 19) return reject(source, "Unrecognized template dimensions");

        WatchtowerSiteRules.Probe probe = new WatchtowerSiteRules.Probe() {
            @Override
            public boolean isAir(int dx, int dy, int dz) {
                return level.getBlockState(origin.offset(dx, dy, dz)).isAir();
            }

            @Override
            public boolean sturdyBelow(int dx, int dy, int dz) {
                var at = origin.offset(dx, dy, dz);
                var state = level.getBlockState(at);
                return state.getFluidState().isEmpty()
                        && state.isFaceSturdy(level, at, Direction.UP);
            }
        };
        if (!WatchtowerSiteRules.canPlace(probe)) {
            return reject(source, "Blocked or uneven site: no blocks may be overwritten; support slope <= 4");
        }
        // Preflight is complete before the first world mutation. Structure has NO air entries.
        if (!template.placeInWorld(level, origin, origin,
                new StructurePlaceSettings(), RandomSource.create(), 3)) {
            return reject(source, "Vanilla placement returned failure; inspect site before retrying");
        }
        // Extend isolated foundations downward through AIR only. Never remove substrate.
        for (int[] column : WatchtowerSiteRules.supportColumns()) {
            for (int d = 1; d <= WatchtowerSiteRules.MAX_SUPPORT_DEPTH; d++) {
                BlockPos at = origin.offset(column[0], -d, column[1]);
                if (!level.getBlockState(at).isAir()) break;
                level.setBlock(at, (column[0] >= 16 ? MkmBlocks.CHARRED_TIMBER :
                        MkmBlocks.AGED_ASHLAR).defaultBlockState(), 3);
            }
        }
        // Two-wide stair approach from footpath Y4 down to source ground Y0.
        for (int i = 0; i < 4; i++) {
            int relativeZ = 20 - i;
            for (int dx : new int[]{8, 9}) {
                for (int lowY = 0; lowY < i; lowY++) {
                    level.setBlock(origin.offset(dx, lowY, relativeZ),
                            MkmBlocks.CHARRED_TIMBER.defaultBlockState(), 3);
                }
                level.setBlock(origin.offset(dx, i, relativeZ),
                        Blocks.OAK_STAIRS.defaultBlockState()
                                .setValue(StairBlock.FACING, Direction.NORTH), 3);
            }
        }
        // Explicit post-placement diagnostics: boolean placeInWorld alone does not prove
        // a resource palette was decoded into the intended registered blocks.
        String runeId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(origin.offset(7, 5, 6)).getBlock()).toString();
        String wallId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(origin.offset(4, 5, 4)).getBlock()).toString();
        String barrelId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(origin.offset(11, 5, 5)).getBlock()).toString();
        source.sendSuccess(() -> Component.literal(
                "MKM: watchtower " + variant + " placed at " + origin
                        + "; all target blocks were air before placement"
                        + "; rune=" + runeId + "; wall=" + wallId + "; barrel=" + barrelId), true);
        return 1;
    }

    private static int reject(CommandSourceStack source, String detail) {
        source.sendFailure(Component.literal("MKM watchtower aborted: " + detail));
        return 0;
    }
}
