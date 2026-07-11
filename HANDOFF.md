# Cannonballs native macOS remake + format crack — HANDOFF / resume point

**Read this first.** It lets a fresh assistant pick up exactly where we are without
repeating finished work or re-walking dead ends. Everything referenced here is in
this repo folder unless it says "(ephemeral)".

## 2026-07-11: BOT TURN HANG (stuck on an NPC's turn)

- **Bug**: game freezes on a computer player's turn. Root cause: the macOS port
  solves a bot's target + firing solution once in `beginTurn`, so a turn that
  begins without a valid target (transient race) leaves `haveSolution == false`
  forever, `slew()` no-ops, nothing fires, and the turn never switches. The
  original `AIThink` re-acquires a target every frame (`Cannon.java:644-669`) and
  never strands.
- **Fix (fidelity-correct)**: `BotAI.think()` now mirrors the per-frame
  re-acquisition — drops an inactive target and re-solves when there is no usable
  solution. Aiming math and projectile/turn machinery were checked and are sound
  (not the cause). The original had **no** watchdog on bot turns.
- **Full writeup + the interpolation watchdog fallback** (ready if hangs persist,
  marked as interpolation since the original had none): `BOT_TURN_HANG.md`.

## 2026-07-11 (latest): WINDOWS HELP! PAGES WOULDN'T OPEN

- **Bug**: in the default single-file Windows exe, the Help! menu (Controls /
  How To Play / Tutorial) opened nothing. Root cause: `HelpViewer.present`
  (`windows/src/ui/help_viewer.gd`) builds a `file://` URL and calls
  `OS.shell_open`, but the host browser can only read REAL on-disk files. In
  the single-file build the HELP pages live inside the embedded `assets.pck`
  (`Assets.root == "res://Resources"`), a virtual path with no file on disk, so
  the URL pointed at nothing. Only the DEFAULT single-file build was affected;
  the fallback folder build and macOS (WKWebView) were fine.
- **Fix**: `HelpViewer` now materializes the whole `HELP/` tree (pages
  cross-link and pull local `images/`) to `user://help` (a real writable OS
  dir) once on first use, then opens from there; folder/dev builds still open
  in place. Verified headlessly against a real mounted `assets.pck`: all
  pages + 12 images copied byte-identically, and the emitted URL correctly
  percent-encodes the space and `!` in the userdata path.
- **Not yet repackaged**: the shipped `dist/Cannonballs-Windows.zip` / GitHub
  Release still has the old exe. Re-run `windows/tools/package_win.sh` and
  re-upload to push the fix to users.

## 2026-07-10: PACKAGED INSTALLERS + GITHUB RELEASE

- **macOS installer**: `macos/tools/make_dmg.sh` builds the release binary,
  stages a self-contained `Cannonballs!.app` (via `package_app.sh`), wraps it in
  a drag-to-install `dist/Cannonballs-macOS.dmg`, AND refreshes the two installed
  copies (`~/Documents/Cannonballs`, `/Applications`) from the same staged
  bundle so the playable local copy always matches the shipped .dmg.
- **Windows package**: `windows/tools/package_win.sh` (unchanged) exports
  `windows/dist/Cannonballs/`; zipped to `dist/Cannonballs-Windows.zip`.
- **Downloads live on GitHub Releases** (tag `v1.0`, marked latest). README has
  `releases/latest/download/...` links for both. Build artifacts (`dist/`,
  `windows/build/`, `windows/dist/`) are now **gitignored** — binaries ship via
  Releases, never committed.
