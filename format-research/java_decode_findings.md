# WLD3 / .wwv / .wsad decode path — Java-side investigation

## VERDICT (definitive)

**Decoding of .wwv (audio) and .wsad (model/actor) is 100% NATIVE.** There is no Java-side
inflate/LZ/Huffman/arithmetic/XOR decode of WLD3 assets anywhere in the 211 decompiled files.
The game's Java code only passes asset *file paths* across a JNI boundary into the WildTangent
WebDriver native runtime, which does all container parsing and decompression internally.

The one pure-Java XOR routine that exists (`Gameplay.SetScreenBuffer` using
`XORBlockEncryptData.wtxt`) is **copy-protection name-scrambling for network player names**, NOT
audio/model asset decode. See section 3.

Next step for the orchestrator: reverse the native `webdriver.dll` + `wt3d.dll` +
`actorobject.dll` (WildTangent WebDriver 3.3.1.001), which are NOT in this dump — they install at
runtime under `WT\webdriver\`. Entry points below.

---

## 1. Every reference to asset loading / WLD3 strings (file:line)

WLD3 / XtoWT / "Compressed and Patented" / .RG .MX .FU .IA literals: **ZERO hits in Java source.**
They exist only inside the binary asset payloads (embedded in `cannonballs-setup.exe`). rg -i over
all 211 files returned 0 matches for `WLD3`, `XtoWT`, `Compressed`, `Patented`, `.RG`, `.MX`,
`.FU`, `.IA`, `0x1af8f2`.

Asset-load call sites (all delegate to native `Main.MainRef.Wt.*`):

| File:line | Call | Asset |
|---|---|---|
| `Media_Object_Sound.java:26,46,72,96,115,130,153,184` | `Main.MainRef.Wt.createAudioClip(MediaPath + Path + "/sound.wwv", CacheType)` → `wildtangent.webdriver.WTAudioClip` | **.wwv audio** |
| `Media_Object_Actor.java:37,47` | `Main.MainRef.Wt.createActor(MediaPath + Path + "/actor.wsad", CacheType)` → `wildtangent.webdriver.WTActor`/WTObject | **.wsad model** |
| `Media_Object_Shader.java:83..156` | `Main.MainRef.Wt.createSurfaceShader()` | shaders |
| `Global_Media.java` (many) | `MediaList.add(new Media_Object_Sound("MEDIA/SOUNDS/SPLASH"), true)` etc. | registers media objects |
| `Key.java:25` | `Main.MainRef.Wt.readFile(MediaPath + "/XORBlockEncryptData.wtxt")` → `WTFile.readAll()` | reads XOR key table (raw bytes, no decode here) |
| `Main.java:440` | `this.Wt = wt3dLib.getWT(object)` | obtains the native `WT` engine handle |

`Wt` is typed `WT Wt;` (`Main.java:28`), imported from `wildtangent.webdriver.wt3dLib`
(`Main.java:20`). All `createAudioClip`/`createActor`/`readFile`/`createActor` are **native methods
on classes that are NOT present in the source tree** (CFR header on every consumer: "Could not load
the following classes: wildtangent.webdriver.WTAudioClip / WTObject / WTOnLoadEvent / wt3dLib …").

## 2. Java vs native — per call

- `wildtangent.webdriver.*` package: **absent from source** (`find src -path '*wildtangent/webdriver*'`
  → empty). 180 files `import wildtangent.webdriver.*` as stubs. This package lives in the native
  WebDriver plugin, exposed to Java via JNI.
- **No `native` keyword** on any asset method. The only `native` methods in the whole tree are the
  Winsock network layer (`com/wildtangent/dmmp/client/transport/winsock/NativeNetwork.java:11-40`:
  `closeConnection/write/poll/acceptConnections/openConnection/init/getConnectionIP/shutdown`),
  loaded via `System.loadLibrary("WT\\webdriver\\wtdmmp")` (line 27) — that's multiplayer transport,
  not asset decode.
- `createAudioClip`/`createActor` have **no Java definition anywhere** (rg over `--type java`
  returns only the `Main.MainRef.Wt.` call sites). They are resolved in the native `WT` object
  returned by `wt3dLib.getWT()`.
- No `inflate`/`deflate`/`Inflater`/`GZIP`/`ZipInputStream` anywhere near asset loading.
- `CacheType = 2` (`Main.java:108`) is passed as the 2nd arg to create* — a native cache-mode flag,
  not a codec selector readable in Java.

**Conclusion: purely native. No transcribable Java algorithm for WLD3 decode exists.**

## 3. XOR / XORBlockEncryptData — what it actually is (NOT asset decode)

Chain: `Key.java:24-27` loads `XORBlockEncryptData.wtxt` via native `WTFile.readAll()` into
`Key.buffer` (raw bytes, no transform). Consumed only at:
- `Network.java:403` and `Network.java:1354`: `this.gameplay.SetScreenBuffer(Main.MainRef.cryptkey.buffer)`
- then `Gameplay.GetPlayerName(...)` (`Network.java:404-405,1355-1357`)

`Gameplay` is a **pure-Java class, no webdriver import** (`Gameplay.java:4`). It uses the key buffer
for player-name obfuscation / copy-protection, not media.

`Gameplay.SetScreenBuffer(byte[] key)` (`Gameplay.java:59-73`) pseudocode — builds 73 "screen"
strings from the key table:
```
m_screen = new String[73]
n = 0
for n2 in 0..72:
    len = range(0, key[n++ % key.length] - 65, 79) + 6     // record length, byte - 'A'
    buf = new byte[len]
    for n4 in 0..len-1:
        buf[n4] = key[n++ % key.length] & 0x7F              // strip high bit -> ASCII
    m_screen[n2] = new String(buf)
