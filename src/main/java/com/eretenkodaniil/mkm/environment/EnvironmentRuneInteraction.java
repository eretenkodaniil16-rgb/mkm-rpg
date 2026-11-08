package com.eretenkodaniil.mkm.environment;

import com.eretenkodaniil.mkm.registry.MkmBlocks;
import com.eretenkodaniil.mkm.environment.net.EnvironmentVisualNetworking;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/**
 * Small, original interactive altar: right-click rune -> chime + local pulse.
 * Invoked on server only, no mana/XP/loot effects and no click packet of our own.
 */
public final class EnvironmentRuneInteraction {
    private static final Map<Player, Long> LAST_PULSE = new WeakHashMap<>();

    private EnvironmentRuneInteraction() {}

    public static void initialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || player.isSpectator()
                    || !world.getBlockState(hit.getBlockPos()).is(MkmBlocks.RUNE_SLATE))
                return InteractionResult.PASS;
            if (world instanceof ServerLevel level) {
                long now = level.getGameTime();
                Long last = LAST_PULSE.get(player);
                if (last == null || now - last >= 20) {
                    LAST_PULSE.put(player, now);
                    EnvironmentVisualNetworking.runePulse(level, hit.getBlockPos());
                }
            }
            return InteractionResult.SUCCESS;
        });
    }
}
