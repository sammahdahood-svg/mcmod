package com.macrohub.macro;

/**
 * Represents a single action in a macro key sequence.
 * Ported from the AHK script's key sequence format:
 *   {LButton down}, {LButton up}, {RButton}, {XButton1}, {XButton2},
 *   {Shift down}, {Shift up}, {q}, {3}, Sleep 80, etc.
 */
public class MacroAction {

    public enum Type {
        KEY_PRESS,       // Press and release a key
        KEY_DOWN,        // Hold a key down
        KEY_UP,          // Release a held key
        MOUSE_LEFT,      // Left click (down+up)
        MOUSE_LEFT_DOWN, // Hold left mouse
        MOUSE_LEFT_UP,   // Release left mouse
        MOUSE_RIGHT,     // Right click
        MOUSE_BUTTON4,   // XButton1 (side button 1) — Sprint/Swap in many setups
        MOUSE_BUTTON5,   // XButton2 (side button 2)
        SLEEP,           // Wait N milliseconds
        SHIFT_DOWN,
        SHIFT_UP,
        SPRINT_TOGGLE,   // Mapped from XButton1 context
    }

    private final Type type;
    private final int value;     // keycode for KEY_*, ms for SLEEP
    private final String label;  // human-readable

    public MacroAction(Type type, int value, String label) {
        this.type = type;
        this.value = value;
        this.label = label;
    }

    public Type getType() { return type; }
    public int getValue() { return value; }
    public String getLabel() { return label; }

    /**
     * Parse an AHK-style token string into a MacroAction.
     * Supported tokens (matching the original script):
     *   {LButton down}, {LButton up}, {LButton}, {RButton},
     *   {XButton1}, {XButton2}, {Shift down}, {Shift up},
     *   Sleep <N>, {q}, {3}, and any single char key.
     */
    public static MacroAction fromToken(String token) {
        String t = token.trim();

        if (t.startsWith("Sleep")) {
            String num = t.replaceAll("\\D+", "");
            int ms = num.isEmpty() ? 50 : Integer.parseInt(num);
            return new MacroAction(Type.SLEEP, ms, "Sleep " + ms + "ms");
        }

        // Strip braces
        String inner = t.replaceAll("[{}]", "").trim();

        return switch (inner.toLowerCase()) {
            case "lbutton down"  -> new MacroAction(Type.MOUSE_LEFT_DOWN, 0, "LMB Hold");
            case "lbutton up"    -> new MacroAction(Type.MOUSE_LEFT_UP,   0, "LMB Release");
            case "lbutton"       -> new MacroAction(Type.MOUSE_LEFT,      0, "LMB Click");
            case "rbutton"       -> new MacroAction(Type.MOUSE_RIGHT,     0, "RMB Click");
            case "xbutton1"      -> new MacroAction(Type.MOUSE_BUTTON4,   0, "Side Button 1");
            case "xbutton2"      -> new MacroAction(Type.MOUSE_BUTTON5,   0, "Side Button 2");
            case "shift down"    -> new MacroAction(Type.SHIFT_DOWN,      0, "Shift Hold");
            case "shift up"      -> new MacroAction(Type.SHIFT_UP,        0, "Shift Release");
            default -> {
                // Single character key (q, 3, etc.)
                int code = inner.length() == 1 ? inner.charAt(0) : 0;
                yield new MacroAction(Type.KEY_PRESS, code, "Key: " + inner);
            }
        };
    }

    public String toToken() {
        return switch (type) {
            case SLEEP           -> "Sleep " + value;
            case MOUSE_LEFT      -> "{LButton}";
            case MOUSE_LEFT_DOWN -> "{LButton down}";
            case MOUSE_LEFT_UP   -> "{LButton up}";
            case MOUSE_RIGHT     -> "{RButton}";
            case MOUSE_BUTTON4   -> "{XButton1}";
            case MOUSE_BUTTON5   -> "{XButton2}";
            case SHIFT_DOWN      -> "{Shift down}";
            case SHIFT_UP        -> "{Shift up}";
            case KEY_PRESS       -> "{" + (char) value + "}";
            default              -> label;
        };
    }

    @Override
    public String toString() { return label; }
}
