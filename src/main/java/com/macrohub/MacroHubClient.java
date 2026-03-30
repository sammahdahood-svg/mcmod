package com.macrohub;

import com.macrohub.config.MacroConfig;
import com.macrohub.gui.MacroHubScreen;
import com.macrohub.keybind.KeybindManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * MacroHub — Fabric client entrypoint.
 *
 * Initialises:
 *  1. Config (load from disk or create defaults)
 *  2. KeybindManager (macro hotkey polling)
 *  3. GUI open keybind (M key by default — matches AHK tray "Show" action)
 */
public class MacroHubClient implements ClientModInitializer {

    public static MacroConfig CONFIG;

    // Keybind to open the MacroHub GUI (mirrors AHK tray "Show")
    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        System.out.println("[MacroHub] Initialising MacroHub v1.0 for Minecraft 1.21.1");

        // Load config (or create defaults matching the AHK script)
        CONFIG = MacroConfig.load();

        // Register macro hotkey polling
        KeybindManager.init(CONFIG);

        // Register "Open GUI" keybind — default: M
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.macrohub.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "MacroHub"
        ));

        // Poll open-GUI keybind each tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MacroHubScreen(null, CONFIG));
                }
            }
        });

        System.out.println("[MacroHub] Ready! Press M to open the macro manager.");
    }

    /** Reset all macros to the default AHK-matching configuration. */
    public static void resetToDefaults() {
        CONFIG = MacroConfig.createDefault();
        CONFIG.save();
        KeybindManager.updateConfig(CONFIG);
        System.out.println("[MacroHub] Reset to defaults.");
    }
}