- **History cleanup**: the auto-commit hook had committed a stray 110 MB
  `windows/build/Cannonballs.exe` (Godot's default export path) into 15 unpushed
  commits, which blocked every push (>100 MB GitHub limit). Purged it from those
  commits with `git filter-repo --path windows/build --invert-paths`; the 72
  already-pushed commits kept their SHAs (clean fast-forward). Don't let
  `windows/build/` get re-tracked.
- **Repo is PRIVATE** — the release download links only resolve for accounts with
  repo access. If public distribution is ever wanted, weigh `LEGAL.md` (Gamigo
  IP) before flipping visibility.

## 2026-07-10 (earlier): PLAYTEST ROUND 2 — fixes + unreproduced reports

- **Windows: power bar had a "chunk cut out."** The fill was drawn as two
  scaled `AtlasTexture` sprites (body u0.1..0.5 + a 12px leading cap u0.5..1.0,
  per Cannon.java:579-589), which left a 1-2px seam where the empty bar showed
  through. The decoded POWERBAR art is a uniform orange 24x24 (no distinct cap
  art), so the two tiles paint the same color — now drawn as ONE sprite of
  width `level*440`. Pixel-identical to the source's solid fill, no seam.
  Verified against the Mac reference (`hud_scene.gd:_process`).
- **Windows: Godot "debug" console window** — disabled the export console
  wrapper (`export_presets.cfg debug/export_console_wrapper=0`) so no extra
  grey console `.exe` ships alongside the game.
- **Both: results-screen "Done" hardened.** COULD NOT REPRODUCE the reported
  dead button in extensive faithful tests: Windows `--donetest` (real
  `push_input` through the GUI phase → game.click → hud.click → on_exit, with
  the REAL game_over state surfaced by the update loop) passes in the editor
  AND on the exported .exe under Wine; Mac `--gameovertest` + the uitest
  real-mouseDown both return to menu. As robustness (targets a plausible
  hit-area/coordinate mismatch on real hardware), the results screen now
  dismisses on a click ANYWHERE, plus Enter/Space/Esc
  (hud_scene.gd/game_controller.gd; HUDScene.swift). NOT a root-cause fix —
  if it persists, need the user's win/lose/forfeit path + window state.
- **Windows: "menu backdrop / grey instead of the level" on Moonlight/Old
  Gods** — COULD NOT REPRODUCE (8/8 faithful runs through the lobby preview +
  iris transition render the correct game camera). Defensive hardening: in
  `_start_game`, the game camera is now made current (`make_current()`) BEFORE
  the menu backdrop is freed — previously the backdrop was freed first, leaving
  an ordering window where its orbit camera (menu behind HUD) or no camera
  (grey) could own the viewport for a frame. Intermittent/environment-specific;
  this closes the theoretical window but was not reproduced locally.
- Diagnostic harness kept: `godot --path windows -- --donetest` (parallels the
  macOS `--gameovertest`).

## 2026-07-10 (earlier): PLAYTEST FIXES — both platforms

- **Windows: name-entry Enter (and every modal button) was dead to the MOUSE.**
  Root cause: the menu hit-tests all clicks manually in
  `MenuScene._unhandled_input`, but the modal `veil`/backing/iris nodes are
  `ColorRect`s (Controls) at default `mouse_filter = STOP`, so they consumed
  the click in Godot's GUI phase before `_unhandled_input` ran (keyboard still
  worked). The probes never caught it because they call `click()` directly,
  bypassing the GUI phase. Fixed by setting `MOUSE_FILTER_IGNORE` on every
  decorative Control: the two menu veils + iris arms (menu_scene.gd), the
  standalone iris arms (iris_transition.gd), the lobby preview backing
  (boot.gd — would have killed lobby clicks too), and the software-render
  display surface (scene_rasterizer.gd — would have blocked HUD menus while
  active). Verified with a push_input() probe that drives a real click through
  the GUI phase: name-entry → settings advances.
- **Windows: Godot boot splash disabled** — `application/boot_splash/show_image
  = false` + black bg in project.godot (the 2002 game launched straight in).
- **Windows: cursor is now the cutlass** — `boot.gd:_apply_cutlass_cursor()`
  sets HUDART/pointer.png (64x64, hotspot 2,2) via
  `Input.set_custom_mouse_cursor`, matching macOS KeyView.cutlass.
- **macOS: results-screen "Done" button** — investigated; the click handler
  and routing are correct and the render loop never stalls
  (`rendersContinuously` stays true), so queued clicks always drain. A new
  regression check drives a REAL view-level mouseDown on Done through the full
  mouseDown→overlay-convert→queue→drain path (main.swift --uitest tail, snapshot
  `uitest-results-done`) and it returns to the main menu; `--gameovertest` also
  asserts `game.click` on Done fires onExit. No production change was needed —
  the back-port's results-screen rework already resolved it; the app was
  repackaged so /Applications ships it.

## The goal
A pixel-for-pixel **native macOS clone** of WildTangent's 2002 game *Cannonballs!*,
built from the original decompiled game + original assets. The clone is playable;
the last frontier is importing the original 3D **meshes** (the `.wsgo` "geom" format).

