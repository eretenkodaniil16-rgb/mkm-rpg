package com.eretenkodaniil.mkm.performance;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Operator-only, read-only diagnostic commands. */
public final class MkmPerformanceCommands {
    private MkmPerformanceCommands() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mkmperf")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> showTicks(ctx.getSource()))
                .then(Commands.literal("ticks").executes(ctx -> showTicks(ctx.getSource())))
                .then(Commands.literal("compat").executes(ctx -> showCompatibility(ctx.getSource()))));
    }

    private static int showTicks(CommandSourceStack source) {
        var s = MkmPerformanceRuntime.tickSummary();
        if (s.samples() == 0) {
            source.sendSuccess(() -> Component.literal("MKM Performance: no server tick samples yet."), false);
            return Command.SINGLE_SUCCESS;
        }
        String report = String.format(Locale.ROOT,
                "MKM tick callback span (approx, not spark MSPT) | n=%d mean=%.2fms p50=%.2fms p95=%.2fms p99=%.2fms max=%.2fms | budget=%s (advisory only)",
                s.samples(), s.meanMs(), s.p50Ms(), s.p95Ms(), s.p99Ms(), s.maxMs(),
                PerformanceBudget.classify(s));
        source.sendSuccess(() -> Component.literal(report), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int showCompatibility(CommandSourceStack source) {
        var report = MkmPerformanceRuntime.compatibility();
        source.sendSuccess(() -> Component.literal("MKM optimization mods: "
                + (report.loadedOptimizers().isEmpty() ? "none detected" : String.join(", ", report.loadedOptimizers()))
                + " | chunks=" + report.chunkBackend()), false);
        for (String warning : report.warnings()) {
            source.sendSuccess(() -> Component.literal("MKM WARNING: " + warning), false);
        }
        source.sendSuccess(() -> Component.literal(
                "MKM M0 is diagnostics-only; optional optimization mods are user-managed, not bundled."), false);
        return Command.SINGLE_SUCCESS;
    }
}
