package com.eretenkodaniil.mkm.showcase;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

/** One operator-prepared protected tower per Overworld save; never auto-places. */
public record ShowcaseSite(int schemaVersion, boolean configured, int x, int y, int z,
                           String keeperUuid) {
    public static final int SCHEMA_VERSION = 1;
    public static final Codec<ShowcaseSite> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("schema_version", 1).forGetter(ShowcaseSite::schemaVersion),
            Codec.BOOL.optionalFieldOf("configured", false).forGetter(ShowcaseSite::configured),
            Codec.INT.optionalFieldOf("x", 0).forGetter(ShowcaseSite::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(ShowcaseSite::y),
            Codec.INT.optionalFieldOf("z", 0).forGetter(ShowcaseSite::z),
            Codec.STRING.optionalFieldOf("keeper_uuid", "").forGetter(ShowcaseSite::keeperUuid)
    ).apply(i, ShowcaseSite::new));
    public ShowcaseSite {
        if (schemaVersion != SCHEMA_VERSION || keeperUuid == null || keeperUuid.length() > 36
                || !configured && !keeperUuid.isBlank())
            throw new IllegalArgumentException("Invalid Showcase world site");
    }
    public ShowcaseSite() { this(SCHEMA_VERSION,false,0,0,0,""); }
    public BlockPos origin() { return new BlockPos(x,y,z); }
    public BlockPos runePos() { return origin().offset(7,5,6); }
    public BlockPos encounterAnchor() { return origin().offset(9,0,32); }
    public ShowcaseSite place(BlockPos pos) {
        if (configured) return this;
        return new ShowcaseSite(SCHEMA_VERSION,true,pos.getX(),pos.getY(),pos.getZ(),"");
    }
    public ShowcaseSite withKeeper(String uuid) {
        if (!configured || !keeperUuid.isBlank() || uuid == null || uuid.isBlank()) return this;
        return new ShowcaseSite(SCHEMA_VERSION,true,x,y,z,uuid);
    }
}
