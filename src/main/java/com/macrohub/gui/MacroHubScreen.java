package com.macrohub.gui;

import com.macrohub.MacroHubClient;
import com.macrohub.config.MacroConfig;
import com.macrohub.keybind.KeybindManager;
import com.macrohub.macro.MacroDefinition;
import com.macrohub.macro.MacroExecutor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Main MacroHub GUI screen.
 *
 * Replicates the AHK MainGui appearance:
 *   - Dark background (0x0a0a0a)
 *   - Green (#00ff88) borders, dividers, accent text
 *   - Title bar: "⚔ MINECRAFT MACRO MANAGER ⚔"
 *   - One row per macro with: name | hotkey field | Edit | Test buttons
 *   - Settings section: Only In Game toggle, Global Delay
 *   - Bottom buttons: Save Config | Reload | Apply Binds | Close
 *   - Status bar
 *
 * Open with: M key (configurable in Minecraft controls)
 */
public class MacroHubScreen extends Screen {

    private static final Identifier BACKGROUND_TEXTURE =
            Identifier.of("macrohub", "textures/gui/background.png");

    // ── Colours ──────────────────────────────────────────────────────────
    private static final int COL_BG        = 0xFF0A0A0A;
    private static final int COL_PANEL     = 0xFF1A1A1A;
    private static final int COL_GREEN     = 0xFF00FF88;
    private static final int COL_GREEN_DIM = 0xFF009955;
    private static final int COL_TEXT      = 0xFF00FF88;
    private static final int COL_WHITE     = 0xFFFFFFFF;
    private static final int COL_GREY      = 0xFF888888;
    private static final int COL_RED       = 0xFFFF4444;
    private static final int COL_BTN_BG    = 0xFF1A1A1A;
    private static final int COL_BTN_HOV   = 0xFF002211;
    private static final int COL_BTN_BRD   = 0xFF00FF88;

    // ── Layout ────────────────────────────────────────────────────────────
    private static final int WIN_W   = 500;
    private static final int ROW_H   = 30;
    private static final int PAD     = 12;

    private final MacroConfig config;
    private final Screen parent;

    // Hotkey text fields — one per macro row
    private final List<TextFieldWidget> hotkeyFields = new ArrayList<>();
    // Delay text field
    private TextFieldWidget globalDelayField;

    private String statusMsg = "Status: Ready";
    private int statusColor  = COL_GREEN;

    // Only-in-game toggle state (mirrors AHK OnlyInMC checkbox)
    private boolean onlyInGame;

    // Capture mode: waiting for a key press to rebind
    private int capturingIndex = -1; // index into config.getMacros()

    private int winX, winY, winH;

    public MacroHubScreen(Screen parent, MacroConfig config) {
        super(Text.literal("⚔ MINECRAFT MACRO MANAGER ⚔"));
        this.parent   = parent;
        this.config   = config;
        this.onlyInGame = config.isOnlyInGame();
    }

    @Override
    protected void init() {
        hotkeyFields.clear();

        List<MacroDefinition> macros = config.getMacros();

        // Calculate window height dynamically
        int macroSectionH = macros.size() * (ROW_H + 6);
        winH = 50 + 20 + macroSectionH + 20 + 80 + 55 + 40; // title+divs+macros+settings+buttons+status
        winX = (width  - WIN_W) / 2;
        winY = (height - winH)  / 2;

        int y = winY + 50; // below title bar

        // ── Macro rows ────────────────────────────────────────────────
        for (int i = 0; i < macros.size(); i++) {
            MacroDefinition m = macros.get(i);
            final int idx     = i;

            // Hotkey text field
            TextFieldWidget tf = new TextFieldWidget(
                    textRenderer,
                    winX + 175, y + 4,
                    72, 22,
                    Text.literal(KeybindManager.keyName(m.getHotkey())));
            tf.setText(KeybindManager.keyName(m.getHotkey()));
            tf.setMaxLength(10);
            tf.setEditable(true);
            hotkeyFields.add(tf);
            addDrawableChild(tf);

            // Edit button
            int editX = winX + 255;
            addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), btn -> openEditScreen(idx))
                    .dimensions(editX, y + 3, 55, 22).build());

            // Test button
            addDrawableChild(ButtonWidget.builder(Text.literal("Test"), btn -> {
                MacroExecutor.execute(config.getMacros().get(idx));
                setStatus("Testing: " + config.getMacros().get(idx).getName(), COL_GREEN);
            }).dimensions(editX + 60, y + 3, 55, 22).build());

            // Enable/Disable toggle
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(m.isEnabled() ? "ON" : "OFF"),
                    btn -> {
                        m.setEnabled(!m.isEnabled());
                        btn.setMessage(Text.literal(m.isEnabled() ? "ON" : "OFF"));
                        setStatus((m.isEnabled() ? "Enabled" : "Disabled") + ": " + m.getName(),
                                m.isEnabled() ? COL_GREEN : COL_RED);
                    })
                    .dimensions(editX + 120, y + 3, 40, 22).build());

            y += ROW_H + 6;
        }

        // ── Settings section ──────────────────────────────────────────
        y += 18; // gap

        // Only-in-game toggle button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Only In Game: " + (onlyInGame ? "ON" : "OFF")),
                btn -> {
                    onlyInGame = !onlyInGame;
                    config.setOnlyInGame(onlyInGame);
                    btn.setMessage(Text.literal("Only In Game: " + (onlyInGame ? "ON" : "OFF")));
                })
                .dimensions(winX + PAD, y, 180, 22).build());

        // Global delay field
        globalDelayField = new TextFieldWidget(textRenderer,
                winX + 280, y + 2, 60, 18,
                Text.literal(String.valueOf(config.getGlobalDelay())));
        globalDelayField.setText(String.valueOf(config.getGlobalDelay()));
        globalDelayField.setMaxLength(5);
        addDrawableChild(globalDelayField);

        y += 36;

        // ── Bottom buttons ────────────────────────────────────────────
        int bY = winY + winH - 60;
        int bX = winX + PAD;

        // Save Config
        addDrawableChild(ButtonWidget.builder(Text.literal("Save Config"), btn -> {
            applyAndSave();
            setStatus("Config saved!", COL_GREEN);
        }).dimensions(bX, bY, 100, 24).build());

        // Apply Binds
        addDrawableChild(ButtonWidget.builder(Text.literal("Apply Binds"), btn -> {
            applyBinds();
            setStatus("Binds applied!", COL_GREEN);
        }).dimensions(bX + 106, bY, 100, 24).build());

        // Reload Defaults
        addDrawableChild(ButtonWidget.builder(Text.literal("Defaults"), btn -> {
            MacroHubClient.resetToDefaults();
            close();
        }).dimensions(bX + 212, bY, 90, 24).build());

        // Close
        addDrawableChild(ButtonWidget.builder(Text.literal("✕ Close"), btn -> close())
                .dimensions(winX + WIN_W - PAD - 90, bY, 90, 24).build());
    }

    // ── Rendering ────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Pigeon background texture stretched across full screen
        ctx.drawTexture(BACKGROUND_TEXTURE, 0, 0, 0, 0, width, height, width, height);
        // Dark overlay so the GUI is readable over the pigeon
        ctx.fill(0, 0, width, height, 0xBB000000);

        // Main panel background
        ctx.fill(winX, winY, winX + WIN_W, winY + winH, COL_BG);

        // Green border
        drawBorder(ctx, winX, winY, WIN_W, winH, 2, COL_GREEN);

        // Title bar
        ctx.fill(winX + 2, winY + 2, winX + WIN_W - 2, winY + 32, COL_PANEL);
        ctx.drawCenteredTextWithShadow(textRenderer,
                "⚔  MINECRAFT MACRO MANAGER  ⚔",
                winX + WIN_W / 2, winY + 11, COL_GREEN);

        // Divider under title
        ctx.fill(winX + 2, winY + 32, winX + WIN_W - 2, winY + 34, COL_GREEN);

        // Section header: MACRO BINDINGS
        ctx.drawTextWithShadow(textRenderer, "MACRO BINDINGS",
                winX + PAD, winY + 38, COL_GREEN_DIM);

        // Divider
        ctx.fill(winX + 2, winY + 47, winX + WIN_W - 2, winY + 48, COL_GREEN);

        // ── Macro rows ────────────────────────────────────────────────
        List<MacroDefinition> macros = config.getMacros();
        int y = winY + 52;

        for (int i = 0; i < macros.size(); i++) {
            MacroDefinition m = macros.get(i);

            // Row bg (subtle alternating)
            if (i % 2 == 0)
                ctx.fill(winX + 2, y, winX + WIN_W - 2, y + ROW_H + 4, 0xFF0F0F0F);

            // Name
            ctx.drawTextWithShadow(textRenderer, m.getName(),
                    winX + PAD, y + 9, COL_TEXT);

            // Hotkey label
            ctx.drawTextWithShadow(textRenderer, "►",
                    winX + 160, y + 9, COL_GREEN_DIM);

            // Sequence preview (right side, truncated)
            String seq = m.getSequenceSummary();
            if (seq.length() > 22) seq = seq.substring(0, 19) + "...";
            ctx.drawTextWithShadow(textRenderer, seq,
                    winX + 375, y + 9, COL_GREY);

            y += ROW_H + 6;
        }

        // ── Settings section header ───────────────────────────────────
        int settingsY = winY + 52 + macros.size() * (ROW_H + 6) + 4;
        ctx.fill(winX + 2, settingsY, winX + WIN_W - 2, settingsY + 1, COL_GREEN);
        settingsY += 4;
        ctx.drawTextWithShadow(textRenderer, "SETTINGS",
                winX + PAD, settingsY, COL_GREEN_DIM);
        settingsY += 14;
        ctx.fill(winX + 2, settingsY, winX + WIN_W - 2, settingsY + 1, COL_GREEN);
        settingsY += 6;

        // Global delay label
        ctx.drawTextWithShadow(textRenderer, "Global Delay (ms):",
                winX + 200, settingsY + 4, COL_TEXT);

        // ── Status bar ────────────────────────────────────────────────
        ctx.fill(winX + 2, winY + winH - 22, winX + WIN_W - 2, winY + winH - 2, COL_PANEL);
        ctx.drawTextWithShadow(textRenderer, statusMsg,
                winX + PAD, winY + winH - 16, statusColor);

        // Capture mode overlay
        if (capturingIndex >= 0) {
            String msg = "Press a key to bind to: " + macros.get(capturingIndex).getName();
            ctx.fill(winX + 60, winY + winH / 2 - 14,
                    winX + WIN_W - 60, winY + winH / 2 + 14, 0xFF002211);
            drawBorder(ctx, winX + 60, winY + winH / 2 - 14,
                    WIN_W - 120, 28, 1, COL_GREEN);
            ctx.drawCenteredTextWithShadow(textRenderer, msg,
                    winX + WIN_W / 2, winY + winH / 2 - 5, COL_GREEN);
        }

        super.render(ctx, mx, my, delta);
    }

    // ── Keyboard handling ─────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingIndex >= 0) {
            // Assign the pressed key to the macro
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                MacroDefinition m = config.getMacros().get(capturingIndex);
                m.setHotkey(keyCode);
                hotkeyFields.get(capturingIndex).setText(KeybindManager.keyName(keyCode));
                setStatus("Bound " + m.getName() + " to " + KeybindManager.keyName(keyCode), COL_GREEN);
            }
            capturingIndex = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private void applyBinds() {
        List<MacroDefinition> macros = config.getMacros();
        for (int i = 0; i < macros.size() && i < hotkeyFields.size(); i++) {
            String text = hotkeyFields.get(i).getText().trim();
            int key = KeybindManager.keyFromName(text);
            if (key != GLFW.GLFW_KEY_UNKNOWN) {
                macros.get(i).setHotkey(key);
            }
        }
        try {
            int gd = Integer.parseInt(globalDelayField.getText().trim());
            config.setGlobalDelay(gd);
        } catch (NumberFormatException ignored) {}

        config.setOnlyInGame(onlyInGame);
        KeybindManager.updateConfig(config);
    }

    private void applyAndSave() {
        applyBinds();
        config.save();
    }

    private void openEditScreen(int macroIndex) {
        client.setScreen(new MacroEditScreen(this, config, macroIndex));
    }

    private void setStatus(String msg, int color) {
        statusMsg   = msg;
        statusColor = color;
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int t, int color) {
        ctx.fill(x,         y,         x + w,     y + t,     color); // top
        ctx.fill(x,         y + h - t, x + w,     y + h,     color); // bottom
        ctx.fill(x,         y,         x + t,     y + h,     color); // left
        ctx.fill(x + w - t, y,         x + w,     y + h,     color); // right
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        applyAndSave();
        client.setScreen(parent);
    }
}
