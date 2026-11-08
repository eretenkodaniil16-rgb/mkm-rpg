package com.eretenkodaniil.mkm.client.input;

import com.eretenkodaniil.mkm.client.accessibility.MkmAccessibility;
import java.nio.IntBuffer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLGamepad;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLStdinc;

/**
 * Opt-in SDL3 action-only controller adapter. Polls button state without
 * draining Minecraft's SDL event queue or synthesizing vanilla attack/mining.
 * No movement, cursor, screen navigation or claim of full controller support.
 */
public final class MkmSdlGamepadAdapter {
    public enum Status { DISABLED, SEARCHING, CONNECTED, CONFLICT, UNAVAILABLE }
    private static final GamepadButtonEdges EDGES = new GamepadButtonEdges();
    private static Status status = Status.DISABLED;
    private static boolean subsystemOwned;
    private static boolean unavailable;
    private static long handle;
    private static int scans;

    private MkmSdlGamepadAdapter() {}

    public static String statusTranslationKey() {
        return "screen.mkm.access.gamepad." + status.name().toLowerCase(java.util.Locale.ROOT);
    }

    public static void tick(Minecraft client) {
        if (!MkmAccessibility.get().gamepadEnabled()) {
            stop();
            unavailable = false;
            status = Status.DISABLED;
            return;
        }
        // Avoid duplicate input owners if a full controller mod is installed.
        if (FabricLoader.getInstance().isModLoaded("controlify")
                || FabricLoader.getInstance().isModLoaded("controlflex")) {
            stop();
            status = Status.CONFLICT;
            return;
        }
        if (unavailable) { status = Status.UNAVAILABLE; return; }
        if (client.player == null || client.level == null) {
            stop();
            status = Status.SEARCHING;
            return;
        }
        try {
            if (!subsystemOwned) {
                if (!SDLInit.SDL_InitSubSystem(SDLInit.SDL_INIT_GAMEPAD)) {
                    unavailable = true;
                    status = Status.UNAVAILABLE;
                    return;
                }
                subsystemOwned = true;
                scans = 0;
            }
            if (handle != 0L && !SDLGamepad.SDL_GamepadConnected(handle)) closeHandle();
            if (handle == 0L && (++scans % 40 == 1)) discover();
            if (handle == 0L) { status = Status.SEARCHING; return; }

            SDLGamepad.SDL_UpdateGamepads();
            int rising = EDGES.rising(buttonMask());
            status = Status.CONNECTED;
            // Never activate combat from a chat/character/settings or palette screen.
            // Ignore GUI-time edges so they cannot fire after a menu is closed.
            if (client.gui.screen() != null) return;
            if ((rising & GamepadButtonEdges.DODGE) != 0)
                MkmActionRouter.request(client, ClientActionGate.Action.DODGE, false);
            if ((rising & GamepadButtonEdges.COMBO) != 0)
                MkmActionRouter.request(client, ClientActionGate.Action.COMBO, false);
            if ((rising & GamepadButtonEdges.RENEWAL) != 0)
                MkmActionRouter.request(client, ClientActionGate.Action.RENEWAL, false);
        } catch (RuntimeException | LinkageError failure) {
            // Fail closed if native SDL is unavailable or another mod owns the device.
            try { stop(); } catch (RuntimeException | LinkageError ignored) { }
            unavailable = true;
            status = Status.UNAVAILABLE;
            System.err.println("MKM SDL3 gamepad disabled: " + failure.getMessage());
        }
    }

    private static void discover() {
        SDLGamepad.SDL_UpdateGamepads();
        if (!SDLGamepad.SDL_HasGamepad()) return;
        IntBuffer ids = SDLGamepad.SDL_GetGamepads();
        try {
            if (ids == null) return;
            for (int i = 0; i < ids.remaining(); i++) {
                int id = ids.get(i);
                if (!SDLGamepad.SDL_IsGamepad(id)) continue;
                long device = SDLGamepad.SDL_OpenGamepad(id);
                if (device != 0L) {
                    handle = device;
                    EDGES.reset(buttonMask()); // No immediate action from held buttons.
                    return;
                }
            }
        } finally {
            SDLStdinc.SDL_free(ids); // SDL_GetGamepads allocation is SDL-owned.
        }
    }

    private static int buttonMask() {
        int buttons = 0;
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_EAST))
            buttons |= GamepadButtonEdges.DODGE;
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_WEST))
            buttons |= GamepadButtonEdges.COMBO;
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH))
            buttons |= GamepadButtonEdges.RENEWAL;
        return buttons;
    }

    private static void closeHandle() {
        if (handle != 0L) SDLGamepad.SDL_CloseGamepad(handle);
        handle = 0L;
        EDGES.reset(0);
    }

    private static void stop() {
        if (!subsystemOwned && handle == 0L) return;
        closeHandle();
        if (subsystemOwned) SDLInit.SDL_QuitSubSystem(SDLInit.SDL_INIT_GAMEPAD);
        subsystemOwned = false;
        scans = 0;
    }
}