**The Fidelity Doctrine (read `CLAUDE.md`, it's binding):** to-the-pixel faithful;
the entire game comprised of original assets rendered and placed by the original
source code whenever possible. `source/*.java` is the single source of truth for
every coordinate/size/color/UV/draw-order/formula — transcribe its constants
verbatim and cite `File.java:line`. Reference footage and screenshots are a
*guide* for verification and for pinning ambiguous engine conventions, never a
data source: no coordinates measured off screenshots, no color-picking, no
eyeballed sizes or animations.

## Where things live
- `shared/Resources/` — the platform-neutral decoded asset tree consumed by ALL
  builds (moved out of the mac target 2026-07-09; export tools write here).
- `macos/` — the Swift/SceneKit macOS clone (source only; run `swift build`;
  package with `macos/tools/package_app.sh`). Built app also lives at
  `~/Documents/Cannonballs/Cannonballs.app` and `/Applications/Cannonballs.app`.
- `windows/` — reserved for the Windows port (not started; see its README for
  what it consumes). Everything outside the build dirs is platform-neutral.
- `source/engine/native/dlls/` — **the recovered native decoder DLLs** (`actorobject.dll` etc.) + `PROVENANCE.md`. THE key artifacts.
- `format-research/` — every format finding: `WLD3_CODEC_CRACKED.md` (audio codec),
  `WSGO_GEOM_FORMAT_NOTES.md` (mesh format, incl. the full log of what failed),
  `external_research.md`, `java_decode_findings.md`, `GEOM_DECODE_SPEC.md` (mesh
  decode spec — being produced by disassembly; may be partial at commit time).
- `tools/` — working scripts (container decode, texture keying, heightmaps, disasm).
  `tools/notes-and-dead-ends/` — scripts from failed approaches, kept only so you
  recognize and avoid them.
- `decoded-assets/` — decoded original audio (29 SFX+music WAVs) and masters.
- (ephemeral) `/tmp/cb/` — the live scratch workspace: `WTExtractor/` (patched),
  `assets/120302/MEDIA/` (all extracted game assets incl. every `.wsgo`), `wtvenv/`
  (python venv with capstone/pefile/numpy/PIL), `xpdlls/`, `genesis3d/`. If `/tmp` is
  wiped, re-extract assets from `~/Documents/Cannonballs/cannonballs-setup.exe` with
  `tools/name_blocks.py`, and re-clone `github.com/diamondman/WTExtractor` + apply the
  `tools/wtextractor-v200-patch/wtextract.py`.
- (ephemeral, 10GB) `~/VMs/cannonballs-86box/xp.img` — the XP disk the DLLs came from.

## DONE and working (do not redo)
1. **Installer + assets fully extracted.** 2002 NSIS unpacked by hand (`tools/name_blocks.py`); 511 blocks named; all models/textures/audio/maps organized under `/tmp/cb/assets/120302/MEDIA`.
2. **Gameplay spec** reverse-engineered from decompiled Java (physics constants, weapons, economy, turn machine, bot AI). In the clone's code + `format-research`.
3. **Audio codec CRACKED and shipped.** `.wwv` = WLD3 container → per-file rolling-XOR (key hashed from metadata) → CAB/MSZIP → RIFF/WAVE. NOT a proprietary codec. All 29 sounds decoded and playing in the clone. Tool: `tools/wwv2wav.py` + the WTExtractor container decoder. See `WLD3_CODEC_CRACKED.md`.
4. **WTExtractor v200 patch upstreamed** — PR: github.com/diamondman/WTExtractor/pull/3 (adds older v200 header support).
5. **Textures decoded** (`.wsbm` → JPEG/PNG) and **props re-skinned + re-proportioned** with the real textures (palms=bark trunk+frond billboards, tikis, chest, etc.). Shipped in the clone.
6. **macOS clone playable**: 11 original islands (real heightmaps/textures/objects.dat), spec-exact physics, 12 weapons, 4 bot AIs, HUD, menus, original music, original SFX, camera modes, macOS menu bar with clickable controls, original app icon (the gold skull coin). Threading bug (SpriteKit HUD race) fixed via render-thread command queue; turn-freeze fixed via `rendersContinuously`.
7. **Game-over flow (victory + defeat) transcribed from source** (2026-07-09).
   Defeat: persistent "You Lose!" + 5 s wait (Packet_Manager.java:262-268) →
   spectator mode: camera view 99, "Spectator Mode"/name labels, power-bar frame
   pulled, Camera menu removed, Q leaves (Cannon.java:1480-1497, Camera.java:278-290,
   HUD.java:94-113/690-698). Match decided: success camera orbit (view 6,
   (0,10,-60) at 10°/s, Camera.java:292-342) + "You Win!"/"<name> Wins!"
   (Packet_Manager.java:416-448), 15 s destroy state (Game_Loop.java:97-106) →
   results screen. Forfeit exits via the destroy state with 5 s pre-elapsed, no
   spectating (Packet_Manager.java:242-253). Regression: `--gameovertest`
   (headless assert) and `--gameovershots` (4 PNGs); uitest results snap moved
   to the 15 s mark.

## KNOWN DEAD ENDS — do NOT repeat these
- **"XOR key" for the container** — red herring. It's per-file rolling XOR + DEFLATE; `XORBlockEncryptData.wtxt` is unrelated (player-name copy-protection).
- **Mesh `.wsgo` "geom" decode by staring at sample bytes / brute-forcing layouts.** Tried exhaustively across FOUR sessions: per-component float-header model (overfit to flat models, WRONG), 8/16-bit low/high-byte, signed-centered, bitfield sweeps, quad-planarity, mirror-symmetry, point-cloud shape scoring. All produce correct value RANGES but garbage assembly, and the arbiters can't distinguish correct from plausible-wrong without ground truth. **Do not resume this approach.** Scripts quarantined in `tools/notes-and-dead-ends/`.
- **"The vertices are spatially reordered and irreversibly lost"** — this was a WRONG conclusion. Disassembly shows `BuildVipmsFromGeoChunkIdx`: the VIPM progressive-mesh reorder happens in memory at load; the FILE stores plain triangle geometry.
- **Getting the 3D codec DLL from WebDriver 4.1 base installer** — it only ships image codecs (`wtImage*`). The 3D codec was an on-demand download (dead servers). It is NOT in the game files either. It IS on the XP image (see below).
- **Hosting the decode by re-implementing WT's COM/"Chimera" serializer framework** — heavier than just disassembling `ReadData`.

## THE MESH FORMAT IS SOLVED ✅ (2026-07-07)
The `.wsgo` "geom"/VIPM mesh format is **cracked** — arrow, cannon, chest, torch all
decode into clean coherent meshes. Full spec: `format-research/GEOM_MESH_FORMAT_SOLVED.md`;
working decoder: `tools/wsgo_decode_SOLVED.py`; proof: `format-research/CRACKED_models.png`.

**The key (what broke 5 sessions):** 16-bit quantized values are stored **byte-planar**
(all high bytes, then all low bytes — NOT interleaved int16). `value = min +
((hi<<8)|lo)/65535·(max-min)`. Per-component block = `[f32 min][f32 max][N hi][N lo]`.
Render vert count = u32 at ViPm+8; positions at ViPm+132 (3 SoA blocks); faces = int16 tris.

**Remaining (mechanical, no more RE):** extend the decoder to also read normals + UVs
(they follow positions in the same packed `ReadPackedLVertArray2` layout: pos vec3,
normal vec3, then u/v floats at LVert +0x20/+0x24), export every model to OBJ+MTL+
textures + `model.json`, drop into `macos/Sources/Cannonballs/Resources/MODELS/`,
rebuild both `.app` bundles, snapshot to confirm real meshes render.

## ACTOR MOTION + SKINNING ALSO SOLVED ✅ (2026-07-09)
The `.wsmo` motion format, the geom skeleton (bone hierarchy + inverse-bind
matrices), and per-vertex matrix-palette skin weights are all cracked and playing
in the clone: chest hops AND rattles its lid, the cannon barrel does its original
recoil squash per shot, the Moonlight lighthouse beam sweeps. Specs:
`format-research/MOTION_FORMAT.md` + the skeleton/skin section of
`GEOM_MESH_FORMAT_SOLVED.md`. Tools: `tools/wsmo_decode.py`,
`tools/wsgo_export_skinned.py`. Swift: `SkinnedModel.swift` (SCNSkinner).
Self-verify: `swift run Cannonballs --skinned CHEST 0.63 out.png`.
The full ViPm vertex layout (incl. per-vertex COLOR — real data on HUT/
LIGHTHOUSE/TAILS etc., constant white elsewhere) is now byte-exact; the old
OBJ exporter's "unknown extra array" mystery is resolved.

--- (historical record of how we got here, below) ---

**Breakthrough that led here:** the native decoder `source/engine/native/dlls/actorobject.dll` was
extracted from the XP image and **retains C++ symbols**. It's a WildTangent fork of
**Genesis3D** (open source — cloned at `/tmp/cb/genesis3d`, `RealityFactory/Genesis3D`).

**The "geom" container header is FULLY resolved** (see `format-research/GEOM_DECODE_SPEC.md`
+ `readdata.txt`): magic `"geom"`, version (≤4), chunk count A, material-name count B,
material count C, A×64B bone matrices, B named materials, A chunk headers, C materials,
then a per-material **VIPM build loop**.

**The actual vertex/face/uv/normal data** for each material is read by a single call:
`@VIPM_CreateFromFile@4` — which is **NOT in actorobject.dll**. It lives in
**`source/engine/native/dlls/WDENGINE.dll`** (708KB, recovered from the XP image, has symbols).

**THE FINAL STEP (all artifacts now in hand — just disassemble + reimplement):**
1. Disassemble `@VIPM_CreateFromFile@4` at **RVA 0x6db55** in `WDENGINE.dll`
   (ImageBase **0x66200000**). Use `/tmp/cb/wtvenv/bin/python` (capstone+pefile) or `tools/wtdis.py`.
2. It reads the mesh via **`@geVFileUtil_ReadFloatArray16@24` / `_2@16` / `_LZ@24` /
   `_LZ2@16`** — WildTangent's **16-bit quantized float array** readers (`_LZ*` =
   LZ-compressed). Disassemble those too for the exact dequant (bit width, min/max
   scaling, and the LZ scheme). This is where the real vertex/normal/uv layout lives.
   Cross-check element order with the getters `@VIPM_GetTriList@12`,
   `@VIPM_GetVertexBuffer@4`, `@VIPM_GetNormals@4`.
