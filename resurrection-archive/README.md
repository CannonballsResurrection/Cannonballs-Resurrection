> **Note:** this folder preserves the original *Cannonballs! Resurrection* project material. The repo's primary focus is now the native macOS remake at the [repo root](../README.md).

# Cannonballs! Resurrection — Archive

A preservation archive of **Cannonballs! Resurrection**, the fan project that kept WildTangent's
discontinued game *Cannonballs!* (2002) alive — created by **Kenneth ("Ken")**, who also went by
**htennek98**, **CaptSmokey6**, and **CrazyCannon**.

> **All credit for the Cannonballs! Resurrection project — the website, the "Access all Islands"
> patch, and the years of effort to keep this game playable — belongs to Kenneth.** See
> [CREDITS.md](CREDITS.md). This repository only collects and documents his work (plus our notes
> on getting it running on modern machines) so it isn't lost.

## What *Cannonballs!* is
A turn-based artillery/strategy game (think *Worms* on tropical islands) WildTangent shipped on
early-2000s OEM PCs and via GameChannel. WildTangent discontinued it; the IP is now held by Gamigo.
Single-player is still playable; the original multiplayer **lobby server is permanently gone**.

## What's in here
- **`website/`** — Kenneth's Cannonballs Resurrection website, preserved as-is (`.cs6` originals +
  `.html` copies for browsing). About page, FAQ, news, downloads, contact, links.
- **`patch/island.exe`** — Kenneth's **"Cannonballs! Access all Islands"** patch installer. This is
  the heart of the project: it removes the in-launcher "Register Now" trial gate and lets you unlock
  the islands (Voodoo Isles, Skull Isle, Pyramids, Volcano, Ziggurat, Crossbones, Nightbridge,
  Overboard, Moonlight Cove, Tropical, Old Gods) **one at a time** — close the game, apply the next
  island's patch from the launcher, reopen.
- **`tools/`** — our scripts for standing up a Windows XP VM (QEMU) on Apple Silicon: unattended XP
  install answer file, launch scripts, and the Python NSIS extractor used to reverse the installer.
- **`tools/fetch-game-files.sh`** — downloads the game itself + the Microsoft Java VM (a dependency)
  from the Internet Archive. See note below on why those aren't hosted here.
- **`docs/`** — reference material and guides:
  - **`install-on-windows.md`** — the reliable way to install and play it on a real Windows PC. **Start
    here if you just want to play.**
  - **`cannonballs-research.md`** — deep research on the game itself (history, the WLD3/DirectX
    engine, file formats, where it's preserved, and the realistic ways to run it on modern hardware).
  - **`cannonballs-resurrection-research.md`** — research on the fan project and its author.
  - **`running-on-macos.md`** — the macOS story: a Windows XP VM via QEMU, the 86Box attempt, and why
    neither emulator path completes on Apple Silicon.
  - **`nsis-extraction.md`** — how the installer payload was decoded.
- **[ATTEMPTS.md](ATTEMPTS.md)** — a full log of everything we tried to get it running natively on a
  Mac (and what worked / what didn't).
- **[docs/kenneth-2016-followup.md](docs/kenneth-2016-followup.md)** — Kenneth's 2016 follow-up (as
  CaptSmokey6) on the WildTangent Restoration blog: updated download links, the key "launch
  cannonballs.exe manually on 64-bit Windows" tip, and current contact. A sequel to his original site.

## What's NOT hosted here (and why)
The raw **WildTangent game binary, the Microsoft Java VM, the soundtrack, and the wallpaper** are
third-party copyrighted material. A community mirror of these on Mega was DMCA-taken-down by the
rights holder, so they are **not** committed here — `tools/fetch-game-files.sh` pulls them from the
Internet Archive instead. Kenneth's own work (his website + his patch) is preserved here.

## How to actually play it
The reliable path is a **real Windows PC** (or any environment with a real GPU + DirectX). In short:
1. Install the **Microsoft Java VM** (`msjavx86`).
2. Install the game (`cannonballs-setup.exe`).
3. Run Kenneth's **`island.exe`** patch, click **Access [island]**, then **Launch Game**.
4. On 64-bit Windows, launch `cannonballs.exe` (the black-and-white-icon launcher) manually.

**Full step-by-step:** [docs/install-on-windows.md](docs/install-on-windows.md).

Trying to get it onto a **Mac**? The honest answer is that no emulator path completes on Apple Silicon
(QEMU has input but no 3D; 86Box boots XP but has no working keyboard). The full story and every fix
tried: [docs/running-on-macos.md](docs/running-on-macos.md) and [ATTEMPTS.md](ATTEMPTS.md).

## Status
- Kenneth's patch works: trial gate removed, all islands selectable, game launches and loads.
- On a **real Windows** machine it plays (single-player) as Kenneth documented.
- In a **QEMU/UTM VM on Apple Silicon**, everything installs and loads but the WLD3 (DirectX 7/8)
  game scene crashes at render because the emulated graphics can't drive it — see ATTEMPTS.md.

## Takedown
This is a non-commercial preservation archive of an abandoned game and a now-defunct fan project.
If you hold rights and want something removed, open an issue.
