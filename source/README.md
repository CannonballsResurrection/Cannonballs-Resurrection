# Cannonballs! — game source

The decompiled source of WildTangent's **Cannonballs!** (2002), the core reference
this whole project is built around. Everything else here (the format decoders, the
asset pipeline, the native macOS rebuild) is derived from reading this code.

## What this is

*Cannonballs!* was a Java game running on WildTangent's **WebDriver** engine. The
`.jar` was recovered from the original install and decompiled with
[CFR](https://www.benf.org/other/cfr/) `0.152`. These 84 classes are the game
logic in the default package; they call into the engine via
`wildtangent.webdriver.*`. That engine API lives in [`engine/`](engine/) so the codebase reads as a whole — reference-only, since the real engine is the native runtime in [`engine/native/`](engine/native/) (see `engine/README.md`).

## Reading order

- **`Main.java`** — entry point, top-level state machine.
- **`Gameplay.java`, `Game_Loop.java`** — turn flow, physics, scoring.
- **`Cannon.java`, `Camera.java`, `Island.java`, `LandPatch.java`** — world + player.
- **`HUD.java`, `Menu_*_Screen.java`, `Button_*.java`** — the UI. These files gave
  the exact 800×600 layout the macOS rebuild reproduces.
- **`Media_Object*.java`, `IO.java`, `Packet*.java`, `Network.java`** — asset
  loading and the (now-dead) multiplayer protocol.
- **`Global.java`, `Global_Media.java`** — constants: weapon tables, colors, the
  media manifest.

## Provenance & copyright

This is decompiled output, not a hand-written clean-room reimplementation. The
code is © WildTangent (IP now held by **Gamigo**) and is included here for
preservation, study, and interoperability of a discontinued product. See
[`../LEGAL.md`](../LEGAL.md). If you are the rights holder and want it removed,
see the takedown note there.
