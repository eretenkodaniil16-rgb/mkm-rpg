package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.combat.net.CombatModeRequest;
import com.eretenkodaniil.mkm.combat.net.CombatModeStatus;
import com.eretenkodaniil.mkm.combat.net.CombatStrikeIntent;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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
    private static int serial;
    private static long pressedTick;
    private static Object level;

    private CombatModeClient() {}

    public static void initialize() {
        KeyMappingHelper.registerKeyMapping(TOGGLE);
        ClientPlayNetworking.registerGlobalReceiver(CombatModeStatus.TYPE,
                (status, context) -> context.client().execute(() -> onStatus(context.client(), status)));
    }

    public static void onStatus(Minecraft client, CombatModeStatus status) {
        enabled = status.enabled();
        if (!enabled) holding = false;
        if (client.player != null) client.player.displayClientMessage(Component.literal(
                enabled ? "MKM | COMBAT MODE ENABLED (F6 to exit)"
                        : "MKM | VANILLA LMB / MINING"), true);
    }

    /** Called by Minecraft.startAttack at HEAD; true ONLY when vanilla LMB is suppressed. */
    public static boolean interceptAttack(Minecraft client) {
        if (!enabled || client.player == null || client.level == null) return false;
        if (holding) return true; // no accidental vanilla mining after starting held entity attack
        if (client.gui.screen() != null || client.player.isSpectator()
                || !client.player.isAlive() || client.player.isUsingItem()
                || !(client.hitResult instanceof EntityHitResult) || !canSend()) return false;
        String id = BuiltInRegistries.ITEM.getKey(
                client.player.getMainHandItem().getItem()).toString();
        if (EquipmentRules.profile(id, EquipmentRules.Slot.MAIN_HAND).family()
                != EquipmentRules.Family.SWORD) return false;
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
            serial = 0;
        }
        if (client.player == null || client.level == null || !client.player.isAlive()) {
            enabled = false;
            holding = false;
            return;
        }
        while (TOGGLE.consumeClick()) {
            if (client.gui.screen() == null && ClientPlayNetworking.canSend(CombatModeRequest.TYPE)) {
                if (holding && canSend()) {
                    ClientPlayNetworking.send(new CombatStrikeIntent((byte) 2, serial));
                    holding = false;
                }
                // Do not enable locally until server ACK confirms QA flag.
                ClientPlayNetworking.send(new CombatModeRequest(!enabled));
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
            }
        }
    }

    public static String hudText(Minecraft client) {
        if (!enabled || client.level == null) return "";
        if (!holding) return "F6 COMBAT | sword target → hold LMB";
        long ticks = Math.max(0L, client.level.getGameTime() - pressedTick);
        return "F6 COMBAT | " + (ticks >= 12 ? "CHARGED (predicted)" :
                "QUICK (predicted) " + ticks + "/12t") + " | release to commit";
    }
}
