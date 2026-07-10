# Cannonballs — Windows build (in progress)

The native Windows port of the Cannonballs! clone: **Godot 4.7, GDScript,
`gl_compatibility` renderer** — see `PORTING.md` for the decision rationale,
the SceneKit→Godot convention map, and build/test commands. The macOS build
(`../macos/`, Swift + SceneKit) is the working reference implementation; this
build matches it feature-for-feature against the same ground truth, ported
file-for-file from the Swift (which carries the Java citations).

Status (2026-07-10): **the port is complete and playable.** All modules are
ported (see the file map in `PORTING.md`), the full `--uitest` sequence
(menu → lobby → game → game-over → results, 17 snapshots) passes natively
AND from the exported .exe under Wine Staging with zero script errors, and
snapshots are visually diffed against `macos/snapshots/`. Package a
distributable folder with `tools/package_win.sh`. Known cosmetic follow-ups
and the macOS Java-fidelity back-port list are in `../HANDOFF.md` (2026-07-10
entry).

## What a Windows build consumes (all already in this repo)

- **`../shared/Resources/`** — the complete platform-neutral asset tree, decoded
  from the 2002 originals. PNG/JPEG textures and sprite sheets, `skinned.json` /
  `model.json` meshes (see `format-research/GEOM_MESH_FORMAT_SOLVED.md` for the
  schema), decoded WAV SFX + MP3 music, heightmaps + `objects.dat` per map,
  bitmap font sheets, HTML help pages. The macOS build reads this exact tree;
  read it in place, don't copy-diverge it.
- **`../source/`** — the decompiled Java of the original game. This is the spec:
  every gameplay constant, HUD coordinate, particle behavior, and bot decision
  comes from here. When in doubt, this wins over the macOS build.
- **`../format-research/`** — cracked format specs, the fidelity matrix
  (`FIDELITY_MATRIX.md` — the living checklist of original-behavior rows and
  honest approximations), UI transcription, and reference video frames.
- **`../tools/`** — the Python decode/export pipeline that produced
  `shared/Resources` from the original installer (rarely needed unless
  re-exporting assets).

## Porting notes (learned the hard way on macOS — read HANDOFF.md)

- All UI is positioned in the original's 800x600 screen space with original
  sprite sheets; never redraw or stretch art (see `UI_TRANSCRIPTION.md`).
- The 2002 engine alpha-blended in sRGB space with no depth-written sprites;
  modern linear-space pipelines need care (see the overlay-alpha and
  alpha-test-depth-prepass entries in `HANDOFF.md`, 2026-07-09).
- Physics/economy/turn machine constants are documented in the spec files and
  implemented in `../macos/Sources/Cannonballs/` — the Swift is readable and
  line-commented with Java references (file:line), so it doubles as a porting
  guide.

## Engine choice

Undecided. Candidate approaches: native D3D/C++, Godot, or MonoGame. Whatever
is chosen must handle: heightfield terrain with live deformation + texture
splats, billboarded sprite particles, skinned skeletal meshes (matrix palette,
from `skinned.json`), an 800x600-space 2D HUD layer, and simple positional
audio.
