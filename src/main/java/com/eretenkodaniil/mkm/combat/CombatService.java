package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Runtime stamina and movement authority. Player combat attachments are volatile and
 * owner-synchronized; clients never choose cost, cooldown or displacement.
 */
public final class CombatService {
    private static final long SYNC_INTERVAL_TICKS = 5L;

    private CombatService() {}

    public static void onLogin(ServerPlayer player) {
        if (player.getAttached(CombatAttachments.PLAYER) == null) {
            player.setAttached(CombatAttachments.PLAYER, new CombatState());
        }
    }

    public static void onPlayerTick(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now % SYNC_INTERVAL_TICKS != 0L) {
            return;
        }

        PlayerModifierSnapshot modifiers = modifiers(player);
        float max = (float) modifiers.get(PlayerModifierId.MAX_STAMINA);
        float regen = (float) modifiers.get(PlayerModifierId.STAMINA_REGENERATION);
        CombatState current = player.getAttachedOrCreate(CombatAttachments.PLAYER);
        // Do not send attachment updates when only the bookkeeping timestamp changed.
        if (Math.abs(current.stamina() - max) < 0.001F) {
            return;
        }
        CombatState next = CombatRules.regenerate(current, now, max, regen);
        if (Math.abs(next.stamina() - current.stamina()) > 0.001F) {
            player.setAttached(CombatAttachments.PLAYER, next);
        }
        // Action bar is interim feedback, not a permanent HUD (which will use synced attachment).
        if (now % 10L == 0 && next.stamina() < max - 0.1F) {
            player.sendOverlayMessage(Component.literal(String.format(
                    Locale.ROOT, "MKM  Stamina %.0f / %.0f", next.stamina(), max)));
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
        player.setAttached(CombatAttachments.PLAYER, next);
        player.sendOverlayMessage(Component.literal(String.format(
                Locale.ROOT, "Heavy strike  -%.0f stamina", cost)));
        return true;
    }

    public static void tryDodge(ServerPlayer player, byte direction) {
        // A one-byte packet is just an intent. Never trust a client-supplied velocity or position.
        if (direction < 0 || direction > 3
                || player.isSpectator() || player.isPassenger() || !player.onGround()) {
            return;
        }
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

        // Clients simulate local-player movement. Updating only the server-side
        // velocity was nearly invisible to the controlling player.
        //
        // A bounded impulse is computed from SERVER facing and SERVER modifiers;
        // the client cannot choose a velocity, distance or stamina cost.
        Vec3 look = player.getLookAngle();
        DodgeMotion motion = DodgeMotion.fromFacing(
                look.x, look.z, direction, modifiers.get(PlayerModifierId.DODGE_DISTANCE));
        if (motion == null) {
            return;
        }
        double vertical = Math.max(0.06D, player.getDeltaMovement().y);
        player.setDeltaMovement(motion.x(), vertical, motion.z());
        // Send motion explicitly to the owner. Normal entity tracking does not
        // reliably transmit the player's own new velocity to their local client.
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.setAttached(CombatAttachments.PLAYER, next);
        player.sendOverlayMessage(Component.literal(String.format(
                Locale.ROOT, "Dodge  -%.0f stamina", cost)));
    }

    private static CombatState fresh(
            ServerPlayer player, PlayerModifierSnapshot modifiers, long now) {
        return CombatRules.regenerate(
                player.getAttachedOrCreate(CombatAttachments.PLAYER), now,
                (float) modifiers.get(PlayerModifierId.MAX_STAMINA),
                (float) modifiers.get(PlayerModifierId.STAMINA_REGENERATION));
    }

    private static long regenerationDelay(PlayerModifierSnapshot modifiers) {
        return Math.max(1L, Math.round(
                20.0D * modifiers.get(PlayerModifierId.STAMINA_REGEN_DELAY)));
    }

    private static PlayerModifierSnapshot modifiers(ServerPlayer player) {
        return PlayerModifierCalculator.calculate(player.getAttachedOrCreate(CharacterAttachments.CHARACTER));
    }
}
