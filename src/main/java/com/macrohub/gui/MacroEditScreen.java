package com.macrohub.gui;

import com.macrohub.config.MacroConfig;
import com.macrohub.macro.MacroAction;
import com.macrohub.macro.MacroDefinition;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-macro editor screen.
 *
 * Replicates AHK's EditGui popup:
 *   - Macro name field
 *   - Key sequence editor (one token per line — pipe separated internally)
 *   - Delay field
 *   - Save / Cancel buttons
 *
 * Action tokens follow AHK notation:
 *   {LButton down}, {LButton up}, {RButton}, {XButton1}, {XButton2},
 *   {Shift down}, {Shift up}, {q}, {3}, Sleep <ms>
 */
public class MacroEditScreen extends Screen {

    // Colours (same palette as main screen)
    private static final int COL_BG        = 0xFF0A0A0A;
    private static final int COL_PANEL     = 0xFF1A1A1A;
    private static final int COL_GREEN     = 0xFF00FF88;
    private static final int COL_GREEN_DIM = 0xFF009955;
    private static final int COL_TEXT      = 0xFF00FF88;
    private static final int COL_GREY      = 0xFF888888;
    private static final int COL_RED       = 0xFFFF4444;

    private static final int WIN_W = 420;
    private static final int WIN_H = 320;
    private static final int PAD   = 14;

    private final Screen parent;
    private final MacroConfig config;
    private final int macroIndex;

    private TextFieldWidget nameField;
    private TextFieldWidget delayField;

    // Action row widgets
    private final List<TextFieldWidget> actionFields = new ArrayList<>();
    private int winX, winY;

    // Quick-add buttons
    private static final String[] QUICK_TOKENS = {
        "{LButton}", "{LButton down}", "{LButton up}",
        "{RButton}", "{XButton1}", "{XButton2}",
        "{Shift down}", "{Shift up}", "{q}", "{3}", "Sleep 50"
    };

    public MacroEditScreen(Screen parent, MacroConfig config, int macroIndex) {
        super(Text.literal("Edit Macro"));
        this.parent      = parent;
        this.config      = config;
        this.macroIndex  = macroIndex;
    }