```
Other obfuscation constants in `Gameplay`: `flashTimeInMS = 43`; copyright string
`"J0Ugo7ru|_|_d@n|>`'ag87wi7w4l2-"` (`Gameplay.java:11`); XOR masks in `GravityFromMass`/name
codec, e.g. `charAt(13 % n2) ^ 0x14`, `^ 0x18`, `^ 0x13`, `^ 0x16`, all `& 0x7E`
(`Gameplay.java:233-236`), plus `c2 ^ flashTimeInMS ^ c3` chains (lines 33-42, 207-217).

This is an anti-cheat name scrambler keyed off `XORBlockEncryptData.wtxt`. It has **nothing to do
with the 0x1af8f2 marker or the .START/.BODY/.RG/.MX/.FU/.IA container** — those are parsed inside
the native runtime, never in Java.

## 4. "300" and the .RG/.MX/.FU/.IA chunks

- **Not in Java at all.** No `"300"` version constant, no chunk-tag parsing in source.
- From `strings cannonballs-setup.exe`, "300" is the **WLD3 format version** in the header line.
  Distinct headers embedded in the setup archive:
  - `WLD3 WildTangent 3D 200 Compressed and Patented` (older v200 assets)
  - `WLD3.wav WildTangent 3D 300 Compressed and Patented` (audio, v300)
  - `WLD3.cab WildTangent 3D 300 Compressed and Patented`
  - `WLD3.txt WildTangent 3D 300 Compressed and Patented`
  So the token after "3D" is a 3-digit version (200 / 300); the token before it (`.wav/.cab/.txt`)
  is the sub-container content type.
- `.START` (162x), `.BODY` (many), and `.RG` / `.MX` / `.FU` / `.IA` all appear **only inside the
  binary payloads** in `cannonballs-setup.exe`, never referenced by game code. They are internal
  WLD3 section tags parsed by the native `wt3d.dll`/`actorobject.dll` importer, not by Java.

## 5. Pure-Java decoder in the WebDriver API layer? NO.

The `com/wildtangent/dmmp/*` source that IS present is only the DMMP multiplayer/community stack
(rooms, lobby, auth, chat, network transport, object serialization codec). Its codec
(`com/wildtangent/dmmp/shared/codec/BasicObjectCodec.java`) does byte<->short/int packing for
**network DObjects**, not WLD3. No WLD3 decoder in this layer. The actual WebDriver media API
(`wildtangent.webdriver.*`) is not shipped as Java — it's the native plugin.

---

## Native entry points to reverse next (from the binaries in the dump)

The WebDriver runtime that decodes WLD3 is **not in this asset dump**; it installs separately. The
launcher and installer name the exact components:

- `CannonballsLaunch.exe` strings: `WT3D.WT`  and  `\webdriver\webdriver.dll`
  - `WT3D.WT` is the JNI/COM class the launcher binds (matches Java `wildtangent.webdriver.WT`,
    obtained via `wt3dLib.getWT()`).
- `WebDriverSilentInstall.exe` lists the WebDriver **3.3.1.001** file set that contains the decoder:
  - `webdriver.dll`  — JNI bridge (implements the native side of `wildtangent.webdriver.*`:
    `createAudioClip`, `createActor`, `readFile`, `WTAudioClip`, `WTActor`, `wt3dLib.getWT`).
  - `wt3d.dll`  — WildTangent 3D engine; the WLD3 container parser + decompressor (also `legacy\wt3d.dll`).
  - `actorobject.dll`  — actor/model object loader → decodes **.wsad**.
  - `jdriver.dll`  — Java driver glue.
  - `dx5drv.dll` / `dx7drv.dll`  — DirectX render + DirectSound audio back-ends (**.wwv** PCM output).
  - `legacy\data.wts`, `legacy\webdriver.dll` — legacy fallbacks.
  - App id `wtwebdriver`, updater `http://updaterservice.wildtangent.com`.
- Other DLLs in the dump are NOT the decoder: `wtgif.dll` (GIF), `wtgutils.dll` (utils),
  `params.dll` (launch params), `GCInstaller.dll`/`WebDriverSilentInstall.exe`/`cannonballs-setup.exe`
  (installers), `wtbgmtt.exe` (background/matchmaking helper). `game.jar` contains **zero** webdriver
  classes (`unzip -l | rg -ci webdriver` → 0).

To recover the WLD3 codec you must obtain and reverse **wt3d.dll + actorobject.dll + webdriver.dll**
from a WildTangent WebDriver 3.3.1.001 install. The XtoWT converter (`XtoWT.exe`, author of the
"Converted by XtoWT" line) is the encoder counterpart, also not in this dump.

## Bonus: where the WLD3 header/tag strings physically live in this dump

`cannonballs-setup.exe` embeds the raw WLD3 asset payloads (that's why `strings` on it shows the
full `WLD3.wav … 300 Compressed and Patented`, `Converted by XtoWT: <date>` for ~2000/2002 builds,
`.START`, `.BODY`, `.RG`, `.MX`, `.FU`, `.IA`). Good source of sample containers to test a
reimplemented decoder against, alongside `assets/120302/MEDIA/**/sound.wwv` and the
`XORBlockEncryptData.wtxt` key table.