3. Reimplement in Python **from the disassembly** → decode each `.wsgo` (container-decode
   first via WTExtractor) → export OBJ+MTL+textures + `model.json` → drop into
   `macos/Sources/Cannonballs/Resources/MODELS/<NAME>/` (the `ModelLibrary.swift`
   importer already reads that schema) → `swift build` → rebuild both `.app` bundles.
4. Validate by rendering a model FILLED (torch/cannon must look like a torch/cannon,
   not spiky noise). Read your own PNGs.

**DO NOT** go back to guessing byte layouts from sample `.wsgo` data — the answer is
fully determined by `VIPM_CreateFromFile` + the `ReadFloatArray16*` primitives, which
are all present in `source/engine/native/dlls/WDENGINE.dll` with symbols. Reference:
Genesis3D `Actor/body.c` (conceptual only; VIPM was WT's proprietary addition).

## 2026-07-09 (latest): REFERENCES ACQUIRED, RETRO PLAN FULLY EXECUTED ✅
Original reference material now lives in format-research/originals/ (60 frames
from the original gameplay video + the 800x600 screenshot). It drove: lobby
corrections (Add AI Player dropdowns ~176px, preview over the lobby ART, panel
width), two-tone bot chat, v1.869 version string. The playtest retro plan is
complete: model audit re-export (10 props + SHIP/MAST/TAILS + all 11 DEBRIS
pieces through the solved pipeline; debris tumbles on prop destruction per
Chunk_Object.java), widget-complete UI transcription for every screen
(UI_TRANSCRIPTION.md), evidence-level annotations (V)/(S) in the matrix, and
an honest approximations table. Preview crash fixed (scene swaps on main,
retint-only color changes). Bot names roll randomly from the roster at
assignment like Network.java:204.

## 2026-07-09 (later): FIDELITY MATRIX FULLY BURNED DOWN ✅
Every row in FIDELITY_MATRIX.md is now ✅ or a justified n/a. The final sweep
added: ALL remaining particle classes (CHUNKS debris, STAR, Sparkle, Ray,
Shockwave, SmokeColumn, FireTrail embers, the full teleport composition, real
30-puff SMOKEPUFF clouds), GRIT terrain detail layer, SHADOW blob patches (and
shadow-mapping OFF - the 2002 engine had none), decorations terrain-follow,
the REAL decoded ARROW weathervane mesh, SPLAT purple bake + quake rays for
crater weapons, all five SKY actors as camera-following skyboxes WITH the
horizon-island billboards/moon/stars, lens-flare terrain occlusion, cannon
REFLECTION env layer, the bot two-phase LOS trick + exact weapon cascade, and
the dumbfire smoke+fireball trail. WT logo intro = n/a by choice (third-party
branding). New exporters: tools/export_skies.py; extended export_fx_sprites.py.

## 2026-07-09 BURN-DOWN: sprites, dressing, small items, deep cuts ALL LANDED
Beyond motion/skinning, this session shipped: original sprite-sheet particles
(EXPLOSION1/SMOKEPUFF/COIN/SPLASH/SPLASHRING via FXSprites.swift with decompiled
physics), world dressing (32-frame WATERANIMATION cycle, shoreline shimmer
planes, wind-driven CLOUDSHADOW shader layer, SCORCH crater bake, 6-element lens
flare), the iris-wipe screen transition (decoded MENUS/TRANSITION disc), verbatim
bot chat tables (BotChat.swift), line-level bot AI fixes, LIGHTBEAM prop fixed to
its animated additive build, and rasterizer hardening (skips add-blend). THUMP is
confirmed dead code in the original; HOURGLASS is load-wait-only (n/a). Chest
scale fixed to native (the x7 was a long-standing bug). FIDELITY_MATRIX.md is
current — remaining ~/✗ rows are the queue (CHUNKS/SHOCKWAVE/SPARKLE/STAR/RAY
sprites, SPLAT paint, horizon-island billboards, blob shadows, GRIT layer,
LOS two-phase bot trick, WT logo intro).

## 2026-07-09 (evening): OVERLAY ALPHA BUG ROOT-CAUSED + game-over + real minimap
Three playtest complaints fixed:
1. **"Gap" between skull medallion and angle bar** was NOT a layout bug (clone
   coordinates were already pixel-identical to HUD.java) — SceneKit's
   overlaySKScene sRGB-DECODES the texture alpha channel, which erased the bars'
   translucent black interiors (alpha 148-238 rendered near-invisible), so the
   pitch/power bars drew as hollow gold frames. Measured model:
   out = enc(dec(dst)·(1−dec(a))). Fix: HUDArt.overlayAlphaCompensate stores
   a' = enc(1−(1−a)^2.2) at sheet load; verified against an SKView reference
   render within ~3/255. Gotchas hit: must take the CGImage from the bitmap REP
   (cgImage(forProposedRect:) returns 2x on retina) and wrap output in an
   explicit NSBitmapImageRep (NSImage(cgImage:) misreports pixelsWide as 2x).
2. **Game over**: forfeit now force-eliminates (Cannon.java:1099 bl flag); local
   elimination mid-game shows the big "You Lose!" banner (Packet_Manager.java:262);
   win banner is "You Win!" when the local player survives (Packet_Manager.java:432).
   uitest now exercises forfeit → banner → auto results.
3. **Minimap**: island is now the original rust-brown mottled stain (Island.java:262
   verbatim: (139,55,24)±20 noise, (20,20,20) shore ring, elevation-faded alpha,
   sourceAtop so the torn parchment edge masks it), 15px MAPBITS X sprites
   (red=cannons, gold=chests) replace bezier X's, arrow at native 15x36, world→map
   mapping fixed to the Java (16+x/vs, 16+z/vs) with correct V flip (was mirrored,
   19px inset, green ground-texture underlay). Orientation verified against
   original video frame_008 (Tropicali stain lobes match).

## 2026-07-09 (night): PLAYTEST BATCH 2 — chat, menus, barrels, controls
Eight-item playtest round, all landed:
1. **TNT barrel** re-exported through the solved pipeline (it had slipped the
   model audit and still used the corrupted heuristic OBJ). Added to
   tools/export_props_solved.py SPECS; skinned.json now wins in Props.node().
   Scaled bbox height = 9.0 exactly (prop.dat).
2. **Chat wrap** — Text.java wordWrap implemented pixel-accurate (charWidths
   * 0.75 advance, 250px column, char-split for over-wide words).
3. **Chat typing** — 'C' opens the entry line ("SAY : " + blinking 0x7F block
   cursor at 14,587, Chat.java semantics: Enter posts, backspace, 750px cap;
   Esc cancels; controls disabled while chatting). Posts as blue name + white
   text like bot lines.
4. **Fade-up game messages** — HUD.java queue verbatim: spawn (400,300), rise
   20 px/s, linear fade over 100 px, 6 in flight, next spawns when all have
   risen ≥24px, 18-deep queue. flashMessage() no longer writes to the chat.
5. **Help! menu** — Controls / How To Play / Tutorial per HUD.java:1071,
   opening the ORIGINAL HTML pages (rescued from /tmp/cb into
   Resources/HELP/, 25 files incl. tutorial1-5 + screenshots) in the browser
   (Main.launchHelpPage equivalent).
6. **Options menu** — Shadows : / Sound : / Music : per Main.getSettings, with
   the original rules (Music row hidden while Sound off; Sound off forces
   Music off). Audio got didSet observers (the old toggles never touched the
   playing music player — that's why they "did nothing"). Shadows toggles all
   "blob-shadow" nodes + gates creation (FXSprites.shadowsEnabled). All three
   persist in UserDefaults (opt.shadows/opt.sound/opt.music).
7. **Left/right aim inverted** — fixed in Cannon.spinInput (left = +spin =
   CCW = screen-left; bots unaffected, they write spinAngle directly).
8. uitest extended: wrapped chat line, live entry line with fabricated key
   events, Options + Help dropdown snaps, forfeit → banner → results flow.

## 2026-07-09 (late night): X-Shot + horizon-cloud clipping
1. **X-Shot** now flies nose-first (it shares Dumbfire's mortar-shell orientation
   from the normalized trajectory — the clone only oriented Dumbfire, so the
   shell tumbled in its authored pose) and sputters the original 30% smoke /
   40% mini-fireball trail (updateXShot:70-79). Impact X-carve was already
   faithful. Difference between the two (for the record): X-Shot is ballistic
   (gravity+wind) and carves the ±45° X grooves + line kills; Dumbfire is a
   NO-GRAVITY straight rocket with a 7 s fuse and a plain crater.
2. **Horizon clipping ROOT-CAUSED**: the sky-actor island billboards used an
   alpha-test discard shader modifier; SceneKit routes such materials through a
   depth prepass that writes the FULL quad rect (ignoring writesToDepthBuffer=
   false), so the map's SMOKEPUFF cloud decorations behind them were clipped
   into hard-edged rectangles ("islands cut off at the top"). Fix: plain .aOne
   alpha blend on sky billboard parts (SkyActor.swift) — matches the 2002
   alpha-blended rendering. Bisected with the new CB_HIDE=<node,names> env var
   on --snapshot, which also grew real --cam x,y,z / --look x,y,z flags.

## 2026-07-09 (latest): REPO PREPPED FOR THE WINDOWS BUILD
- **`shared/Resources/` is the asset tree now** (moved from
  macos/Sources/Cannonballs/Resources — platform-neutral, consumed by all
  builds). The export tools (export_props_solved / export_fx_sprites /
  export_skies / gen_sfx / key_textures) write there.
- **SPM no longer bundles resources** (it copies directory symlinks as broken
  links, so the symlink trick failed). Assets.swift resolves the tree at
  runtime: packaged .app → Contents/Resources/Resources; dev `swift run` →
  walks up to shared/Resources (bonus: dev builds read assets live, no rebuild
  after asset edits). `macos/tools/package_app.sh` assembles the .app (binary +
  rsync of shared/Resources + icns + codesign) — the old
  CannonballsMac_Cannonballs.bundle is gone.
- **`windows/` reserved** with a README mapping what a port consumes and the
  macOS-learned rendering gotchas. Repo docs (README/PIPELINE/ROADMAP) updated
  to describe the two-build layout; monorepo decision recorded in ROADMAP.
- `macos/Cannonballs.icns` moved out of the shared tree (mac packaging asset);
  gen_sfx.py consolidated to tools/ (duplicate removed);
  empty macos/Resources_original_audio removed.

## 2026-07-09 (later night): PLAYTEST BATCH 3 — native scale, foliage, collision
Nine-item playtest round, all landed:
1. **Props render at NATIVE scale, period.** Prop.java:388 uses the 1-arg
   Media_Object_Actor ctor (finalscale -1 → setAbsoluteScale never runs); the
   scaled 4-arg ctor is dead code game-wide. The old model.json "scale"
   (prop.dat HEIGHT ÷ corrupted-legacy-OBJ height) was fabricated and is now
   dead: LIGHTHOUSE was 5.69x oversize, SHIP 0.32x (tiny), BRIDGE 2.03x,
   LIGHTBEAM's hard-coded 0.835 gone, debris offsets unscaled. prop.dat
   RADIUS/HEIGHT are collision-cylinder data only (Prop.java:308).
2. **Five props re-exported through the solved pipeline** (they still used the
   corrupted heuristic OBJs — TNT's old failure mode): OBELISK (Pyramids),
   MOUNDBEAM (the "binoculars" warp on Old Gods + Ziggurat), FIREHEAD
   (Crossbones), HUT, TORCH. DEBRIS texture dirs that were empty are filled.
3. **Decorations are real actors now** (Particle_Object_Decoration.java:24):
   BRUSH2 purple fern mesh, PALM2/brush1 = the GREEN bush (was entirely
   missing — every ground decoration collapsed into the purple billboard),
   TAILS tail1/2/3 cattails. Scale per :26 setAbsoluteScale(sx, sy, sx);
   water decorations pin Y=0.0 (:50). Purple IS the original color (ferno
   texture; frames 026/033 confirm).
4. **Projectiles collide with WTCOLLIDEABLE prop MESHES** (Weapon.java:1029-
   1040, the sweep that runs before every proximity test): the Nightbridge
   deck now blocks shots. Standable decks (code 5) per weapon: cannonball/
   X-Shot/Dumbfire burst (Explosion1 + 4 smoke + 3 embers, no crater), Bouncer
   bounces on the deck via the mask-8 ray, Spike Roller rolls across it, TNT
   plants on the terrain below (packet 21 + ONGROUND), crater balls splat 20
   chunks and keep flying, teleport fires on deck contact. Non-standable
   collideables (code 4) hide the shot + per-weapon splat/burst. The
   destructible-prop proximity cylinder now requires Destructible
   (Prop.java:304). Molehill's raise-suppression is the mask-8 standable
   probe (Weapon.java:590), not any-prop proximity.
5. **prop.dat <FIRE> implemented** (Prop.java:501-512, :628): persistent fire
   SmokeColumn at the prop-local offset, dies with the prop — TORCH/
   TORCHBEARER/FIREHEAD keep their flames now that the real meshes win.
6. **Procedural sun/moon spheres deleted** (World.swift): the original's only
   celestial visuals are the sky actor's authored parts (NIGHT `moon`
   billboard) + the lens flare on HasSun maps. The old world-anchored moon
   ball parallaxed across the camera-following horizon islands and painted
   over them (the reported Moonlight bug).
7. **Sky actor**: follows the camera's FULL position incl. Y (Camera.java:
   583-586), and parts layer by authored wsgo order (renderingOrder -300+i)
   instead of a shared order.
8. **Help! renders in-app** (HelpViewer.swift, WKWebView over the game view,
   800px column, Esc/click-out closes; file links stay in-panel, http goes to
   the browser). The original called the host page's JS
   (Main.java:248 launchHelpPage) — game and help shared one window in 2002.
9. Verified: --snapshot renders of Moonlight/Old Gods/Ziggurat/Pyramids/
   Crossbones/Overboard/Nightbridge/Tropicali + FIREHEAD/OBELISK/MOUND
   close-ups; full --uitest pass; release app repackaged.
   **PROBE RESOLVED (same session): the objects.dat z convention.**
   objects.dat coordinates are heightmap-ROW space for EVERY tag.
   Island.java:879-927 flips them ALL (Height - z) into world space — PROP,
   PROPPOS, DECORATION, DECORATIONWATER, CLOUD, FIREFLY alike — and
   getTerrainHeight flips world z straight back into row space
   (f2 = Width*VertexScale - f2), so the two flips cancel: file z IS the
   heightmap row. The clone keeps terrain and objects in file-row space with
   NO flips, which is equivalent; its old <FIREFLY> (grid - z) was a double
   flip mirroring fireflies across the map — removed. Also: VOLCANO's
   objects.dat <FIRE> lines are dead data (no <FIRE> handler in Island.java's
   objects.dat parser; only Prop.java:501 reads <FIRE>, from prop.dat) — the
   clone's invented flame handler for them is deleted.
   **Known residual from the probe:** the clone's world is a z-MIRROR of the
   original's internal world coordinates (self-consistent, invisible for
   object placement). World-anchored externals could betray it: the maplist
   sun vector (lens-flare bearing) and any world-fixed sky yaw are used
   unmirrored, so the sun's bearing relative to island features may be
   z-mirrored vs the original. To check: find a reference frame with the
   lens flare AND a recognizable island silhouette and compare bearings. If
   confirmed, EITHER mirror the sun vector's z at load (small fix) OR move
   the whole world to Java coordinates (flip terrain rows + transcribe all
   placements verbatim; then the minimap's tuned V flip likely comes OFF —
   re-validate against frame_008).

