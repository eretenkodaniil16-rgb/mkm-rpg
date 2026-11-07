package com.eretenkodaniil.mkm;

import com.eretenkodaniil.mkm.abilities.AbilityAttachments;
import com.eretenkodaniil.mkm.abilities.AbilityService;
import com.eretenkodaniil.mkm.abilities.net.AbilityNetworking;
import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.net.CharacterNetworking;
import com.eretenkodaniil.mkm.character.CharacterEvents;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginAttachments;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginService;
import com.eretenkodaniil.mkm.character.origin.net.CharacterOriginNetworking;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.CombatEvents;
import com.eretenkodaniil.mkm.combat.CombatService;
import com.eretenkodaniil.mkm.combat.net.CombatNetworking;
import com.eretenkodaniil.mkm.command.MkmCommands;
import com.eretenkodaniil.mkm.encounter.EncounterAttachments;
import com.eretenkodaniil.mkm.encounter.EncounterCommands;
import com.eretenkodaniil.mkm.encounter.EncounterDefinitions;
import com.eretenkodaniil.mkm.encounter.EncounterDirector;
import com.eretenkodaniil.mkm.quests.FordQuestAttachments;
import com.eretenkodaniil.mkm.quests.FordQuestCommands;
import com.eretenkodaniil.mkm.quests.FordQuestService;
import com.eretenkodaniil.mkm.quests.net.FordQuestNetworking;
import com.eretenkodaniil.mkm.performance.MkmPerformanceRuntime;
import com.eretenkodaniil.mkm.performance.MkmPerformanceCommands;
import com.eretenkodaniil.mkm.registry.MkmCreativeTabs;
import com.eretenkodaniil.mkm.registry.MkmItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MkmMod implements ModInitializer {
    public static final String MOD_ID = "mkm";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        // Explicit static initialization registers Fabric attachments before player data is read.
        CharacterAttachments.initialize();
        CharacterOriginAttachments.initialize();
        AbilityAttachments.initialize();
        CombatAttachments.initialize();
        FordQuestAttachments.initialize();
        EncounterAttachments.initialize();
        EncounterDefinitions.initialize();
        MkmItems.initialize();
        MkmCreativeTabs.initialize();
        CombatNetworking.initialize();
        CharacterNetworking.initialize();
        CharacterOriginNetworking.initialize();
        AbilityNetworking.initialize();
        FordQuestNetworking.initialize();
        FordQuestService.initialize();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CharacterEvents.onPlayerLoggedIn(handler.player);
            CharacterOriginService.onLogin(handler.player);
            CombatService.onLogin(handler.player);
            AbilityService.onLogin(handler.player);
            FordQuestService.onLogin(handler.player);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                CombatService.onPlayerTick(player);
                AbilityService.onPlayerTick(player);
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register(CombatEvents::afterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(CombatEvents::afterDeath);
        ServerLivingEntityEvents.AFTER_DEATH.register(CharacterEvents::onEntityDeath);
        ServerLivingEntityEvents.AFTER_DEATH.register(FordQuestService::onDeath);
        ServerLivingEntityEvents.AFTER_DEATH.register(EncounterDirector::onDeath);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> MkmCommands.register(dispatcher));
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> FordQuestCommands.register(dispatcher));
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> EncounterCommands.register(dispatcher));
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> MkmPerformanceCommands.register(dispatcher));
        // Register the timing END callback after MKM's own end-tick work.
        MkmPerformanceRuntime.initialize();
        LOGGER.info("MKM Fabric 26.3 initialized");
    }
}
