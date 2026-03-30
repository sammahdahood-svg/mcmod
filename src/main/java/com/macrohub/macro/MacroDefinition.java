package com.macrohub.macro;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one macro entry — a named sequence of MacroActions
 * with a bound hotkey and inter-action delay.
 *
 * Directly mirrors the Config entries in the original AHK script:
 *   Config["StunSlam"] := {hotkey: "F1", keys: [...], delay: 40}
 */
public class MacroDefinition {

    private String name;
    private int hotkey;          // GLFW key code
    private int delay;           // ms between actions (AHK: data.delay)
    private List<MacroAction> actions;
    private boolean enabled;

    public MacroDefinition(String name, int hotkey, int delay, List<MacroAction> actions) {
        this.name    = name;
        this.hotkey  = hotkey;
        this.delay   = delay;
        this.actions = new ArrayList<>(actions);
        this.enabled = true;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public String getName()              { return name; }
    public void   setName(String n)      { this.name = n; }

    public int  getHotkey()              { return hotkey; }
    public void setHotkey(int k)         { this.hotkey = k; }

    public int  getDelay()               { return delay; }
    public void setDelay(int d)          { this.delay = d; }

    public List<MacroAction> getActions() { return actions; }
    public void setActions(List<MacroAction> a) { this.actions = new ArrayList<>(a); }

    public boolean isEnabled()           { return enabled; }
    public void    setEnabled(boolean e) { this.enabled = e; }

    /** Build a human-readable summary of the key sequence. */
    public String getSequenceSummary() {
        if (actions.isEmpty()) return "(empty)";
        StringBuilder sb = new StringBuilder();
        for (MacroAction a : actions) {
            if (!sb.isEmpty()) sb.append(" → ");
            sb.append(a.getLabel());
        }
        return sb.toString();
    }

    /** Serialise actions to a pipe-separated token string (for config persistence). */
    public String actionsToString() {
        StringBuilder sb = new StringBuilder();
        for (MacroAction a : actions) {
            if (!sb.isEmpty()) sb.append("|");
            sb.append(a.toToken());
        }
        return sb.toString();
    }

    /** Parse a pipe-separated token string back into actions. */
    public static List<MacroAction> actionsFromString(String s) {
        List<MacroAction> list = new ArrayList<>();
        if (s == null || s.isBlank()) return list;
        for (String token : s.split("\\|")) {
            if (!token.isBlank()) list.add(MacroAction.fromToken(token));
        }
        return list;
    }

    @Override
    public String toString() {
        return name + " [key=" + hotkey + ", delay=" + delay + "ms, actions=" + actions.size() + "]";
    }
}
