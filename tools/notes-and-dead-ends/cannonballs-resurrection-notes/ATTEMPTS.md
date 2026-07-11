# Attempts log — getting Cannonballs! running on modern hardware (esp. Apple Silicon macOS)

A faithful record of everything tried, so others don't repeat the dead ends. Host machine was an
Apple Silicon Mac (macOS 26). Goal: actually play Kenneth's all-islands build.

## TL;DR
- The game is a **WildTangent Web Driver** title using the **WLD3 (DirectX 7/8)** engine, the
  **Microsoft Java VM**, and an **MSHTML/IE** launcher shell.
- **Native macOS: impossible.** It's a 32-bit Windows app.
- **Wine/CrossOver: dead end.** The NSIS installer crashes deterministically.
- **Windows XP VM (QEMU/UTM) on Apple Silicon: installs + launches + loads, but the WLD3 3D scene
  crashes at render** — emulated graphics (no GPU DirectX) can't drive the engine. Tried std VGA,
  Cirrus, software DirectDraw, software Direct3D. All crash.
- **Kenneth's patch works perfectly** in the VM: the "Register Now" trial gate is gone and every
  island is selectable.
- **Real Windows works** (single-player) — confirmed by gameplay footage online.
- **86Box** (software-emulates a period 3D card) **boots Windows XP fine** — but its **keyboard input
  never reaches the guest** on this Apple Silicon Mac, even after granting macOS Input Monitoring,
  switching to a PS/2 keyboard, and disabling App Nap. Without a keyboard you can't drive the game, so
  it's a dead end here too (details in §7).
- **Net on-Mac verdict:** QEMU gives input but **no 3D**; 86Box gives a period 3D card but **no working
  keyboard**. Neither yields playable gameplay on Apple Silicon. **Use a real Windows PC** — see
  [docs/install-on-windows.md](docs/install-on-windows.md).

## 1. Native macOS — N/A
32-bit Windows binary; no native path. Expected.

## 2. Wine / CrossOver (Apple Silicon)
- Wine 11.9 (staging) + game-porting-toolkit installed.
- The WildTangent NSIS installer (`Cannonballs 1.0.exe` and `cannonballs-setup.exe`) **crashes
  deterministically** under Wine: `Unhandled page fault ... at address 0065679A` — same address every
  time, both the 2002 and 2008 builds. Setting Windows version to XP didn't help; `/NCRC` didn't help.
- Conclusion: the WildTangent NSIS exehead/plugin doesn't run under Wine here. Even past the
  installer, the WebDriver host (MSHTML + MS Java + DirectX) is the known-hardest preservation case.

## 3. Reverse-engineering the installer
- The installers are **Nullsoft (NSIS) self-extractors** bundling the WildTangent 3D runtime
  (`WLD3.cab`). Modern 7-Zip can't decode the 2002-era NSIS script (dumps the payload as one blob).
- Wrote a small **Python deflate extractor** (`tools/nsis_extract.py`) that reads each `[size][deflate]`
  block and dumps all 511 payload files (exe, DLLs, the WLD3 CAB, HTML/JS, .wt assets). Useful for
  inspection; not needed if you just install in a real Windows/VM.

## 4. Windows XP VM via QEMU (the main effort)
- Pre-built XP disk images (VirtualPC VHD, VirtualBox VDI) **crash-loop** under QEMU (HAL/driver
  mismatch). Did a **fresh unattended XP SP3 install** instead (`tools/winnt.sif` on a floppy).
- **Critical QEMU gotchas on Apple Silicon (qemu 11):**
  - Must use **`-cpu max`** — named CPUs (`pentium3`, `Penryn`, `qemu32`) hang before ntldr here.
  - **`-device usb-tablet`** required for a working (absolute) mouse.
  - `-vga std` tops out at 640×480.
- Installed the Microsoft Java VM, the game, and Kenneth's `island.exe` patch.
- **Game-side fixes that mattered** (registry / AppCompatFlags):
  - `CannonballsLaunch.exe` → **Windows 98 compatibility** (gets past WLD3 init).
  - Display must be **16-bit color** (game rejects 8-bit; warns and bails).
  - It warns **"No 3D hardware acceleration found"** → click OK to fall back to software.
- **GUID/folder gotcha:** the patch (`island.exe`) targets the game folder GUID created by
  `cannonballs-setup.exe` (2008 build), **not** the 2002 `Cannonballs 1.0.exe` GUID. Install the
  matching base, or merge the base game files into the patch's folder so both live together.
- **IE security gate:** the trial page's active content is blocked by IE's Local Machine Zone
  lockdown ("Information Bar"). Allowing blocked content (or lowering the zone) gets past it — but
  Kenneth's patch already removes the in-launcher Register button, which is the real gate.

