# Cannonballs! Resurrection — our notes & experiments (not Kenneth's)

This material used to live inside [`../../../resurrection-archive/`](../../../resurrection-archive/),
but that archive is meant to hold **only Kenneth's own work** (his website, his "Access all Islands"
patch, his 2016 follow-up). Everything here is **ours** — what we produced in 2026 while trying to
get the game running on modern hardware. It's parked in `notes-and-dead-ends/` because the main
thing it chases (a native/emulated macOS play path) never panned out.

## What's here

- **`ATTEMPTS.md`** — the full log of everything we tried to run the game on a Mac, and what
  worked / didn't.
- **`tools/`** — Windows-XP-VM scripts for Apple Silicon (QEMU): `run-vm.sh`, `run-install.sh`,
  the unattended-install answer file `winnt.sif`, the `nsis_extract.py` installer extractor, and
  `fetch-game-files.sh` (pulls the game binary + Microsoft Java VM from the Internet Archive, since
  those third-party copyrighted files aren't committed).
- **`docs/`**
  - `install-on-windows.md` — the reliable way to install and play it on a real Windows PC.
  - `cannonballs-research.md` — deep research on the game (history, the WLD3/DirectX engine, file
    formats, preservation, running it on modern hardware).
  - `cannonballs-resurrection-research.md` — research on the fan project and its author.
  - `running-on-macos.md` — the macOS story: QEMU, the 86Box attempt, and why neither emulator path
    completes on Apple Silicon.
  - `nsis-extraction.md` — how the installer payload was decoded.

## Still useful vs. actual dead end

Not all of this is worthless — the "dead-ends" side of the folder name is about the emulation goal, not every file:

- **Still-good reference:** `docs/install-on-windows.md` (the Windows path genuinely works),
  `docs/cannonballs-research.md`, and `docs/cannonballs-resurrection-research.md`. Read these if you
  want to actually play the game or understand its history.
- **True dead end:** the `tools/` VM scripts and `docs/running-on-macos.md` / `ATTEMPTS.md`. No
  emulator path completes on Apple Silicon (QEMU has input but no 3D; 86Box boots XP but has no
  working keyboard), so the WLD3 (DirectX 7/8) scene never renders. Don't sink more time into the
  VM route.

For Kenneth's actual project material and full credits, see
[`../../../resurrection-archive/`](../../../resurrection-archive/).
