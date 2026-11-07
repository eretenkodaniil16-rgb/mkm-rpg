package com.eretenkodaniil.mkm.encounter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable server-level event journal. Absence from the currently loaded
 * entity index NEVER means death. Terminal records are never evicted or restarted.
 * This is distinct from the older player-owned Ilra/Husk Ford quest attachment.
 */
public record EncounterLedger(Map<String, Entry> encounters) {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_HISTORY = 128;
    public static final int MAX_CONCURRENT_MOBS = 6;
    public static final String RESERVED = "reserved", ACTIVE = "active",
            DEFEATED = "defeated", CLAIMED = "claimed";

    public record Member(String uuid, String entityId, String role, boolean elite, boolean dead) {
        public static final Codec<Member> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("uuid").forGetter(Member::uuid),
                Codec.STRING.fieldOf("entity").forGetter(Member::entityId),
                Codec.STRING.fieldOf("role").forGetter(Member::role),
                Codec.BOOL.fieldOf("elite").forGetter(Member::elite),
                Codec.BOOL.fieldOf("dead").forGetter(Member::dead)
        ).apply(i, Member::new));

        public Member {
            if (uuid == null || uuid.isBlank() || entityId == null || entityId.isBlank()
                    || role == null || role.isBlank()) throw new IllegalArgumentException("Invalid member");
        }
        public Member killed() { return new Member(uuid, entityId, role, elite, true); }
    }

    public record Entry(int schemaVersion, String ownerId, int x, int y, int z,
                        int rewardXp, String phase, List<Member> members) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("schema_version").forGetter(Entry::schemaVersion),
                Codec.STRING.fieldOf("owner_uuid").forGetter(Entry::ownerId),
                Codec.INT.fieldOf("x").forGetter(Entry::x),
                Codec.INT.fieldOf("y").forGetter(Entry::y),
                Codec.INT.fieldOf("z").forGetter(Entry::z),
                Codec.INT.fieldOf("reward_xp").forGetter(Entry::rewardXp),
                Codec.STRING.fieldOf("phase").forGetter(Entry::phase),
                Member.CODEC.listOf().fieldOf("members").forGetter(Entry::members)
        ).apply(i, Entry::new));
        public Entry {
            members = List.copyOf(members);
            if (schemaVersion != SCHEMA_VERSION || ownerId == null || ownerId.isBlank()
                    || rewardXp < 1 || rewardXp > 1_000
                    || !List.of(RESERVED, ACTIVE, DEFEATED, CLAIMED).contains(phase)
                    || members.isEmpty() || members.size() > 6
                    || members.stream().map(Member::uuid).distinct().count() != members.size()) {
                throw new IllegalArgumentException("Invalid encounter save schema/roster");
            }
        }
        public Entry start() { return phase.equals(RESERVED)
                ? new Entry(schemaVersion, ownerId, x, y, z, rewardXp, ACTIVE, members) : this; }
        public Entry markDeath(String uuid) {
            if (!phase.equals(RESERVED) && !phase.equals(ACTIVE)) return this;
            boolean changed = false;
            List<Member> after = new ArrayList<>(members.size());
            for (Member m : members) {
                if (!m.dead() && m.uuid().equals(uuid)) {
                    after.add(m.killed());
                    changed = true;
                } else after.add(m);
            }
            if (!changed) return this;
            String stage = after.stream().allMatch(Member::dead) ? DEFEATED : phase;
            return new Entry(schemaVersion, ownerId, x, y, z, rewardXp, stage, after);
        }
        public Entry claim() { return phase.equals(DEFEATED)
                ? new Entry(schemaVersion, ownerId, x, y, z, rewardXp, CLAIMED, members) : this; }
        public int aliveCount() { return (int) members.stream().filter(m -> !m.dead()).count(); }

        /** Whitelist-only transitions; owner, reward, spawn anchor and roster identity never change. */
        public boolean mayAdvanceTo(Entry next) {
            if (next == null || schemaVersion != next.schemaVersion
                    || !ownerId.equals(next.ownerId) || x != next.x || y != next.y || z != next.z
                    || rewardXp != next.rewardXp || members.size() != next.members.size()) return false;
            for (int i = 0; i < members.size(); i++) {
                Member before = members.get(i), after = next.members.get(i);
                if (!before.uuid().equals(after.uuid()) || !before.entityId().equals(after.entityId())
                        || !before.role().equals(after.role()) || before.elite() != after.elite()
                        || before.dead() && !after.dead()) return false;
            }
            if (phase.equals(CLAIMED) || phase.equals(DEFEATED) && !next.phase.equals(CLAIMED)) return false;
            if (phase.equals(RESERVED) && !(next.phase.equals(RESERVED)
                    || next.phase.equals(ACTIVE) || next.phase.equals(DEFEATED))) return false;
            if (phase.equals(ACTIVE) && !(next.phase.equals(ACTIVE)
                    || next.phase.equals(DEFEATED))) return false;
            if ((next.phase.equals(DEFEATED) || next.phase.equals(CLAIMED))
                    && next.aliveCount() != 0) return false;
            return true;
        }
    }

    public static final Codec<EncounterLedger> CODEC = Codec.unboundedMap(Codec.STRING, Entry.CODEC)
            .xmap(EncounterLedger::new, EncounterLedger::encounters);

    public EncounterLedger {
        encounters = Map.copyOf(encounters);
        if (encounters.size() > MAX_HISTORY) throw new IllegalArgumentException("Encounter history capacity exceeded");
    }
    public EncounterLedger() { this(Map.of()); }
    public Entry find(String id) { return encounters.get(id); }
    public int activeMobReservations() {
        return encounters.values().stream().filter(e -> e.phase().equals(RESERVED)
                || e.phase().equals(ACTIVE)).mapToInt(Entry::aliveCount).sum();
    }
    public boolean canReserve(String id, int count) {
        return id != null && !encounters.containsKey(id) && count > 0
                && count <= MAX_CONCURRENT_MOBS
                && encounters.size() < MAX_HISTORY
                && activeMobReservations() <= MAX_CONCURRENT_MOBS - count;
    }
    public EncounterLedger reserve(String id, Entry entry) {
        if (!canReserve(id, entry.members().size()) || !entry.phase().equals(RESERVED)) return this;
        Map<String, Entry> next = new HashMap<>(encounters);
        next.put(id, entry);
        return new EncounterLedger(next);
    }
    public EncounterLedger replace(String id, Entry old, Entry replacement) {
        if (old == null || replacement == null || old == replacement
                || encounters.get(id) != old || !old.mayAdvanceTo(replacement)) return this;
        Map<String, Entry> next = new HashMap<>(encounters);
        next.put(id, replacement);
        return new EncounterLedger(next);
    }
    public EncounterLedger markDeath(String id, String uuid) {
        Entry entry = find(id);
        return entry == null ? this : replace(id, entry, entry.markDeath(uuid));
    }
    public EncounterLedger claim(String id) {
        Entry entry = find(id);
        return entry == null ? this : replace(id, entry, entry.claim());
    }
}
