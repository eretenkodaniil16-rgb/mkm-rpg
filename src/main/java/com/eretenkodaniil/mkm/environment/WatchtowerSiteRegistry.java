package com.eretenkodaniil.mkm.environment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Persistent, server-level placement proof for the authored watchtower rune.
 * The entry is reserved BEFORE NBT placement and finalized only after all
 * blocks and the stair/foundation work have completed. A partial crash stays
 * PENDING: no XP is ever issued on an unverified build.
 */
public record WatchtowerSiteRegistry(int schemaVersion, List<Site> sites) {
    public static final int VERSION = 1;
    public static final int MAX_SITES = 32;

    public record Site(int x, int y, int z, int variant, String phase) {
        public static final String PENDING = "pending";
        public static final String PLACED = "placed";
        public static final Codec<Site> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Site::x),
                Codec.INT.fieldOf("y").forGetter(Site::y),
                Codec.INT.fieldOf("z").forGetter(Site::z),
                Codec.INT.fieldOf("variant").forGetter(Site::variant),
                Codec.STRING.fieldOf("phase").forGetter(Site::phase)
        ).apply(i, Site::new));

        public Site {
            if (variant < 1 || variant > 3 || (!PENDING.equals(phase) && !PLACED.equals(phase)))
                throw new IllegalArgumentException("Invalid watchtower placement record");
        }
        public boolean matches(BlockPos p) {
            return x == p.getX() && y == p.getY() && z == p.getZ();
        }
        public boolean isRune(BlockPos p) {
            return PLACED.equals(phase) && x + 7 == p.getX()
                    && y + 5 == p.getY() && z + 6 == p.getZ();
        }
        public Site complete() {
            return new Site(x, y, z, variant, PLACED);
        }
    }

    public static final Codec<WatchtowerSiteRegistry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("schema_version", VERSION).forGetter(WatchtowerSiteRegistry::schemaVersion),
            Site.CODEC.listOf().optionalFieldOf("sites", List.of()).forGetter(WatchtowerSiteRegistry::sites)
    ).apply(i, WatchtowerSiteRegistry::new));

    public WatchtowerSiteRegistry {
        if (schemaVersion < 1 || sites == null || sites.size() > MAX_SITES)
            throw new IllegalArgumentException("Invalid watchtower registry");
        sites = List.copyOf(sites);
        var seen = new HashSet<String>();
        for (Site site : sites) {
            String location = site.x() + ":" + site.y() + ":" + site.z();
            if (!seen.add(location)) throw new IllegalArgumentException("Duplicate watchtower site");
        }
    }

    public WatchtowerSiteRegistry() { this(VERSION, List.of()); }

    public boolean canReserve(BlockPos anchor) {
        return schemaVersion == VERSION && sites.size() < MAX_SITES
                && sites.stream().noneMatch(x -> x.matches(anchor));
    }

    public WatchtowerSiteRegistry reserve(BlockPos anchor, int variant) {
        if (!canReserve(anchor)) return this;
        var entries = new ArrayList<>(sites);
        entries.add(new Site(anchor.getX(), anchor.getY(), anchor.getZ(), variant, Site.PENDING));
        return new WatchtowerSiteRegistry(schemaVersion, entries);
    }

    public WatchtowerSiteRegistry finish(BlockPos anchor) {
        if (schemaVersion != VERSION) return this;
        var entries = new ArrayList<Site>(sites.size());
        boolean updated = false;
        for (Site site : sites) {
            if (site.matches(anchor) && site.phase().equals(Site.PENDING)) {
                entries.add(site.complete());
                updated = true;
            } else entries.add(site);
        }
        return updated ? new WatchtowerSiteRegistry(schemaVersion, entries) : this;
    }

    public boolean verifiedRune(BlockPos pos) {
        return schemaVersion == VERSION && sites.stream().anyMatch(site -> site.isRune(pos));
    }
}
