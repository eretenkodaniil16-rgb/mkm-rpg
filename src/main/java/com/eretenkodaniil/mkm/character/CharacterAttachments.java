package com.eretenkodaniil.mkm.character;

import com.eretenkodaniil.mkm.MkmMod;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers persistent/synchronized character attachments.
 */
public final class CharacterAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MkmMod.MOD_ID);

    /**
     * Player RPG state. It is persisted to player data, survives death/respawn and is synchronized
     * only to the player that owns the attachment.
     */
    public static final Supplier<AttachmentType<CharacterData>> CHARACTER = ATTACHMENTS.register(
            "character",
            () -> AttachmentType.builder(CharacterData::new)
                    .serialize(CharacterData.CODEC)
                    .copyOnDeath()
                    .sync((holder, player) -> holder == player, CharacterData.STREAM_CODEC)
                    .build());

    private CharacterAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
