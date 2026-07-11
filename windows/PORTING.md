# Windows port — engine decision + porting conventions

## Engine: Godot 4.7, GDScript, `gl_compatibility` renderer

Decided 2026-07-10. Rationale against the README's candidates:

- **Native D3D/C++**: cannot be compiled or run on the development Mac; every
  iteration would be blind. Rejected.
- **MonoGame**: runs on macOS for testing and cross-publishes, but custom
  shaders (needed for matrix-palette skinning) require the mgfxc effect
  compiler, which is fragile on macOS, and there is no scene graph — the whole
  SceneKit layer would have to be rebuilt by hand. Rejected.
- **Godot 4**: develops and tests natively on this Mac, exports a real Windows
  .exe headlessly (templates installed), and its scene graph maps ~1:1 onto
  the SceneKit reference implementation. The `gl_compatibility` renderer
  blends in sRGB/gamma space like the 2002 WT engine (and unlike modern
  linear pipelines — see the overlay-alpha entry in `../HANDOFF.md`), which
  sidesteps the alpha-compensation machinery the macOS build needed.

## What is the porting source

**Port from `../macos/Sources/Cannonballs/*.swift`, not from scratch.** The
Swift is the verified transcription of the decompiled Java; it carries the
Java citations (`File.java:line`) inline. Rules:

1. Carry every comment and every `File.java:line` citation over **verbatim**.
   Add the Swift origin at the top of each file: `# Port of
   macos/Sources/Cannonballs/<File>.swift`.
2. Keep original constants literal (e.g. `640.0 / 96.0`, not `6.667`).
3. Where Godot forces a convention change, do it in ONE visible place with a
   comment. Known ones are listed below — reuse them, don't rediscover them.
4. `source/*.java` still outranks the Swift if they ever disagree.

## File map (Swift → GDScript)

| Swift | GDScript | Notes |
|---|---|---|
| Types.swift | src/types.gd | `G` constants + weapon tables |
| Assets.swift | src/assets.gd | runtime loading, never res:// imports |
| MapCatalog.swift | src/map_catalog.gd | `MapCatalog.MapInfo` inner class |
| Terrain.swift | src/terrain.gd | |
| World.swift | src/world.gd | |
| CameraController.swift | src/camera_controller.gd | |
| ModelLibrary/SkinnedModel | src/model_library.gd, src/skinned_model.gd | ArrayMesh + Skeleton3D |
| Props/WorldDressing/Chest/SkyActor | src/props.gd, src/world_dressing.gd, src/chest.gd, src/sky_actor.gd | |
| GameController/Cannon/Projectile/BotAI | src/game_controller.gd, src/cannon.gd, src/projectile.gd, src/bot_ai.gd | |
| Particles/FXSprites | src/fx_sprites.gd, src/particles.gd | |
| HUDScene/MenuScene/HUDArt/IrisTransition | src/ui/*.gd | CanvasLayer, 800x600 space |
| Audio.swift | src/audio.gd | |
| main.swift (uitest) | src/boot.gd | `--uitest` screenshot harness |

## SceneKit → Godot conventions (solved once, reuse)

- **Axes match**: both right-handed, camera forward −Z, +Y up. No coordinate
  flips anywhere.
- **Triangle winding flips**: SceneKit front faces are counter-clockwise,
  Godot front faces are **clockwise**. Swap the second/third index of every
  triangle when porting index buffers (see `terrain.gd:_rebuild_geometry`).
- **FOV matches**: `SCNCamera.fieldOfView` (default vertical) →
  `Camera3D.fov` (vertical with default `keep_aspect`). 55° stays 55°.
- **Light intensity**: SceneKit lumens ÷ 1000 → `light_energy` /
  `ambient_light_energy` (950 → 0.95).
- **`lightingModel = .constant`** → `shading_mode = SHADING_MODE_UNSHADED`.
  **`.lambert`** → shaded + `metallic_specular = 0`, `roughness = 1`.
- **`m.multiply.contents = color`** over a diffuse texture →
  `albedo_color = color` (albedo tint multiplies the texture).
- **`transparency = a`** → `albedo_color.a = a` + `transparency =
  TRANSPARENCY_ALPHA`.
- **`diffuse.contentsTransform` scale** → `uv1_scale`; animated offsets →
  advance `uv1_offset` in `_process` (no CAAnimation equivalent; keep the
  original rates in comments).
- **`CABasicAnimation`** → per-frame `_process` math or Tween; transcribe the
  from/to/duration values verbatim.
- **`renderingOrder`** → `render_priority` on the material (+ `no_depth_test`
  where the Swift disables depth reads).
- **`SCNBillboardConstraint`** → `billboard_mode = BILLBOARD_ENABLED` on the
  material, or per-frame orientation where the original billboards manually.
- **`hitTestWithSegment`** (prop mesh collision) → per-prop `StaticBody3D` +
  `ConcavePolygonShape3D` from the mesh triangles, queried with
  `PhysicsDirectSpaceState3D.intersect_ray`. Only build bodies for
  `wtCollideable` / `standable` props.
- **Image decal baking** (`NSImage.lockFocus`) → `Image.blend_rect` /
  per-pixel writes on a 512×512 RGBA8 canvas + `ImageTexture.update`.
  AppKit's bottom-left origin vs Image's top-left is already reconciled in
  `terrain.gd` (image row = `z/size*512` directly; no flip).
- **HUD / menus**: a `CanvasLayer` in the 800x600 canvas (project stretch mode
  `canvas_items` handles scaling). The original's screen-pixel coordinates
  (from `UI_TRANSCRIPTION.md` and the Swift) are used DIRECTLY: the SpriteKit
  conversions (`HUDScene.wt()` = 0.002736 units/px, y-up, center origin) drop
  out entirely. Canvas is y-down from top-left; original positions are quad
  CENTERS unless noted — either offset by half size or center the anchor.
- **Overlay alpha**: the macOS build routes HUD art through `HUDArt.image()`
  to compensate SceneKit's sRGB-decode of `overlaySKScene` alpha (see
  memory/HANDOFF). Godot's `gl_compatibility` canvas has no such decode —
  do NOT port the compensation; keep the loader API, document the omission.
- **Skinned meshes**: `skinned.json` → `ArrayMesh` with `ARRAY_BONES` /
  `ARRAY_WEIGHTS` (4 influences, zero-padded) + a `Skeleton3D`;
  `MeshInstance3D.skeleton` points at it. Decoded WT meshes rest facing −Z
  (see the cannon-barrel and wind-arrow notes in the Swift).
- **Naming**: Swift `camelCase` → GDScript `snake_case`. Swift static
  `load(...)` factories → `load_<thing>(...)` (avoid shadowing the global
  `load`). Keep class names identical (`class_name SkinnedModel`).
- **Typing across files**: only use typed references to classes that already
  exist in `src/`; while a sibling module is still being ported, keep the
  reference untyped (duck-typed call) so files parse independently.
- **Assets**: always loaded at runtime from `shared/Resources` (dev: found by
  walking up from the project dir; exported: `Resources/` beside the exe).
  NEVER copy assets into `res://` or use Godot's import pipeline for them —
  the tree is shared with the macOS build, read in place.

## Build / test

- Run on the Mac: `godot --path windows` (or `--path windows -- --uitest`
  to write `windows/snapshots/uitest-*.png` and quit — the analogue of the
  macOS `--uitest`).
- Parse-check everything headless: `godot --headless --path windows --quit`.
- Windows export: `godot --headless --path windows --export-release
  "Windows Desktop" build/Cannonballs.exe`, then copy `shared/Resources` →
  `build/Resources`. Smoke test under Wine Staging.