## 5. The wall: the WLD3 3D scene crashes in QEMU
With everything installed + patched + merged, clicking **PLAY GAME / Launch Game**:
- The game **loads** (RAM climbs to ~0.8 GB), then **crashes/exits straight to the patched
  "Thanks for playing Cannonballs Resurrection!" page** — every time.
- Tried: `-vga std`, `-vga cirrus` (real DirectDraw), `HKLM\...\DirectDraw\EmulationOnly=1`
  (force software DirectDraw), `Direct3D\Drivers\SoftwareOnly=1`. All crash at the 3D scene.
- Root cause: QEMU's TCG software graphics give the guest **no Direct3D device**; the WLD3 engine's
  software path won't survive it. This is the documented Apple-Silicon limitation — mainstream
  hypervisors (Parallels/VMware) only run **ARM** Windows there, and x86 emulation has no 3D.

## 6. 86Box — boots Windows XP, but keyboard input is a dead end (Apple Silicon)
86Box is the one emulator that **software-emulates a period 3D card** (S3 ViRGE/DX, 3dfx Voodoo),
giving the guest a real Direct3D device — so in theory it's the on-Mac path that could satisfy the
WLD3 engine. We got much further than expected, then hit a wall that isn't about graphics at all.

**Setup that worked:** machine `p2bls` (ASUS P2B-LS, 440BX, Award BIOS), `pentium2_deschutes` + dynarec,
`gfxcard = virge_dx_pci`, the XP disk migrated from the QEMU build as a raw `.img` on the onboard IDE
(geometry `63, 16, 20806`), 256 MB RAM. ROMs from github.com/86Box/roms.

**Two misdiagnoses we cleared up:**
- **"Falls to ROM BASIC / won't boot the disk"** was **not** a BIOS boot-order problem. It was the
  `86box.cfg` silently reverting to `machine = ibmpc` (an 8088 IBM PC, which *has* ROM BASIC). With the
  correct `p2bls` machine, **the disk boots** — it reaches Windows XP's NTLDR. (Migrating a
  QEMU-prepared XP image to 86Box's chipset booted fine; MergeIDE had been applied beforehand.)
- **"The VM hangs — frozen boot countdown, unresponsive"** was **macOS App Nap** throttling 86Box to a
  crawl whenever it isn't the frontmost window (CPU ~99% foreground vs ~20–30% backgrounded). Fix:
  `defaults write net.86box.86Box NSAppSleepDisabled -bool YES`. After that the NTLDR boot-menu
  countdown ticked down and **auto-booted on its own**. Also: saving the CMOS once writes
  `nvr/p2bls.nvr`, which removes the "Press F1 to continue" pause on cold boot.

**The actual wall — keyboard input never reaches the guest.** On this Apple Silicon Mac, 86Box build
9001 does not deliver keystrokes to the emulated machine. Things tried, all unsuccessful:
- **Synthetic input** (`osascript` key codes, `cliclick kp:`) — confirmed *not* received (arrow keys
  never moved the NTLDR menu highlight).
- **Real human keypresses** — also not received, even after:
  - granting **Input Monitoring** (and Accessibility) to 86Box in macOS Privacy & Security,
  - setting `keyboard_type = keyboard_ps2` (a 440BX board's correct keyboard; `keyboard_at` /
    `keyboard_pc_xt` are the other valid tokens),
  - clicking inside the screen to **capture input** first.

  A telling side effect: capturing the mouse (`mouse_type = ps2`; `ps2_2button` is *not* a valid token
  and silently reverts to `none`) grabs the cursor **globally**, and the release combo is a *keystroke*
  (Ctrl+End → Ctrl+Fn+→ on a Mac). With keyboard dead, you can't release it — you have to force-quit.

**Conclusion:** 86Box can *boot* the game's OS and offers a real Direct3D device, but with no keyboard
it can't run the game here. Combined with QEMU's no-3D wall, **there is no complete working
emulator path on Apple Silicon.** Stopped here deliberately.

## 7. What actually works
- **Real x86 Windows PC** (XP/7 ideal, 10/11 works) — plays fine, single-player. This is the reliable
  route; see **[docs/install-on-windows.md](docs/install-on-windows.md)**.
- **Parallels Desktop on Apple Silicon** (Windows 11 ARM + x86 emulation + Parallels' DirectX) — the
  best shot at running it *on a Mac*, but paid, needs a Windows license, and is **untested for this
  game's DX7/8 engine**.
- **Cloud Windows** (GPU cloud VM, Shadow, etc.) — works, ongoing cost/friction.
- **Boot Camp** is Intel-Mac only (not available on Apple Silicon).

## Files referenced
- `tools/run-install.sh`, `tools/run-vm.sh`, `tools/winnt.sif` — the QEMU XP build.
- `tools/nsis_extract.py` — the installer payload extractor.
- `tools/fetch-game-files.sh` — pull the game + MS Java from the Internet Archive.
- `resurrection-archive/patch/island.exe` — Kenneth's all-islands patch.
