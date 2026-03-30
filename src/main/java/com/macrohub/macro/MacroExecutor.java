package com.macrohub.macro;

import com.macrohub.config.MacroConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes macros asynchronously on a dedicated thread.
 *
 * Replicates the AHK ExecuteMacro() function:
 *   - Iterates key list
 *   - Sends keys / mouse inputs
 *   - Sleeps between actions (data.delay or explicit Sleep tokens)
 *
 * Mouse inputs are sent via GLFW callbacks and Minecraft's internal
 * mouse handler so they register as real in-game actions.
 */
public class MacroExecutor {

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "MacroHub-Executor");
                t.setDaemon(true);
                return t;
            });

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static Future<?> currentTask = null;

    /** Execute a macro — mirrors AHK's ExecuteMacro(name). */
    public static void execute(MacroDefinition macro) {
        if (!macro.isEnabled()) return;
        if (running.get()) return; // don't overlap

        currentTask = EXECUTOR.submit(() -> {
            running.set(true);
            try {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc == null || mc.world == null) return;

                for (MacroAction action : macro.getActions()) {
                    if (Thread.currentThread().isInterrupted()) break;

                    switch (action.getType()) {

                        case SLEEP -> {
                            Thread.sleep(action.getValue());
                            continue; // skip normal inter-action delay
                        }

                        // ── Mouse actions ──────────────────────────────
                        case MOUSE_LEFT -> {
                            simulateMouseButton(mc, GLFW.GLFW_MOUSE_BUTTON_LEFT, true);
                            Thread.sleep(20);
                            simulateMouseButton(mc, GLFW.GLFW_MOUSE_BUTTON_LEFT, false);
                        }

                        case MOUSE_LEFT_DOWN ->
                            simulateMouseButton(mc, GLFW.GLFW_MOUSE_BUTTON_LEFT, true);

                        case MOUSE_LEFT_UP ->
                            simulateMouseButton(mc, GLFW.GLFW_MOUSE_BUTTON_LEFT, false);

                        case MOUSE_RIGHT -> {
                            simulateMouseButton(mc, GLFW.GLFW_MOUSE_BUTTON_RIGHT, true);
                            Thread.sleep(20);
                            simulateMouseButton(mc, GLFW.GLFW_MOUSE_BUTTON_RIGHT, false);
                        }

                        // XButton1/2 — map to hotbar slot or sprint as in AHK context
                        case MOUSE_BUTTON4 -> simulateHotbarKey(mc, 0); // slot 1
                        case MOUSE_BUTTON5 -> simulateHotbarKey(mc, 1); // slot 2

                        // ── Keyboard actions ───────────────────────────
                        case SHIFT_DOWN  -> setShiftHeld(mc, true);
                        case SHIFT_UP    -> setShiftHeld(mc, false);

                        case KEY_PRESS -> {
                            int ch = action.getValue();
                            // Map character to a Minecraft option and activate it
                            pressCharKey(mc, (char) ch);
                        }

                        default -> {} // unknown action, skip
                    }

                    Thread.sleep(macro.getDelay());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                running.set(false);
                // Release any held keys on finish
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) releaseAll(mc);
            }
        });
    }

    /** Cancel a running macro immediately. */
    public static void cancel() {
        if (currentTask != null) currentTask.cancel(true);
        running.set(false);
    }

    public static boolean isRunning() { return running.get(); }

    // ── Private helpers ──────────────────────────────────────────────────

    private static void simulateMouseButton(MinecraftClient mc, int button, boolean down) {
        long window = mc.getWindow().getHandle();
        int action = down ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE;
        mc.execute(() ->
            GLFW.glfwSetMouseButtonCallback(window, null)); // passthrough
        mc.getMouse().onMouseButton(window, button, action, 0);
    }

    /**
     * Simulate pressing a hotbar digit key (1-9).
     * In the AHK script, key "3" selects hotbar slot 3.
     */
    private static void pressCharKey(MinecraftClient mc, char ch) {
        GameOptions opts = mc.options;
        if (ch >= '1' && ch <= '9') {
            int slot = ch - '1';
            mc.execute(() -> {
                if (mc.player != null)
                    mc.player.getInventory().selectedSlot = slot;
            });
        } else if (ch == 'q' || ch == 'Q') {
            // 'q' = drop item (AttribSwap1 in AHK uses {q})
            mc.execute(() -> {
                if (mc.player != null && mc.gameRenderer != null)
                    mc.player.dropSelectedItem(false);
            });
        }
        // Additional keys can be mapped here
    }

    private static void simulateHotbarKey(MinecraftClient mc, int slot) {
        mc.execute(() -> {
            if (mc.player != null)
                mc.player.getInventory().selectedSlot = slot;
        });
    }

    private static boolean shiftHeld = false;

    private static void setShiftHeld(MinecraftClient mc, boolean held) {
        shiftHeld = held;
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.setSneaking(held);
            }
        });
    }

    private static void releaseAll(MinecraftClient mc) {
        if (shiftHeld) setShiftHeld(mc, false);
        mc.getMouse().onMouseButton(
                mc.getWindow().getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                GLFW.GLFW_RELEASE, 0);
    }
}
