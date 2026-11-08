package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.MkmMod;
import com.mojang.serialization.Codec;
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

    private static final StreamCodec<RegistryFriendlyByteBuf, ActionTimeline> TIMELINE_CODEC = new StreamCodec<>() {
        @Override
        public ActionTimeline decode(RegistryFriendlyByteBuf buf) {
            int kind = buf.readUnsignedByte();
            int phase = buf.readUnsignedByte();
            if (kind >= ActionTimeline.Kind.values().length
                    || phase >= ActionTimeline.Phase.values().length) {
                throw new IllegalArgumentException("Unknown MKM action timeline value");
            }
            return new ActionTimeline(
                    ActionTimeline.Kind.values()[kind],
                    ActionTimeline.Phase.values()[phase],
                    buf.readUnsignedByte(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readByte());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ActionTimeline value) {
            buf.writeByte(value.kind().ordinal());
            buf.writeByte(value.phase().ordinal());
            buf.writeByte(value.comboStep());
            buf.writeLong(value.phaseUntilTick());
            buf.writeLong(value.comboUntilTick());
            buf.writeByte(value.dodgeDirection());
        }
    };

    public static final AttachmentType<ActionTimeline> TIMELINE = AttachmentRegistry.create(
            MkmMod.id("action_timeline"), builder -> builder
                    .initializer(ActionTimeline::new)
                    .syncWith(TIMELINE_CODEC, AttachmentSyncPredicate.targetOnly()));

    public static final AttachmentType<PoiseState> POISE = AttachmentRegistry.createDefaulted(
            MkmMod.id("poise"), PoiseState::new);

    /** Server-only persisted cooldown across relog; resets on death, no client authority. */
    public static final AttachmentType<Long> BRACE_READY_TICK = AttachmentRegistry.create(
            MkmMod.id("warrior_brace_ready"), builder -> builder
                    .initializer(() -> 0L).persistent(Codec.LONG));

    private CombatAttachments() {}

    public static void initialize() {
        PLAYER.identifier();
        TIMELINE.identifier();
        POISE.identifier();
        BRACE_READY_TICK.identifier();
    }
}
