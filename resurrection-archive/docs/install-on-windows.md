# Install & play Cannonballs! on a real Windows PC (the reliable way)

This is the path that actually works. On a real x86 Windows machine (or any environment with a real
GPU + DirectX), Kenneth's all-islands build plays single-player exactly as he documented. No
emulator gymnastics required.

> **Why "real Windows"?** The game's **WLD3 engine is DirectX 7/8** and needs a real (or
> period-accurate) Direct3D device. Software-emulated graphics on Apple Silicon don't provide that —
> see [running-on-macos.md](running-on-macos.md) and [../ATTEMPTS.md](../ATTEMPTS.md) for the full
> story of why the Mac route stalls. On real Windows hardware, none of that is a problem.

## What you need
- A **Windows PC** — Windows XP/Vista/7 is ideal; **Windows 10/11 works too** (see the 64-bit notes
  below). Even a cheap used mini-PC or an old laptop does the job.
- The **game installer**, the **Microsoft Java VM**, and Kenneth's **`island.exe`** patch.
  - The patch is in this repo: [`../patch/island.exe`](../patch/island.exe).
  - The game + MS Java are third-party copyrighted files and are **not** hosted here; pull them from
    the Internet Archive with [`../tools/fetch-game-files.sh`](../tools/fetch-game-files.sh) (or
    download them manually from the `wildtangent-archive` collection).

## Step 0 — Safety first
Old WildTangent software can trip antivirus "PUP" flags because of its era's telemetry (different
from active malware). Still:
- **Scan every download with VirusTotal** before running it.
- **Open any `.bat` in a text editor first** to see what it does (the `win1064bitunlock.bat` only
  sets registry compatibility flags — it's safe, but verify).

## Step 1 — Get the files
On any machine with `curl`/`zsh`:
```sh
./tools/fetch-game-files.sh ./game-files
```
That downloads `Cannonballs.zip`, the larger "unofficial copy" zip (Kenneth's full distribution: the
game `cannonballs-setup.exe`, the Microsoft Java VM `msjavx86.exe`, the soundtrack, the wallpaper,
and `island.zip`), and `win1064bitunlock.bat`. Copy the extracted files to your Windows PC.

You already have the patch (`patch/island.exe` in this repo) — copy that over too.

## Step 2 — Install the Microsoft Java VM (mandatory)
WildTangent games require the **Microsoft Java VM** — this is not optional, per Kenneth.
- Run **`msjavx86.exe`** and finish the installer **before** installing the game.
- On **Windows XP/Vista/7** it installs cleanly.
- On **Windows 10/11**, the legacy MS Java installer may refuse to run. If so, run it in
  **compatibility mode** (right-click → Properties → Compatibility → "Run this program in
  compatibility mode for: Windows XP SP3"). If it still won't install, use an **XP/7 machine or VM**
  for the smoothest result — this dependency is the main reason older Windows is preferable.

## Step 3 — Install the game
- Run **`cannonballs-setup.exe`** (the 2008 trial installer from Kenneth's distribution) and let it
  finish. It installs into the WildTangent GameChannel folder under
  `C:\Program Files\WildTangent\Apps\GameChannel\Games\<GUID>\`.

## Step 4 — Apply Kenneth's all-islands patch
- Run **`island.exe`** (from `patch/`). This is the **"Access all Islands"** patch: it removes the
  in-launcher **"Register Now"** trial gate and changes the exit page.
- **Important — the patch unlocks islands one at a time.** To switch islands you: close the game,
  re-run the patch (or pick the next island in the launcher), then reopen. The islands are: Voodoo
  Isles, Skull Isle, Pyramids, Volcano, Ziggurat, Crossbones, Nightbridge, Overboard, Moonlight Cove,
  Tropical, Old Gods.

> **GUID/folder gotcha:** the patch targets the game-folder GUID created by `cannonballs-setup.exe`
> (the 2008 build), **not** the older 2002 `Cannonballs 1.0.exe`. Install the matching base above and
> they line up. (If you ever mix builds, merge the base game files into the patch's folder so the
> WLD3 engine and the patched launcher live together.)

## Step 5 — Launch the game
- The patch installs **`cannonballs.exe`** — the **black-and-white-icon launcher** — in the install
  folder. From it: pick an island → **Launch Game**.
- **On 64-bit Windows** (this is Kenneth's key tip): the normal shortcut may not work. **Manually
  navigate to the install folder and run `cannonballs.exe`** (the b&w icon one) directly:
  `C:\Program Files\WildTangent\Apps\GameChannel\Games\<GUID>\cannonballs.exe`
- Also on 64-bit Windows, run **`win1064bitunlock.bat`** (from the fetch script) once — it registers
  `params.dll` so the launcher works. **Inspect it in a text editor first** (it only sets registry
  keys).

## Optional compatibility tweaks (only if it misbehaves)
On most real Windows these aren't needed, but if the launcher stalls or the screen errors:
- Set **`CannonballsLaunch.exe`** (and/or `cannonballs.exe`) to **Windows 98 compatibility**
  (Properties → Compatibility).
- If it complains about color depth, set the desktop to **16-bit color**.
- If it warns **"No 3D hardware acceleration found,"** click OK — on real hardware it'll still run;
  this warning is only fatal on the emulator path.
- If Internet Explorer's "Information Bar" blocks the launcher's active content, choose **Allow
  Blocked Content** (Kenneth's patch already removes the actual Register gate).

## What you get — and what's gone forever
- **Single-player: fully playable.** Independently confirmed by gameplay footage online
  (YouTuber *StellarGwynn* recorded Tropicali, Moonlight Cove, and Voodoo Isles runs on real Windows).
- **Multiplayer (Guest Login / the lobby): permanently dead.** WildTangent shut down the lobby/license
  servers years ago. Nothing can bring that back — this is server-side, not something the patch
  touches.

## Need help?
Kenneth (CaptSmokey6) left an open offer to help anyone using his patch:
**ken@captsmokey.com** / **captsmokey6@gmail.com**. See
[kenneth-2016-followup.md](kenneth-2016-followup.md). Please be kind — this is a one-person passion
project he kept alive for over a decade.
