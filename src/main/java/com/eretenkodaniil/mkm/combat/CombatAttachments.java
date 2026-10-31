package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.MkmMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Volatile synchronized owner resources; poise stays private to the server. */
public final class CombatAttachments {
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

    public static final AttachmentType<CombatState> PLAYER = AttachmentRegistry.create(
            MkmMod.id("combat"), builder -> builder
                    .initializer(CombatState::new)
                    .syncWith(COMBAT_CODEC, AttachmentSyncPredicate.targetOnly()));

    public static final AttachmentType<PoiseState> POISE = AttachmentRegistry.createDefaulted(
            MkmMod.id("poise"), PoiseState::new);

    private CombatAttachments() {}

    public static void initialize() {
        PLAYER.identifier();
        POISE.identifier();
    }
}
