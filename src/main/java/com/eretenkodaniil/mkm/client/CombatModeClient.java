package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.combat.mode.CombatModeInputRules;
import com.eretenkodaniil.mkm.combat.net.CombatModeRequest;
import com.eretenkodaniil.mkm.combat.net.CombatModeStatus;
import com.eretenkodaniil.mkm.combat.net.CombatModeContact;
import com.eretenkodaniil.mkm.combat.net.CombatStrikeIntent;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Client INPUT ONLY. No local damage, client attack() or client hit selection.
 * Normal vanilla LMB, block mining and clicks are not modified until the
 * server ACK explicitly enables experimental combat mode.
 */
public final class CombatModeClient {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(MkmMod.id("combat_mode"));
    private static final KeyMapping TOGGLE = new KeyMapping(
            "key.mkm.combat_mode", InputConstants.KEY_F6, CATEGORY);
    private static boolean enabled;
    private static boolean holding;
    private static boolean queued;
    private static long queuedTick;
    private static int serial;
    private static long pressedTick;
    private static Object level;
    private static CombatModeContact lastContact;
    private static long lastContactLocalTick;

    private CombatModeClient() {}

    public static void initialize() {
        KeyMappingHelper.registerKeyMapping(TOGGLE);
        ClientPlayNetworking.registerGlobalReceiver(CombatModeStatus.TYPE,
                (status, context) -> context.client().execute(() -> onStatus(context.client(), status)));
        ClientPlayNetworking.registerGlobalReceiver(CombatModeContact.TYPE,
                (receipt, context) -> context.client().execute(() -> onContact(context.client(), receipt)));
    }

    public static void onStatus(Minecraft client, CombatModeStatus status) {
        enabled = status.enabled();
        if (!enabled) {
            holding = false;
            queued = false;
            lastContact = null;
        }
        // The server sends the authoritative action-bar feedback; this ACK
        // only controls whether the client may redirect entity-only LMB.
    }

    /** Read-only MNAE/UI consumer of the server event; never an attack source. */
    public static void onContact(Minecraft client, CombatModeContact contact) {
        if (!enabled || client.level == null) return;
        lastContact = contact;
        lastContactLocalTick = client.level.getGameTime();
        queued = false;
    }

    /** The vanilla continuing-mining loop must not run during an entity-only hold. */
    public static boolean isHoldingEntityStrike() {
        return enabled && holding;
    }

    /** Called by Minecraft.startAttack at HEAD; true ONLY when vanilla LMB is suppressed. */
    public static boolean interceptAttack(Minecraft client) {
        if (!enabled || client.player == null || client.level == null) return false;
        // A queued sword strike can be canceled by switching to block mining,
        // which must remain vanilla. Do not emit another entity hit in between.
        if (queued) {
            if (client.hitResult instanceof net.minecraft.world.phys.BlockHitResult) {
                if (canSend()) ClientPlayNetworking.send(
                        new CombatStrikeIntent((byte) 2, serial));
                queued = false;
                return false;
            }
            return true;
        }
        String id = BuiltInRegistries.ITEM.getKey(
                client.player.getMainHandItem().getItem()).toString();
        boolean sword = EquipmentRules.profile(id, EquipmentRules.Slot.MAIN_HAND).family()
                == EquipmentRules.Family.SWORD;
        if (!CombatModeInputRules.intercept(enabled, holding, client.gui.screen() != null,
                client.player.isAlive() && !client.player.isSpectator()
                        && !client.player.isUsingItem(),
                sword, client.hitResult instanceof EntityHitResult hit
                        && hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity,
                canSend())) return false;
        if (holding) return true;
        if (serial == Integer.MAX_VALUE) {
            // Return to vanilla if connection has exhausted monotonically increasing serials.
            enabled = false;
            return false;
        }
        holding = true;
        pressedTick = client.level.getGameTime();
        serial++;
        ClientPlayNetworking.send(new CombatStrikeIntent((byte) 0, serial));
        return true;
    }

    private static boolean canSend() {
        return ClientPlayNetworking.canSend(CombatStrikeIntent.TYPE);
    }

    public static void onTick(Minecraft client) {
        if (client.level != level) {
            level = client.level;
            enabled = false;
            holding = false;
            queued = false;
            serial = 0;
            lastContact = null;
        }
        if (client.player == null || client.level == null || !client.player.isAlive()) {
            enabled = false;
            holding = false;
            queued = false;
            lastContact = null;
            return;
        }
        while (TOGGLE.consumeClick()) {
            if (client.gui.screen() == null && ClientPlayNetworking.canSend(CombatModeRequest.TYPE)) {
                if (holding && canSend()) {
                    ClientPlayNetworking.send(new CombatStrikeIntent((byte) 2, serial));
                    holding = false;
                }
                queued = false;
                // Do not enable locally until server ACK confirms QA flag.
                ClientPlayNetworking.send(new CombatModeRequest(!enabled));
            }
        }
        if (queued) {
            boolean invalid = !enabled || client.gui.screen() != null
                    || client.player.isSpectator()
                    || EquipmentRules.profile(BuiltInRegistries.ITEM.getKey(
                            client.player.getMainHandItem().getItem()).toString(),
                            EquipmentRules.Slot.MAIN_HAND).family() != EquipmentRules.Family.SWORD
                    || client.level.getGameTime() - queuedTick > 40L;
            if (invalid) {
                if (canSend()) ClientPlayNetworking.send(
                        new CombatStrikeIntent((byte) 2, serial));
                queued = false;
            }
        }
        if (holding) {
            boolean cancel = !enabled || client.gui.screen() != null
                    || client.player.isSpectator() || client.player.isUsingItem()
                    || EquipmentRules.profile(BuiltInRegistries.ITEM.getKey(
                            client.player.getMainHandItem().getItem()).toString(),
                            EquipmentRules.Slot.MAIN_HAND).family() != EquipmentRules.Family.SWORD
                    || client.level.getGameTime() - pressedTick > 35L;
            boolean release = !client.options.keyAttack.isDown();
            if (cancel || release) {
                if (canSend()) ClientPlayNetworking.send(
                        new CombatStrikeIntent((byte) (cancel ? 2 : 1), serial));
                holding = false;
                if (!cancel) {
                    queued = true;
                    queuedTick = client.level.getGameTime();
                }
            }
        }
    }

    public static String hudText(Minecraft client) {
        if (!enabled || client.level == null) return "";
        if (!holding) {
            if (queued) return "F6 COMBAT | SERVER WINDUP (no hit predicted)";
            if (lastContact != null && client.level.getGameTime() >= lastContactLocalTick
                    && client.level.getGameTime() - lastContactLocalTick <= 40L) {
                return lastContact.confirmedHealthHit()
                        ? "SERVER HIT " + (lastContact.kind() == 2 ? "CHARGED " : "QUICK ")
                            + Math.round(lastContact.actualHealthDamage())
                        : "SERVER CONTACT | BLOCKED / MISSED";
            }
            return "F6 COMBAT | sword target → hold LMB";
        }
        long ticks = Math.max(0L, client.level.getGameTime() - pressedTick);
        return "F6 COMBAT | " + (ticks >= 12 ? "CHARGED (predicted)" :
                "QUICK (predicted) " + ticks + "/12t") + " | release to commit";
    }
}
