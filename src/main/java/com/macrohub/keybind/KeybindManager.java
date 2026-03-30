package com.macrohub.keybind;

import com.macrohub.config.MacroConfig;
import com.macrohub.macro.MacroDefinition;
import com.macrohub.macro.MacroExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Manages hotkey polling and dispatching for all macros.
 *
 * Replaces AHK's Hotkey() registration system.
 * We poll key state each client tick and fire macros on key-down edge.
 *
 * Also handles the "Only active in Minecraft" logic
 * (AHK: WinActive("ahk_exe javaw.exe") check) — since we're inside
 * Minecraft this is always true, but we honour the onlyInGame flag
 * to allow disabling macros while a GUI is open.
 */
public class KeybindManager {

    private static MacroConfig config;
    private static final Set<Integer> heldKeys = new HashSet<>();
    private static boolean registered = false;

    public static void init(MacroConfig cfg) {
        config = cfg;

        if (!registered) {
            registered = true;
            ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
        }
    }

    public static void updateConfig(MacroConfig cfg) {
        config = cfg;
    }

    private static void tick(MinecraftClient client) {
        if (config == null) return;
        if (client.currentScreen != null) return; // don't fire while GUI is open

        long window = client.getWindow().getHandle();

        for (MacroDefinition macro : config.getMacros()) {
            if (!macro.isEnabled()) continue;

            int key = macro.getHotkey();
            if (key == GLFW.GLFW_KEY_UNKNOWN) continue;

            int state = GLFW.glfwGetKey(window, key);
            boolean pressed = (state == GLFW.GLFW_PRESS);

            if (pressed && !heldKeys.contains(key)) {
                // Rising edge — fire macro
                heldKeys.add(key);
                MacroExecutor.execute(macro);
            } else if (!pressed) {
                heldKeys.remove(key);
            }
        }
    }

    /** Map a GLFW key integer to a human-readable name. */
    public static String keyName(int glfwKey) {
        return switch (glfwKey) {
            case GLFW.GLFW_KEY_F1  -> "F1";
            case GLFW.GLFW_KEY_F2  -> "F2";
            case GLFW.GLFW_KEY_F3  -> "F3";
            case GLFW.GLFW_KEY_F4  -> "F4";
            case GLFW.GLFW_KEY_F5  -> "F5";
            case GLFW.GLFW_KEY_F6  -> "F6";
            case GLFW.GLFW_KEY_F7  -> "F7";
            case GLFW.GLFW_KEY_F8  -> "F8";
            case GLFW.GLFW_KEY_F9  -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            case GLFW.GLFW_KEY_R   -> "R";
            case GLFW.GLFW_KEY_G   -> "G";
            case GLFW.GLFW_KEY_H   -> "H";
            case GLFW.GLFW_KEY_J   -> "J";
            case GLFW.GLFW_KEY_K   -> "K";
            case GLFW.GLFW_KEY_Z   -> "Z";
            case GLFW.GLFW_KEY_X   -> "X";
            case GLFW.GLFW_KEY_C   -> "C";
            case GLFW.GLFW_KEY_V   -> "V";
            case GLFW.GLFW_KEY_B   -> "B";
            case GLFW.GLFW_KEY_N   -> "N";
            case GLFW.GLFW_KEY_M   -> "M";
            default -> "Key(" + glfwKey + ")";
        };
    }

    /** Return the GLFW key code for a display name string. */
    public static int keyFromName(String name) {
        return switch (name.toUpperCase()) {
            case "F1"  -> GLFW.GLFW_KEY_F1;
            case "F2"  -> GLFW.GLFW_KEY_F2;
            case "F3"  -> GLFW.GLFW_KEY_F3;
            case "F4"  -> GLFW.GLFW_KEY_F4;
            case "F5"  -> GLFW.GLFW_KEY_F5;
            case "F6"  -> GLFW.GLFW_KEY_F6;
            case "F7"  -> GLFW.GLFW_KEY_F7;
            case "F8"  -> GLFW.GLFW_KEY_F8;
            case "F9"  -> GLFW.GLFW_KEY_F9;
            case "F10" -> GLFW.GLFW_KEY_F10;
            case "F11" -> GLFW.GLFW_KEY_F11;
            case "F12" -> GLFW.GLFW_KEY_F12;
            case "R"   -> GLFW.GLFW_KEY_R;
            case "G"   -> GLFW.GLFW_KEY_G;
            case "H"   -> GLFW.GLFW_KEY_H;
            case "J"   -> GLFW.GLFW_KEY_J;
            case "K"   -> GLFW.GLFW_KEY_K;
            case "Z"   -> GLFW.GLFW_KEY_Z;
            case "X"   -> GLFW.GLFW_KEY_X;
            case "C"   -> GLFW.GLFW_KEY_C;
            case "V"   -> GLFW.GLFW_KEY_V;
            case "B"   -> GLFW.GLFW_KEY_B;
            default    -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }
}