## 2026-07-09 (post-midnight): MOUNDBEAM RENDERS AS LIGHT — gMat render style solved
MOUNDBEAM (Old Gods + Ziggurat) was falling through to the generic lit-opaque
skinned path and rendering like a structure. Root cause found in the original
data: the `.wsad` actor container carries a per-material `taMg` ("gMat") chunk
with a **render style** word — LIGHTBEAM and MOUND/beam both carry style **2**
(and beam.wsad adds emissive 255,255,255), every opaque structure checked
(mound, PALM2, TORCH) carries 0. LIGHTBEAM's indisputable visual outcome pins
style 2 = additive light. Full chunk layout + observed table in
`format-research/GEOM_MESH_FORMAT_SOLVED.md`. Clone-side: Props.swift now
routes MOUNDBEAM through the same additive treatment as LIGHTBEAM (shared
`applyBeamLightMaterials`); the software rasterizer already skips add-blend
materials so it stays consistent. The mesh is the mound's TWIN EYE-BEAMS
(the old "binoculars" export), bright green at the eyes fading to black —
additive turns the black tail fully transparent. Verified with
`--snapshot "Old Gods"` front/back + Ziggurat shots
(`macos/snapshots/uitest-moundbeam-*.png`): eyes blaze green from the front,
plain stone mound from behind. `moundillum` (mound's second material,
style 5) remains unpinned — likely the glow overlay on the stone eyes/runes;
worth a look if the mound face seems too flat vs footage.

