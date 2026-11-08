package com.eretenkodaniil.mkm.client.environment;

import com.eretenkodaniil.mkm.environment.EnvironmentVisualSettings;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Brief, local, non-flashing profile feedback; always hidden with vanilla F1 HUD. */
public final class EnvironmentProfileHud {
    private static long visibleUntilTick = Long.MIN_VALUE;
    private static Component message = Component.empty();

    private EnvironmentProfileHud() {}

    public static void onProfileChange(Minecraft client, EnvironmentVisualSettings.Profile profile) {
        message = Component.translatable("message.mkm.environment.profile", profile.name());
        visibleUntilTick = client.level == null ? Long.MIN_VALUE : client.level.getGameTime() + 80;
    }

    public static void onReducedChange(Minecraft client, boolean reduced) {
        message = Component.translatable("message.mkm.environment.reduced_effects", reduced);
        visibleUntilTick = client.level == null ? Long.MIN_VALUE : client.level.getGameTime() + 80;
    }

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.gui.screen() != null
                || client.level.getGameTime() > visibleUntilTick) return;
        int x = Math.max(5, client.getWindow().getGuiScaledWidth() - client.font.width(message) - 12);
        graphics.text(client.font, message, x, 10, 0xFFE4C8A9);
    }
}
