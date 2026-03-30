package com.macrohub.config;

import com.google.gson.*;
import com.macrohub.macro.MacroAction;
import com.macrohub.macro.MacroDefinition;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles serialisation/deserialisation of macro definitions.
 *
 * Config is stored at: .minecraft/config/macrohub.json
 *
 * Default macros mirror the AHK Config section exactly:
 *   StunSlam  (F1) — {XButton1} LMB LMB Sleep80 {3} LMB LMB
 *   Anchor    (F2) — RMB Shift+LMB
 *   AttribSwap1 (F3) — {q} Sleep60 {XButton2}
 *   AttribSwap2 (F4) — LMB Sleep3 {3}
 *   CustomMacro1 (F5) — empty
 *   CustomMacro2 (F6) — empty
 */
public class MacroConfig {

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("macrohub.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private List<MacroDefinition> macros = new ArrayList<>();
    private int globalDelay = 50;
    private boolean onlyInGame = true;

    // ── Defaults (matching AHK script) ──────────────────────────────────

    public static MacroConfig createDefault() {
        MacroConfig cfg = new MacroConfig();
        cfg.macros = new ArrayList<>();

        // StunSlam — F1 — delay 40
        cfg.macros.add(new MacroDefinition("StunSlam", GLFW.GLFW_KEY_F1, 40,
                MacroDefinition.actionsFromString(
                        "{XButton1}|{LButton down}|{LButton up}|Sleep 80|{3}|{LButton down}|{LButton up}")));

        // Anchor — F2 — delay 30
        cfg.macros.add(new MacroDefinition("Anchor", GLFW.GLFW_KEY_F2, 30,
                MacroDefinition.actionsFromString(
                        "{RButton}|{Shift down}|{LButton}|{Shift up}")));

        // AttribSwap1 — F3 — delay 40
        cfg.macros.add(new MacroDefinition("AttribSwap1", GLFW.GLFW_KEY_F3, 40,
                MacroDefinition.actionsFromString(
                        "{q}|Sleep 60|{XButton2}")));

        // AttribSwap2 — F4 — delay 1
        cfg.macros.add(new MacroDefinition("AttribSwap2", GLFW.GLFW_KEY_F4, 1,
                MacroDefinition.actionsFromString(
                        "{LButton down}|{LButton up}|Sleep 3|{3}")));

        // CustomMacro1 — F5 — empty
        cfg.macros.add(new MacroDefinition("CustomMacro1", GLFW.GLFW_KEY_F5, 50,
                new ArrayList<>()));

        // CustomMacro2 — F6 — empty
        cfg.macros.add(new MacroDefinition("CustomMacro2", GLFW.GLFW_KEY_F6, 50,
                new ArrayList<>()));

        return cfg;
    }

    // ── Persistence ──────────────────────────────────────────────────────

    public void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("globalDelay", globalDelay);
            root.addProperty("onlyInGame", onlyInGame);

            JsonArray arr = new JsonArray();
            for (MacroDefinition m : macros) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name",    m.getName());
                obj.addProperty("hotkey",  m.getHotkey());
                obj.addProperty("delay",   m.getDelay());
                obj.addProperty("actions", m.actionsToString());
                obj.addProperty("enabled", m.isEnabled());
                arr.add(obj);
            }
            root.add("macros", arr);

            try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            System.err.println("[MacroHub] Failed to save config: " + e.getMessage());
        }
    }

    public static MacroConfig load() {
        if (!CONFIG_PATH.toFile().exists()) return createDefault();

        try (Reader r = new FileReader(CONFIG_PATH.toFile())) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            MacroConfig cfg = new MacroConfig();

            if (root.has("globalDelay")) cfg.globalDelay = root.get("globalDelay").getAsInt();
            if (root.has("onlyInGame"))  cfg.onlyInGame  = root.get("onlyInGame").getAsBoolean();

            JsonArray arr = root.getAsJsonArray("macros");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String name    = obj.get("name").getAsString();
                int hotkey     = obj.get("hotkey").getAsInt();
                int delay      = obj.get("delay").getAsInt();
                String actions = obj.has("actions") ? obj.get("actions").getAsString() : "";
                boolean enabled= !obj.has("enabled") || obj.get("enabled").getAsBoolean();

                MacroDefinition md = new MacroDefinition(name, hotkey, delay,
                        MacroDefinition.actionsFromString(actions));
                md.setEnabled(enabled);
                cfg.macros.add(md);
            }
            return cfg;
        } catch (Exception e) {
            System.err.println("[MacroHub] Failed to load config, using defaults: " + e.getMessage());
            return createDefault();
        }
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public List<MacroDefinition> getMacros()                { return macros; }
    public int  getGlobalDelay()                            { return globalDelay; }
    public void setGlobalDelay(int d)                       { this.globalDelay = d; }
    public boolean isOnlyInGame()                           { return onlyInGame; }
    public void setOnlyInGame(boolean b)                    { this.onlyInGame = b; }

    public MacroDefinition getByName(String name) {
        return macros.stream().filter(m -> m.getName().equals(name)).findFirst().orElse(null);
    }
}
