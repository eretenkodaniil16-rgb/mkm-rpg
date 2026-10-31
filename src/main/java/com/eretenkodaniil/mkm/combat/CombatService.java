package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Runtime stamina and movement authority. Player combat attachments are volatile and
 * owner-synchronized; clients never choose cost, cooldown or displacement.
 */
public final class CombatService {
    private static final long SYNC_INTERVAL_TICKS = 5L;
    private static final double DODGE_IMPULSE = 0.9D;

    private CombatService() {}

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getData(CombatAttachments.PLAYER);
            player.syncData(CombatAttachments.PLAYER);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long now = player.level().getGameTime();
        if (now % SYNC_INTERVAL_TICKS != 0L) {
            return;
        }

        PlayerModifierSnapshot modifiers = modifiers(player);
        float max = (float) modifiers.get(PlayerModifierId.MAX_STAMINA);
        float regen = (float) modifiers.get(PlayerModifierId.STAMINA_REGENERATION);
        CombatState current = player.getData(CombatAttachments.PLAYER);
        // Do not send attachment updates when only the bookkeeping timestamp changed.
        if (Math.abs(current.stamina() - max) < 0.001F) {
            return;
        }
        CombatState next = CombatRules.regenerate(current, now, max, regen);
        if (Math.abs(next.stamina() - current.stamina()) > 0.001F) {
            player.setData(CombatAttachments.PLAYER, next);
        }
        // Action bar is interim feedback, not a permanent HUD (which will use synced attachment).
        if (now % 10L == 0 && next.stamina() < max - 0.1F) {
            player.displayClientMessage(Component.literal(String.format(
                    Locale.ROOT, "MKM  Stamina %.0f / %.0f", next.stamina(), max)), true);
        }
    }

    public static boolean tryHeavy(ServerPlayer player) {
        long now = player.level().getGameTime();
        PlayerModifierSnapshot modifiers = modifiers(player);
        CombatState current = fresh(player, modifiers, now);
        float cost = (float) (CombatRules.HEAVY_BASE_COST
                * modifiers.get(PlayerModifierId.STAMINA_COST));
        CombatState next = CombatRules.heavy(current, now, cost, regenerationDelay(modifiers));
        if (next == null) {
            return false;
        }
        player.setData(CombatAttachments.PLAYER, next);
        player.displayClientMessage(Component.literal(String.format(
                Locale.ROOT, "Heavy strike  -%.0f stamina", cost)), true);
        return true;
    }

    public static void tryDodge(ServerPlayer player, byte direction) {
        // A one-byte packet is just an intent. Never trust a client-supplied velocity or position.
        if (direction < 0 || direction > 3
                || player.isSpectator() || player.isPassenger() || !player.onGround()) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 0.0001D) {
            return;
        }
        Vec3 forward = flat.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 axis = switch (direction) {
            case 0 -> forward;
            case 1 -> forward.scale(-1.0D);
            case 2 -> right.scale(-1.0D);
            default -> right;
        };

        long now = player.level().getGameTime();
        PlayerModifierSnapshot modifiers = modifiers(player);
        CombatState current = fresh(player, modifiers, now);
        float cost = (float) (CombatRules.DODGE_BASE_COST
                * modifiers.get(PlayerModifierId.DODGE_COST)
                * modifiers.get(PlayerModifierId.STAMINA_COST));
        CombatState next = CombatRules.dodge(current, now, cost, regenerationDelay(modifiers));
        if (next == null) {
            return;
        }

        // Server-side impulse uses bounded modifiers; no teleport and no invulnerability frames.
        double scale = DODGE_IMPULSE * modifiers.get(PlayerModifierId.DODGE_DISTANCE);
        player.push(axis.x * scale, 0.06D, axis.z * scale);
        player.hurtMarked = true;
        player.setData(CombatAttachments.PLAYER, next);
        player.displayClientMessage(Component.literal(String.format(
                Locale.ROOT, "Dodge  -%.0f stamina", cost)), true);
    }

    private static CombatState fresh(
            ServerPlayer player, PlayerModifierSnapshot modifiers, long now) {
        return CombatRules.regenerate(
                player.getData(CombatAttachments.PLAYER), now,
                (float) modifiers.get(PlayerModifierId.MAX_STAMINA),
                (float) modifiers.get(PlayerModifierId.STAMINA_REGENERATION));
    }

    private static long regenerationDelay(PlayerModifierSnapshot modifiers) {
        return Math.max(1L, Math.round(
                20.0D * modifiers.get(PlayerModifierId.STAMINA_REGEN_DELAY)));
    }

    private static PlayerModifierSnapshot modifiers(ServerPlayer player) {
        return PlayerModifierCalculator.calculate(player.getData(CharacterAttachments.CHARACTER));
    }
}
