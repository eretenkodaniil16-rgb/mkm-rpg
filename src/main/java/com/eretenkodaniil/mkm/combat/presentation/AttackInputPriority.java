package com.eretenkodaniil.mkm.combat.presentation;

/**
 * A single input-precedence contract shared with MNAE.
 * It controls whether MKM MAY emit cosmetic combat presentation.
 * It does not cancel or synthesize vanilla Minecraft input.
 */
public final class AttackInputPriority {
    public enum Intent { LMB, SHIFT_LMB, R_SPECIAL, G_DODGE, USE_ITEM, LMB_RELEASE }
    public enum Decision {
        VANILLA_MINING, GUI_OR_SPECTATOR, ITEM_USE_OR_SHIELD,
        EXISTING_SPECIAL, EXISTING_DODGE, EXISTING_HEAVY, VANILLA_SWING,
        NO_ACTION
    }
    public record Context(boolean guiOpen, boolean spectator, boolean lookingAtBlock,
                          boolean usingItem, boolean actionTimelineBusy) {}

    private AttackInputPriority() {}

    public static Decision classify(Intent intent, Context context) {
        if (intent == null || context == null) throw new IllegalArgumentException("Input is required");
        if (context.guiOpen() || context.spectator()) return Decision.GUI_OR_SPECTATOR;
        if (context.usingItem() || intent == Intent.USE_ITEM) return Decision.ITEM_USE_OR_SHIELD;
        if (intent == Intent.G_DODGE) return Decision.EXISTING_DODGE;
        if (intent == Intent.R_SPECIAL) return Decision.EXISTING_SPECIAL;
        if (intent == Intent.LMB_RELEASE) return Decision.NO_ACTION;
        if (context.lookingAtBlock()) return Decision.VANILLA_MINING;
        if (context.actionTimelineBusy()) return Decision.NO_ACTION;
        return intent == Intent.SHIFT_LMB ? Decision.EXISTING_HEAVY : Decision.VANILLA_SWING;
    }
}
