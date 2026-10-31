package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.MkmMod;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Runtime-only combat attachments. Combat state is synchronized to its player, but neither
 * stamina nor mob poise is persisted in CharacterData (schema v2 remains unchanged).
 */
public final class CombatAttachments {
    private static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MkmMod.MOD_ID);

    private static final StreamCodec<RegistryFriendlyByteBuf, CombatState> COMBAT_CODEC = new StreamCodec<>() {
        @Override
        public CombatState decode(RegistryFriendlyByteBuf buf) {
            return new CombatState(
                    buf.readFloat(), buf.readLong(), buf.readLong(), buf.readLong(), buf.readLong());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, CombatState state) {
            buf.writeFloat(state.stamina());
            buf.writeLong(state.lastUpdateTick());
            buf.writeLong(state.regenReadyTick());
            buf.writeLong(state.heavyReadyTick());
            buf.writeLong(state.dodgeReadyTick());
        }
    };

    public static final Supplier<AttachmentType<CombatState>> PLAYER = REGISTER.register(
            "combat",
            () -> AttachmentType.builder(CombatState::new)
                    .sync((holder, player) -> holder == player, COMBAT_CODEC)
                    .build());

    public static final Supplier<AttachmentType<PoiseState>> POISE = REGISTER.register(
            "poise", () -> AttachmentType.builder(PoiseState::new).build());

    private CombatAttachments() {}

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