## 2026-07-10: WINDOWS PORT LANDED — full Godot 4 clone, Wine-verified

The Windows build went from "reserved folder" to a complete feature-matched
port in one day. **Godot 4.7 + GDScript + `gl_compatibility`** (decision
rationale + the full SceneKit→Godot convention map: `windows/PORTING.md`).
Every Swift file ported file-for-file with the Java citations carried
verbatim; `windows/README.md` has the file map.

- **Everything runs**: menu flow → lobby (spinning tinted cannon preview) →
  iris → full game (terrain deformation, all 12 weapons' Java resolution
  ladders, 4 bot AIs, chest, props/collision, FX, audio) → game-over
  (forfeit/spectator/success orbit) → results → menu. The `--uitest` harness
  ports the macOS sequence 1:1 and writes 17 snapshots
  (`windows/snapshots/`), visually diffed against `macos/snapshots/`.
- **Software rasterizer ported** (soft_raster/scene_rasterizer/raster_demo;
  ~1.4 s/frame in GDScript — easter-egg cadence, algorithm untouched).
- **The exported .exe runs the full game under Wine Staging**, completing the
  entire uitest sequence (17/17 snapshots, zero script errors). Package with
  `windows/tools/package_win.sh` (headless export + real Resources copy into
  `windows/dist/`).
