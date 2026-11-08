package com.eretenkodaniil.mkm.quests;

/** Pure allowlist for player dialogue. Called before any quest or XP mutation. */
public final class FordDialogueRules {
    private FordDialogueRules() {}

    public static boolean permitted(byte stage, byte choice) {
        return switch(stage) {
            case 0 -> choice == 1 || choice == 2 || choice == 3 || choice == 6 || choice == 7;
            case 1 -> choice == 1 || choice == 3 || choice == 5 || choice == 6 || choice == 7;
            case 2 -> choice == 3 || choice == 4;
            case 3 -> choice == 3 || choice == 8;
            default -> false;
        };
    }
}
