package com.eretenkodaniil.mkm.combat.enemy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registration-only store for approved special MOB features. One instance is
 * owned by the server adapter; pure test fixtures can make isolated instances.
 * Existing vanilla mobs have no MKM overlay until explicitly registered.
 */
public final class EnemyCombatFeatureRegistry {
    public static final int MAX_TYPES = 256;
    private volatile Map<String, EnemyCombatProfile> registered = Map.of();
    private boolean sealed;

    public synchronized void register(String entityTypeId, EnemyCombatProfile profile) {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        Objects.requireNonNull(profile, "profile");
        if (sealed) throw new IllegalStateException("Enemy combat profiles are sealed");
        if (entityTypeId.length() > 128
                || !entityTypeId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                || entityTypeId.equals("minecraft:player")) {
            throw new IllegalArgumentException("Only valid non-player entity IDs are accepted");
        }
        if (registered.containsKey(entityTypeId) || registered.size() >= MAX_TYPES) {
            throw new IllegalArgumentException("Duplicate or excessive enemy profile registrations");
        }
        Map<String, EnemyCombatProfile> next = new HashMap<>(registered);
        next.put(entityTypeId, profile);
        registered = Map.copyOf(next);
    }

    /** Called at server startup, no live registry changes during combat. */
    public synchronized void seal() {
        sealed = true;
    }

    public Optional<EnemyCombatProfile> find(String entityTypeId) {
        return Optional.ofNullable(registered.get(entityTypeId));
    }

    public boolean isSealed() {
        return sealed;
    }
}