- Dev loop: `godot --path windows` to run; `-- --uitest` full sequence;
  `-- --uitest-terrain --map=N` terrain-only; parse gate
  `godot --headless --path windows --script tools/check_parse.gd`;
  menu-only probe `tools/menu_probe.gd`.
- **JAVA-FIDELITY DIVIDEND (back-port to macOS pending, task tracked): the
  port audit found ~40 places the Swift deviates from the decompiled Java.**
  Biggest: Projectile hit-code ladder (Weapon.java:1026-1095 — chest/prop
  kills gated on PROJECTILEIMPACT, crater weapons detonate on proximity,
  supercrater has NO explosion), chunk sheet indexing transposed (dirt
  debris invisible on macOS!), bot tilt clamp mirrored, shoreline/firefly/
  water-scroll parameters. Grep `"Java outranks"` / `"Java wins"` in
  windows/src to enumerate all.
- Known cosmetic follow-ups: power-bar body/cap ~2px seam (atlas edge
  bleed), Your Turn banner visibility in the game-over frame (compare
  macos/snapshots/gameover-lose.png), renderer teardown warnings at quit
  (leaked GLES instances — free order in boot teardown), REFLECTION env-map
  layer + per-material ambient knob unported (need a ShaderMaterial).

## 2026-07-10 (later): JAVA-FIDELITY BACK-PORT TO macOS — DONE

