# Cannonballs! Resurrection — project instructions

Read `HANDOFF.md` for the current state of the work. The rules below override
convenience every time they conflict with it.

## The Fidelity Doctrine (non-negotiable)

This project is a **to-the-pixel faithful resurrection** of WildTangent's 2002
*Cannonballs!* (official title includes the `!`). The entire game should be
comprised of its **original assets**, rendered and placed by its **original
source code** whenever possible.

1. **The decompiled source (`source/*.java`) is the single source of truth** for
   every coordinate, size, color, UV rect, draw order, timing, formula, and
   behavior. To implement or fix anything, first find its data in the source and
   transcribe it verbatim (keep the original constants in the code, convert
   units in one visible place, and cite `File.java:line` in a comment).
2. **Reference material is a guide, never a data source.** The original video
   frames (`format-research/originals/`), website screenshots, and archived
   footage exist to *verify* results and *disambiguate* engine conventions —
   nothing else. Never hard-code a coordinate measured off a screenshot, never
   color-pick from an image, never eyeball a size, position, animation, or
   color. If a value can't be found in the source, say so explicitly and mark
   the interpolation in a comment rather than passing it off as original.
3. **Original assets only.** Never redraw, restyle, stretch, or substitute art.
   Use the decoded originals (`shared/Resources/`, from `raw/game-media/`);
   mechanical format conversions (e.g. image+alpha pairs merged to RGBA PNG)
   are fine, creative edits are not.
4. **Pin ambiguous engine semantics with internal probes, not taste.** The WT
   runtime is native and lost; when an API's convention is unknown (rotation
   signs, anchor points, coordinate handedness), find a usage in the source
   whose correct visual outcome is beyond doubt and solve for the convention.
   Worked examples: the cannon barrel pins `setOrientation(0,1,0,θ)` for
   meshes (rest facing -Z, π yaw compensation, `Cannon.buildParts`); the
   minimap arrow pins `setBitmapOrientation(t)` ≡ SpriteKit `-t`
   (`HUDScene.swift:517`). Record each newly solved convention in a comment at
   the use site.
5. **Sentinels, hidden states, and edge behavior count as fidelity too.** The
   original's `-1000` "never fired" sentinels, show/hide lifecycles, and
   draw-order option numbers are part of the game. Port them, don't
   approximate them.

## Practical notes

- WT HUD screen units: 0.002736 per pixel, origin at screen center, +y up
  (see `HUDScene.wt()`); positions are quad centers.
- Verify visual work with `swift build` + `./.build/debug/Cannonballs --uitest`
  (writes `macos/snapshots/uitest-*.png`), then compare against
  `format-research/originals/video_frames/` — as a check, not a ruler.
- The app users launch is packaged by `macos/tools/package_app.sh` from the
  **release** build (`swift build -c release`); `swift build` alone does not
  update `/Applications`.
