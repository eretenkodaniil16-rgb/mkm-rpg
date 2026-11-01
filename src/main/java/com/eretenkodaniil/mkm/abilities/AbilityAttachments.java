package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.MkmMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Independent persistent ability resource ledger: no CharacterData migration and
 * no changes to CombatState or ActionTimeline. Owner-only synchronization.
 */
public final class AbilityAttachments {
    public static final Codec<ManaLedger> PERSISTENT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("mana").forGetter(ManaLedger::mana),
                    Codec.LONG.fieldOf("last_tick").forGetter(ManaLedger::lastUpdateTick),
                    Codec.LONG.fieldOf("regen_ready_tick").forGetter(ManaLedger::regenReadyTick),
                    Codec.unboundedMap(Codec.STRING, Codec.LONG)
                            .optionalFieldOf("cooldowns", Map.of())
                            .forGetter(ManaLedger::cooldowns)
            ).apply(instance, ManaLedger::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManaLedger> NETWORK_CODEC = new StreamCodec<>() {
        @Override
        public ManaLedger decode(RegistryFriendlyByteBuf buf) {
            double mana = buf.readDouble();
            long lastTick = buf.readLong();
            long regenTick = buf.readLong();
            int count = buf.readVarInt();
            if (count < 0 || count > ManaLedger.MAX_COOLDOWNS) {
                throw new IllegalArgumentException("Excessive ability cooldown count");
            }
            Map<String, Long> cooldowns = new HashMap<>();
            for (int i = 0; i < count; i++) {
                String id = buf.readUtf(64);
                long ready = buf.readLong();
                if (cooldowns.putIfAbsent(id, ready) != null) {
                    throw new IllegalArgumentException("Duplicate cooldown ID");
                }
            }
            return new ManaLedger(mana, lastTick, regenTick, cooldowns);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ManaLedger state) {
            buf.writeDouble(state.mana());
            buf.writeLong(state.lastUpdateTick());
            buf.writeLong(state.regenReadyTick());
            buf.writeVarInt(state.cooldowns().size());
            new TreeMap<>(state.cooldowns()).forEach((id, tick) -> {
                buf.writeUtf(id);
                buf.writeLong(tick);
            });
        }
    };

    public static final AttachmentType<ManaLedger> PLAYER = AttachmentRegistry.create(
            MkmMod.id("ability_resources"), builder -> builder
                    .initializer(ManaLedger::new)
                    .persistent(PERSISTENT_CODEC)
                    .copyOnDeath()
                    .syncWith(NETWORK_CODEC, AttachmentSyncPredicate.targetOnly()));

    private AbilityAttachments() {}

    public static void initialize() {
        PLAYER.identifier();
    }
}
