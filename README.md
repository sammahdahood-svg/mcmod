# ⚔ MacroHub — Minecraft Macro Manager

A faithful in-game port of the **MinecraftMacroHub AHK script** for Minecraft **1.21.1** (Fabric).

---

## Features

All 6 original AHK macros are included out of the box:

| Macro | Default Key | What it does |
|-------|------------|--------------|
| **StunSlam** | F1 | Side button → LMB hold/release → slot 3 → LMB hold/release (80ms gap) |
| **Anchor** | F2 | RMB → Shift+LMB combo |
| **AttribSwap1** | F3 | Drop item (Q) → wait 60ms → Side button 2 |
| **AttribSwap2** | F4 | LMB tap → wait 3ms → slot 3 |
| **CustomMacro1** | F5 | Empty — configure in-game |
| **CustomMacro2** | F6 | Empty — configure in-game |

---

## Usage

1. Install the mod (requires **Fabric Loader ≥ 0.16** + **Fabric API**)
2. Launch Minecraft 1.21.1
3. Press **M** to open the Macro Manager GUI
4. Edit hotkeys, adjust delays, add/modify key sequences
5. Press **Save Config** — settings persist across sessions in `.minecraft/config/macrohub.json`

---

## GUI Overview

- **MACRO BINDINGS** — list of all macros with their hotkey field, Edit, Test, and ON/OFF toggle
- **Edit** — opens a per-macro editor where you can change the key sequence token by token
- **Test** — fires the macro immediately (500ms delay for safety)
- **Apply Binds** — registers the new hotkeys from the text fields
- **Save Config** — writes everything to disk
- **Defaults** — resets all macros to the original AHK defaults

---

## Key Sequence Tokens

In the macro editor, each row accepts one AHK-style token:

```
{LButton}         Left click (press + release)
{LButton down}    Hold left mouse button
{LButton up}      Release left mouse button
{RButton}         Right click
{XButton1}        Side mouse button 1 → hotbar slot 1
{XButton2}        Side mouse button 2 → hotbar slot 2
{Shift down}      Hold sneak/shift
{Shift up}        Release sneak/shift
{q}               Drop held item
{3}               Select hotbar slot 3
Sleep 80          Wait 80 milliseconds
```

---

## Config File

Located at: `.minecraft/config/macrohub.json`

Automatically created on first launch. Edit manually or use the in-game GUI.

---

## Requirements

- Minecraft **1.21.1**
- Fabric Loader **≥ 0.16.9**
- Fabric API **0.102.0+1.21.1**
- Java **21**

---

## Building from Source

```bash
./gradlew build
# Output: build/libs/macrohub-1.0.0.jar
```
