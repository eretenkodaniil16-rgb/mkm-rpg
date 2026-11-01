package com.eretenkodaniil.mkm;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.CharacterEvents;
import com.eretenkodaniil.mkm.character.net.CharacterNetworking;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.CombatEvents;
import com.eretenkodaniil.mkm.combat.CombatService;
import com.eretenkodaniil.mkm.combat.net.CombatNetworking;
import com.eretenkodaniil.mkm.command.MkmCommands;
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
        CombatAttachments.initialize();
        MkmItems.initialize();
        MkmCreativeTabs.initialize();
        CombatNetworking.initialize();
        CharacterNetworking.initialize();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CharacterEvents.onPlayerLoggedIn(handler.player);
            CombatService.onLogin(handler.player);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                CombatService.onPlayerTick(player);
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register(CombatEvents::afterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(CombatEvents::afterDeath);
        ServerLivingEntityEvents.AFTER_DEATH.register(CharacterEvents::onEntityDeath);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> MkmCommands.register(dispatcher));
        LOGGER.info("MKM Fabric 26.3 initialized");
    }
}