All ~57 corrections from the Windows-port audit are now in the Swift too
(FXSprites/Particles: 8; Props/WorldDressing/World: 7; HUDScene: 12;
Projectile/Cannon/BotAI: 30, incl. the full Weapon.java:1026-1095 hit-code
ladder restructure and shooter attribution plumbed through GameController
for the bot-retaliation rule). Every fix was independently re-verified
against the cited source/*.java before applying; deliberate clone-side
retentions (roller 25 s failsafe, pivot offsets) were kept. Verified with
swift build + full --uitest (17/17) + --gameovertest + Moonlight firefly/
shoreline snapshots; release rebuilt and /Applications repackaged.

Two candidates flagged during the pass, NOT yet applied on either platform:
- Water bob: Island.java:332 bobs `0.5 + SinTable[3]*0.5` (0..1 at 1.0/s);
  both clones keep a 0.05→0.55 easeInEaseOut 2.6 s approximation.
- TurnArrow (HUD.java:284-288, :1112): the bobbing pointer beside the turn
  list exists in the Java and the Godot port but was never built in the
  Swift HUD.

## How to build/run the clone
```
cd macos && swift build && swift run          # windowed app (assets read live
                                              # from ../shared/Resources)
swift run Cannonballs --snapshot "Skull Isle" out.png  # offscreen render
./tools/package_app.sh                        # assemble/refresh Cannonballs.app

godot --path windows                          # the Windows-port clone, native
windows/tools/package_win.sh                  # export + package windows/dist/
```
Arrow keys aim, Space charges/fires, +/- weapons, V camera, C chat, Esc menu.
