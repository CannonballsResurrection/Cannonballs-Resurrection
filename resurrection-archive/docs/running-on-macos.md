# Running Cannonballs! on a Mac (and the hard part)

Cannonballs! is a 32-bit Windows game built on WildTangent's **WLD3 (DirectX 7/8)** engine, launched
through an **MSHTML/IE** shell, and it depends on the **Microsoft Java VM**. That stack is why it's
hard to revive.

## The honest summary
**Bottom line: there is no complete working emulator path on Apple Silicon.** The two viable emulators
each fail on a *different* axis — QEMU has input but no 3D; 86Box has a 3D card but no working
keyboard. To actually play, use a real Windows PC: **[install-on-windows.md](install-on-windows.md)**.

- **Native macOS:** not possible (32-bit Windows app).
- **Wine / CrossOver:** the WildTangent NSIS installer crashes deterministically; even past it, the
  MSHTML + MS-Java + DirectX combo is the worst-case for Wine.
- **Windows XP VM (QEMU/UTM) on Apple Silicon:** everything installs, the patched launcher runs with
  all islands, the game **loads** — but the 3D scene **crashes at render** because the emulated
  graphics give the guest no Direct3D. Keyboard/mouse work fine; graphics don't. (Details + every fix
  tried: [../ATTEMPTS.md](../ATTEMPTS.md).)
- **86Box:** software-emulates a period 3D card, so it *can* drive DX7/8, and we got it to **boot
  Windows XP** — but on this Mac its **keyboard input never reaches the guest** (even with Input
  Monitoring granted, a PS/2 keyboard, and App Nap disabled), so the game can't be driven. Dead end
  here. (Full write-up: [../ATTEMPTS.md](../ATTEMPTS.md) §6.)
- **Parallels Desktop (Apple Silicon):** Windows 11 ARM + x86 emulation + Parallels' own DirectX — the
  best shot at running it *on a Mac*, but paid and **untested for this DX7/8 engine**.
- **Real Windows PC:** works (single-player). The reliable route —
  [install-on-windows.md](install-on-windows.md). (Boot Camp is Intel-Mac only.)

## Build a Windows XP VM with QEMU (Apple Silicon)
You'll need `qemu` (e.g. `brew install qemu`) and a Windows XP SP3 ISO.

1. Create a disk and do a fresh install (pre-built XP images tend to crash-loop under QEMU):
   ```sh
   qemu-img create -f qcow2 xp.qcow2 10G
   ```
   Use `tools/winnt.sif` on a floppy image for an unattended install, and boot the ISO. **Use
   `-cpu max`** (named CPU models hang before ntldr on Apple Silicon QEMU).
2. After install, launch with `tools/run-vm.sh` (key flags: `-cpu max`, `-device usb-tablet`,
   `-vga std` or `-vga cirrus`, `-rtc base=localtime`). Set `$VMDIR`/paths first.
3. Inside XP: run `msjavx86.exe` (Microsoft Java VM), then `cannonballs-setup.exe` (the game), then
   Kenneth's `island.exe` (the all-islands patch). In the patch's launcher: **Access [island]** →
   **Launch Game**.
4. Compatibility tweaks that helped: set `CannonballsLaunch.exe` to **Windows 98** compatibility, and
   keep the desktop at **16-bit color**. The game will warn "No 3D hardware acceleration" — click OK.

That gets you to a loading game. On QEMU it then crashes at the 3D scene (the wall). On real Windows
it plays.

## Driving a headless QEMU VM (handy for automation)
Add `-vnc 127.0.0.1:1`, `-monitor unix:/tmp/mon.sock,server,nowait`, and
`-qmp unix:/tmp/qmp.sock,server,nowait`. Then:
- Screenshot the guest: `printf 'screendump /tmp/g.ppm\n' | socat - UNIX-CONNECT:/tmp/mon.sock`
- Keystrokes: `printf 'sendkey ret\n' | socat - UNIX-CONNECT:/tmp/mon.sock`
- Precise mouse via QMP `input-send-event` (absolute 0–32767 axis values mapped from screen pixels).

Note: a **fullscreen DirectDraw** game won't be captured by `screendump` (it shows the desktop); use
a native `-display cocoa` window to actually see it.
