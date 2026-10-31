package com.eretenkodaniil.mkm;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.CharacterEvents;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.CombatEvents;
import com.eretenkodaniil.mkm.combat.CombatService;
import com.eretenkodaniil.mkm.combat.net.CombatNetworking;
import com.eretenkodaniil.mkm.registry.MkmCreativeTabs;
import com.eretenkodaniil.mkm.registry.MkmItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Common entry point for MKM.
 *
 * <p>This class must remain safe to load on a dedicated server. Client-only Minecraft classes
 * belong under {@code com.eretenkodaniil.mkm.client} and must never be referenced from common
 * bootstrap code.</p>
 */
@Mod(MkmMod.MOD_ID)
public final class MkmMod {
    public static final String MOD_ID = "mkm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MkmMod(IEventBus modEventBus, ModContainer modContainer) {
        MkmItems.register(modEventBus);
        MkmCreativeTabs.register(modEventBus);
        CharacterAttachments.register(modEventBus);
        CombatAttachments.register(modEventBus);
        modEventBus.addListener(CombatNetworking::register);

        NeoForge.EVENT_BUS.addListener(CharacterEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(CombatService::onLogin);
        NeoForge.EVENT_BUS.addListener(CombatService::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onDamagePre);

        LOGGER.info("MKM bootstrap initialized");
    }
}
