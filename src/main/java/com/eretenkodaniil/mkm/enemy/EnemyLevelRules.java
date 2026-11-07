package com.eretenkodaniil.mkm.enemy;

/** Deterministic, capped scaling. Apply exactly once at spawn, never on chunk reload. */
public final class EnemyLevelRules {
    public static final int MAX_LEVEL = 30;
    private EnemyLevelRules() {}

    public record Stats(int level, double health, double attack, double speed, long xp) {}

    public static Stats forLevel(EnemyDefinition definition, int requestedLevel) {
        int level = Math.max(1, Math.min(MAX_LEVEL, requestedLevel));
        int steps = level - 1;
        return new Stats(level,
                definition.baseHealth() * (1.0 + 0.11 * steps),
                definition.baseAttack() * (1.0 + 0.07 * steps),
                Math.min(0.42, definition.baseSpeed() + 0.002 * steps),
                definition.baseXp() + 3L * steps);
    }

    public static String levelTag(int level) {
        if (level < 1 || level > MAX_LEVEL) throw new IllegalArgumentException("Enemy level out of bounds");
        return "mkm.enemy.level." + level;
    }

    public static int readLevel(java.util.Set<String> tags) {
        for (int i = 1; i <= MAX_LEVEL; i++) {
            if (tags.contains(levelTag(i))) return i;
        }
        return 1;
    }
}
