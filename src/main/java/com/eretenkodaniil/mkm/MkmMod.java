package com.eretenkodaniil.mkm;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.CharacterEvents;
import com.eretenkodaniil.mkm.command.MkmCommands;
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
        CharacterAttachments.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(CharacterEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(MkmCommands::onRegisterCommands);

        LOGGER.info("MKM bootstrap initialized");
    }
}
