package com.eretenkodaniil.mkm.combat;

/**
 * Server startup-only A/B switch. No client request can set guard economics.
 * Default remains D3's original 1.0x until independent gameplay approval.
 */
public final class GuardCostExperiment {
    public static final String PROPERTY = "mkm.guard.costFactor";
    public static final double CONTROL = 1.0D;
    public static final double EXPERIMENTAL = 1.5D;
    private static final double ACTIVE = parse(System.getProperty(PROPERTY));

    private GuardCostExperiment() {}

    public static double activeFactor() { return ACTIVE; }

    public static double parse(String setting) {
        if (setting == null || setting.isBlank() || setting.equals("1.0")) return CONTROL;
        if (setting.equals("1.5")) return EXPERIMENTAL;
        // Invalid input must never allow free guard or uncapped cost.
        return CONTROL;
    }
}
