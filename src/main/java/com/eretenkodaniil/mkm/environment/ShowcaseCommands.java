package com.eretenkodaniil.mkm.environment;

import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.encounter.EncounterDirector;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Safe player-readable discovery guide. No extra XP, entity spawns or client packets. */
public final class ShowcaseCommands {
    private static final String RUNE_REWARD = "mkm:showcase/watchtower_rune";
    private ShowcaseCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mkmshowcase")
                .executes(ctx -> guide(ctx.getSource()))
                .then(Commands.literal("guide").executes(ctx -> guide(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("ambush").executes(ctx -> ambush(ctx.getSource()))));
    }

    private static int guide(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.sendSystemMessage(Component.translatable("message.mkm.showcase.guide1"));
        player.sendSystemMessage(Component.translatable("message.mkm.showcase.guide2"));
        player.sendSystemMessage(Component.translatable("message.mkm.showcase.guide3"));
        return status(source);
    }

    /**
     * After registered rune discovery, players can trigger the EXISTING
     * EncounterDirector: same 3 mobs, same world-once reservation, same
     * source of XP. This exposes no new client packet or GM spawn loophole.
     */
    private static int ambush(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel world)) return 0;
        var origin = player.getAttached(CharacterOriginAttachments.PROFILE);
        boolean selected = origin != null
                && origin.schemaVersion() == 1 && origin.selected();
        boolean v4 = CharacterService.get(player).schemaVersion() == CharacterData.CURRENT_SCHEMA_VERSION;
        boolean receipt = CharacterService.hasExperienceReward(player, RUNE_REWARD);
        var sites = world.getAttached(WatchtowerAttachments.SITES);
        boolean near = sites != null && sites.sites().stream()
                .filter(site -> site.phase().equals(WatchtowerSiteRegistry.Site.PLACED))
                .anyMatch(site -> {
                    double dx = player.getX() - (site.x() + 11.5);
                    double dy = player.getY() - (site.y() + 5.0);
                    double dz = player.getZ() - (site.z() + 9.5);
                    return dx * dx + dy * dy + dz * dz
                            <= ShowcaseAmbushGate.MAX_DISTANCE_SQUARED;
                });
        if (!ShowcaseAmbushGate.allowed(player.isAlive(), player.isSpectator(),
                v4, selected, receipt, near)) {
            player.sendSystemMessage(Component.translatable("message.mkm.showcase.ambush_locked"));
            return 0;
        }
        // Downstream enforces overworld, loaded spawn positions, night/biome,
        // Peaceful denial, single event reservation and owner reward.
        return EncounterDirector.start(player) ? 1 : 0;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel world)) return 0;
        var origin = player.getAttached(CharacterOriginAttachments.PROFILE);
        boolean selected = origin != null && origin.schemaVersion() == 1 && origin.selected();
        player.sendSystemMessage(Component.translatable("message.mkm.showcase.status.origin",
                selected ? origin.raceId() + " / " + origin.classId() : "K"));
        var sites = world.getAttached(WatchtowerAttachments.SITES);
        if (sites == null || sites.sites().stream().noneMatch(site -> site.phase()
                .equals(WatchtowerSiteRegistry.Site.PLACED))) {
            player.sendSystemMessage(Component.translatable("message.mkm.showcase.no_site"));
        } else {
            var p = player.blockPosition();
            var closest = sites.sites().stream()
                    .filter(site -> site.phase().equals(WatchtowerSiteRegistry.Site.PLACED))
                    .min(java.util.Comparator.comparingDouble(site -> {
                        double dx = (double) site.x() - p.getX();
                        double dy = (double) site.y() - p.getY();
                        double dz = (double) site.z() - p.getZ();
                        return dx * dx + dy * dy + dz * dz;
                    })).orElseThrow();
            player.sendSystemMessage(Component.translatable("message.mkm.showcase.site",
                    closest.x(), closest.y(), closest.z(),
                    closest.x() + 7, closest.y() + 5, closest.z() + 6));
        }
        player.sendSystemMessage(Component.translatable("message.mkm.showcase.status.reward",
                CharacterService.hasExperienceReward(player, RUNE_REWARD)
                        ? "CLAIMED" : "AVAILABLE"));
        return 1;
    }
}
