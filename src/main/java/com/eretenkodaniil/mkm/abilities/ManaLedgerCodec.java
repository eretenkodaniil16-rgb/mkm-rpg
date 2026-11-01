package com.eretenkodaniil.mkm.abilities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;

/**
 * Versioned disk codec kept independent of Fabric attachment registration so
 * save round-trips can be tested without starting a Minecraft server.
 */
public final class ManaLedgerCodec {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Codec<Integer> SCHEMA_CODEC = Codec.INT.flatXmap(
            value -> value >= 0 && value <= CURRENT_SCHEMA_VERSION
                    ? DataResult.success(value)
                    : DataResult.error(() -> "Unsupported ability ledger schema " + value),
            DataResult::success);

    // Schema 0 (the pre-review prototype) omitted schema_version. It decodes
    // as 0, while subsequent saves explicitly emit schema_version=1.
    public static final Codec<ManaLedger> PERSISTENT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    SCHEMA_CODEC.optionalFieldOf("schema_version", 0)
                            .forGetter(ledger -> CURRENT_SCHEMA_VERSION),
                    Codec.DOUBLE.fieldOf("mana").forGetter(ManaLedger::mana),
                    Codec.LONG.fieldOf("last_tick").forGetter(ManaLedger::lastUpdateTick),
                    Codec.LONG.fieldOf("regen_ready_tick").forGetter(ManaLedger::regenReadyTick),
                    Codec.unboundedMap(Codec.STRING, Codec.LONG)
                            .optionalFieldOf("cooldowns", Map.of())
                            .forGetter(ManaLedger::cooldowns)
            ).apply(instance, ManaLedgerCodec::decode));

    private ManaLedgerCodec() {}

    private static ManaLedger decode(int schema, double mana, long last,
                                    long regen, Map<String, Long> cooldowns) {
        return new ManaLedger(mana, last, regen, cooldowns);
    }
}