    @Override
    protected void init() {
        winX = (width  - WIN_W) / 2;
        winY = (height - WIN_H) / 2;

        MacroDefinition macro = config.getMacros().get(macroIndex);
        actionFields.clear();

        int y = winY + 50;

        // Name field
        nameField = new TextFieldWidget(textRenderer,
                winX + PAD + 60, y, WIN_W - PAD * 2 - 60, 20,
                Text.literal(macro.getName()));
        nameField.setText(macro.getName());
        nameField.setMaxLength(30);
        addDrawableChild(nameField);
        y += 30;

        // Delay field
        delayField = new TextFieldWidget(textRenderer,
                winX + PAD + 90, y, 60, 20,
                Text.literal(String.valueOf(macro.getDelay())));
        delayField.setText(String.valueOf(macro.getDelay()));
        delayField.setMaxLength(6);
        addDrawableChild(delayField);
        y += 30;

        // Action sequence — one text field per action
        // We display up to 4 action rows; user can edit token strings directly
        List<MacroAction> actions = macro.getActions();
        int rows = Math.max(4, actions.size() + 1); // always show at least 4 rows
        rows = Math.min(rows, 6); // cap at 6 visible

        for (int i = 0; i < rows; i++) {
            String token = (i < actions.size()) ? actions.get(i).toToken() : "";
            TextFieldWidget tf = new TextFieldWidget(textRenderer,
                    winX + PAD, y, WIN_W - PAD * 2, 20,
                    Text.literal(token));
            tf.setText(token);
            tf.setMaxLength(60);
            actionFields.add(tf);
            addDrawableChild(tf);
            y += 26;
        }

        y += 8;

        // Quick-add row (two rows of buttons)
        int bx = winX + PAD;
        int by = y;
        for (int i = 0; i < QUICK_TOKENS.length; i++) {
            if (i == 6) { bx = winX + PAD; by += 24; }
            String tok = QUICK_TOKENS[i];
            String label = tok.replace("{", "").replace("}", "");
            if (label.length() > 8) label = label.substring(0, 8);
            addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> addToken(tok))
                    .dimensions(bx, by, 60, 20).build());
            bx += 62;
        }

        y = winY + WIN_H - 40;

        // Save
        addDrawableChild(ButtonWidget.builder(Text.literal("✔ Save"), btn -> save())
                .dimensions(winX + PAD, y, 100, 24).build());

        // Clear all
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear All"), btn -> {
            for (TextFieldWidget tf : actionFields) tf.setText("");
        }).dimensions(winX + PAD + 106, y, 90, 24).build());

        // Cancel
        addDrawableChild(ButtonWidget.builder(Text.literal("✕ Cancel"), btn -> close())
                .dimensions(winX + WIN_W - PAD - 90, y, 90, 24).build());
    }

    private void addToken(String token) {
        // Find first empty field and fill it
        for (TextFieldWidget tf : actionFields) {
            if (tf.getText().isBlank()) {
                tf.setText(token);
                return;
            }
        }
        // All full — put in last field
        if (!actionFields.isEmpty())
            actionFields.get(actionFields.size() - 1).setText(token);
    }

    private void save() {
        MacroDefinition macro = config.getMacros().get(macroIndex);

        // Name
        if (!nameField.getText().isBlank())
            macro.setName(nameField.getText().trim());

        // Delay
        try {
            macro.setDelay(Integer.parseInt(delayField.getText().trim()));
        } catch (NumberFormatException ignored) {}

        // Actions
        List<MacroAction> newActions = new ArrayList<>();
        for (TextFieldWidget tf : actionFields) {
            String text = tf.getText().trim();
            if (!text.isBlank()) newActions.add(MacroAction.fromToken(text));
        }
        macro.setActions(newActions);

        config.save();
        close();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);

        // Background
        ctx.fill(winX, winY, winX + WIN_W, winY + WIN_H, COL_BG);

        // Border
        drawBorder(ctx, winX, winY, WIN_W, WIN_H, 2, COL_GREEN);

        // Title bar
        ctx.fill(winX + 2, winY + 2, winX + WIN_W - 2, winY + 32, COL_PANEL);
        ctx.drawCenteredTextWithShadow(textRenderer,
                "Edit: " + config.getMacros().get(macroIndex).getName(),
                winX + WIN_W / 2, winY + 11, COL_GREEN);
        ctx.fill(winX + 2, winY + 32, winX + WIN_W - 2, winY + 33, COL_GREEN);

        // Labels
        int y = winY + 55;
        ctx.drawTextWithShadow(textRenderer, "Name:",   winX + PAD, y, COL_TEXT);
        y += 30;
        ctx.drawTextWithShadow(textRenderer, "Delay (ms):", winX + PAD, y, COL_TEXT);
        y += 30;
        ctx.drawTextWithShadow(textRenderer, "Key Sequence:", winX + PAD, y - 8, COL_GREEN_DIM);
        ctx.fill(winX + 2, y - 10, winX + WIN_W - 2, y - 9, COL_GREEN);

        // Action row numbers
        for (int i = 0; i < actionFields.size(); i++) {
            ctx.drawTextWithShadow(textRenderer, (i + 1) + ".",
                    winX + 2, winY + 110 + i * 26, COL_GREY);
        }

        // Quick-add label
        ctx.drawTextWithShadow(textRenderer, "Quick Add:",
                winX + PAD, winY + WIN_H - 86, COL_GREEN_DIM);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() { client.setScreen(parent); }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int t, int color) {
        ctx.fill(x,         y,         x + w,     y + t,     color);
        ctx.fill(x,         y + h - t, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + t,     y + h,     color);
        ctx.fill(x + w - t, y,         x + w,     y + h,     color);
    }
}
