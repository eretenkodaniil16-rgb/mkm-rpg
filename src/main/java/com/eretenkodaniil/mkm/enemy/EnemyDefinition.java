package com.eretenkodaniil.mkm.enemy;

import java.util.Objects;

/**
 * Stable content identity, separate from the temporary vanilla EntityType host.
 * This is a contract, not a second damage/XP engine.
 */
public record EnemyDefinition(
        String id, String persistentTag, Host host, Family family, Role role,
        int dangerTier, double baseHealth, double baseAttack, double baseSpeed,
        long baseXp, String rigProfile) {
    public enum Host { HUSK, SPIDER }
    public enum Family { HUMANOID, ARACHNID }
    public enum Role { SKIRMISHER, AMBUSHER, GUARD, ARCHER, CASTER, SUPPORT, BOSS }

    public EnemyDefinition {
        Objects.requireNonNull(id);
        Objects.requireNonNull(persistentTag);
        Objects.requireNonNull(host);
        Objects.requireNonNull(family);
        Objects.requireNonNull(role);
        Objects.requireNonNull(rigProfile);
        if (!id.matches("mkm:[a-z0-9_/-]+")
                || !persistentTag.matches("mkm\\.enemy\\.[a-z0-9_.-]+")
                || !rigProfile.startsWith("mkm:rig/")
                || dangerTier < 1 || dangerTier > 5
                || !Double.isFinite(baseHealth) || baseHealth <= 0.0
                || !Double.isFinite(baseAttack) || baseAttack < 0.0
                || !Double.isFinite(baseSpeed) || baseSpeed <= 0.0 || baseSpeed > 1.0
                || baseXp < 0L) {
            throw new IllegalArgumentException("Invalid enemy definition");
        }
    }
}
