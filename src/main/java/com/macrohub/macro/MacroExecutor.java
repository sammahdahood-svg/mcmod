package com.macrohub.macro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class MacroExecutor {

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "MacroHub-Executor");
                t.setDaemon(true);
                return t;
            });

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static Future<?> currentTask = null;

    public static void execute(MacroDefinition macro) {
        if (!macro.isEnabled()) return;
        if (running.get()) return;

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
                            continue;
                        }

                        case MOUSE_LEFT, MOUSE_LEFT_DOWN, MOUSE_LEFT_UP -> {
                            mc.execute(() -> {
                                if (mc.crosshairTarget != null && mc.player != null) {
                                    mc.interactionManager.attackBlock(
                                        mc.player.getBlockPos(), net.minecraft.util.math.Direction.UP);
                                }
                            });
                        }

                        case MOUSE_RIGHT -> {
                            mc.execute(() -> {
                                if (mc.player != null && mc.interactionManager != null) {
                                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                                }
                            });
                        }

                        case MOUSE_BUTTON4 -> {
                            mc.execute(() -> {
                                if (mc.player != null)
                                    mc.player.getInventory().selectedSlot = 0;
                            });
                        }

                        case MOUSE_BUTTON5 -> {
                            mc.execute(() -> {
                                if (mc.player != null)
                                    mc.player.getInventory().selectedSlot = 1;
                            });
                        }

                        case SHIFT_DOWN -> {
                            mc.execute(() -> {
                                if (mc.player != null) mc.player.setSneaking(true);
                            });
                        }

                        case SHIFT_UP -> {
                            mc.execute(() -> {
                                if (mc.player != null) mc.player.setSneaking(false);
                            });
                        }

                        case KEY_PRESS -> {
                            int ch = action.getValue();
                            mc.execute(() -> {
                                if (mc.player == null) return;
                                if (ch >= '1' && ch <= '9') {
                                    mc.player.getInventory().selectedSlot = ch - '1';
                                } else if (ch == 'q' || ch == 'Q') {
                                    mc.player.dropSelectedItem(false);
                                }
                            });
                        }

                        default -> {}
                    }

                    Thread.sleep(macro.getDelay());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                running.set(false);
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.execute(() -> {
                        if (mc.player != null) mc.player.setSneaking(false);
                    });
                }
            }
        });
    }

    public static void cancel() {
        if (currentTask != null) currentTask.cancel(true);
        running.set(false);
    }

    public static boolean isRunning() { return running.get(); }
}
