# Deep research: *Cannonballs!* by WildTangent — history, engine, and how to run it

Reference material: a research dive into the game itself (what it is, the technology behind it, where
it's preserved, and the realistic options for running it on modern machines, especially macOS).

## Summary
*Cannonballs!* (~2002) is a discontinued WildTangent casual game: a turn-based, tropical-island
multiplayer artillery game in the spirit of *Worms*. It survives only in community archives. The
crucial technical fact for running it: the standalone installer is a **Nullsoft (NSIS) self-extractor**
that bundles WildTangent's own 3D runtime (`WLD3.cab`), so it does **not** depend on the dead
browser plug-in — but the game engine is **WLD3 (DirectX 7/8)** and it needs the **Microsoft Java VM**
and an **MSHTML/IE** launcher shell.

## What the game is
- **Cannonballs!** by WildTangent, ~2002 (in active use by April 2003 per a DSLReports "Cannonballs
  at shockwave.com" thread).
- Turn-based artillery/strategy, tropical-island setting, up to 8 players + CPU opponents. Weapons
  (x-shot, spikeroller, dumbfire) cost in-game gold; defenses include molehills, towers, teleports.
  Calypso soundtrack.
- Distributed via WildTangent's GameChannel (OEM-bundled on HP/Dell PCs) and via Shockwave.com.
  Trialware model (free to try, unlock to play fully).
- Now "partially lost media." WildTangent's catalog was acquired by **Gamigo AG** (2019), so it's
  delisted rather than truly abandoned.
- **Disambiguation:** not the same as WildTangent's current "3D Cannon Ball" (2025) or Shockwave's
  "Cannon Blast."

## Where it's preserved (and safety)
- **Internet Archive** `wildtangent-archive` collection — folder
  `Games/Original/Download/Cannonballs/` has `Cannonballs.zip`, a larger "unofficial copy" zip (a
  scrape of Kenneth's distribution site: the game, MS Java, soundtrack, wallpaper, and his
  `island.zip` patch), and a `win1064bitunlock.bat` (a registry tweak that registers `params.dll`
  for 64-bit Windows).
- **pug.quest WildTangent Archive** + its Discord — community preservation (a Mega copy was
  DMCA-blocked).
- **NOT on MyAbandonware.**
- **Safety:** scan any download with VirusTotal; inspect any `.bat` in a text editor first (the
  win10 one only sets registry compatibility flags). Old WildTangent software may trip AV "PUP"
  flags due to its telemetry — different from active malware.

## The technology (why it's hard)
- **WildTangent Web Driver / WLD3:** a DirectX-backed 3D runtime (based on the acquired Genesis3D
  engine). Originally an IE/Netscape **NPAPI/ActiveX plug-in**; the standalone game bundles the same
  runtime via a local host (`CannonballsLaunch.exe` / wthost) that drives an **MSHTML** (IE) shell.
- **File formats:** `.wt` (compressed DirectX mesh; sometimes a plain ZIP inside), `.wpg`/`.wjp`
  (compressed PNG/JPG), `.wwv` (ADPCM audio), `.wsad/.wsgo/.wsmo/.wsbm` (actor/mesh/motion/material),
  `WLD3.cab` (the 3D runtime).
- **Dependencies:** 32-bit Windows, DirectX 7/8-era, the **Microsoft Java VM** (`jvmchecker.class`),
  and MSHTML. Server-side DRM/lobby once existed (now dead).
- **Why it's unplayable the easy ways:** NPAPI is gone from all browsers; the lobby/license servers
  are offline; it's a 32-bit Windows app; and the 3D engine needs real DirectX.

## Running it on modern macOS — the options
1. **Wine / CrossOver (Apple Silicon):** the NSIS installer crashes deterministically under Wine; the
   MSHTML + MS-Java + DirectX stack is the worst case. Dead end in practice.
2. **Windows VM:**
   - On **Intel Macs**: VMware Fusion / VirtualBox + Windows XP gives native x86 + hardware DirectDraw —
     the most capable legacy route.
   - On **Apple Silicon**: mainstream hypervisors (Parallels/VMware) only run **ARM** Windows, and
     x86 emulation (QEMU/UTM) has **no 3D acceleration** — so the WLD3 scene won't render (see
     `running-on-macos.md` and `../ATTEMPTS.md`).
   - **86Box** is the one emulator that software-emulates a **period 3D card** (Voodoo/S3), giving the
     guest a real Direct3D device — the most promising on-Apple-Silicon path, but heavy and slow.
3. **Browser-plug-in route:** not viable — NPAPI is dead even on real Windows.

## Bottom line
The standalone game + Kenneth's all-islands patch is the right setup, and it runs on **real Windows**.
On Apple Silicon, the blocker is purely graphics: the DirectX-7/8 engine needs a real (or
period-emulated, e.g. 86Box) GPU, which standard x86 emulation doesn't provide.

## Useful references
- WildTangent file-format reverse-engineering: `github.com/diamondman/WTExtractor`.
- Internet Archive `wildtangent-archive` (game + the Web Driver installer `WTWebDriverInstall`).
- Kenneth's project + the all-islands patch: see this repo's `resurrection-archive/` (`website/`,
  `patch/`, and `docs/kenneth-2016-followup.md`).
