# WebDriver / Genesis3D rasterizer — behavior spec

Cannonballs rendered through WebDriver's `dx7drv.dll` → Genesis3D's **D3D7xDrv**
(DirectX 7 fixed-function). This is the exact pipeline the clone's software
rasterizer reproduces. Every state below is verbatim from
`source/engine/native/genesis3d/Engine/Drivers/D3D7xDrv/`.

## Pipeline states (from `Render.cpp` / `Scene.cpp` / `D3d_fx.cpp`)

| State | Value | Meaning for the rasterizer |
|---|---|---|
| `SHADEMODE` | `GOURAUD` | interpolate vertex color across the triangle |
| `LIGHTING` | `FALSE` | **no hardware lighting** — the engine bakes color per vertex on the CPU |
| `COLORVERTEX` | `FALSE` | vertices carry their own diffuse color |
| `SPECULARENABLE` | `FALSE` | **no specular** (the cannon must not be glossy) |
| `COLOROP` (stage 0) | `MODULATE(TEXTURE, DIFFUSE)` | final = texel × vertex color |
| `TEXTUREMAG` | `LINEAR` | bilinear magnification (soft, not nearest) |
| `TEXTUREMIN` | `LINEARMIPNEAREST` | bilinear min, nearest mip |
| `TEXTUREPERSPECTIVE` | `TRUE` | perspective-correct UV |
| `CULLMODE` | `NONE` | **no backface culling** (everything double-sided) |
| `COLORKEYENABLE` | `TRUE` | color-key transparency (foliage etc.) |
| `FOGTABLEMODE` | `LINEAR` | per-pixel linear fog, `FogStart/FogEnd/FogColor` |
| `DITHERENABLE` | `TRUE` | ordered dither into 16-bit |
| `ANTIALIAS` | `NONE` (mostly) | hard polygon edges |
| framebuffer | `PIXELFORMAT_16BIT_565_RGB` | **RGB565** output |
| `ZFUNC` | `LESSEQUAL` | standard depth test |

## The "2002 look", decomposed

1. **RGB565 + ordered dither** — the single biggest signature: 5/6/5-bit color
   with a 4×4 Bayer dither, so gradients band and stipple.
2. **Bilinear-magnified low-res textures** — soft/blurry up close, not crisp pixels.
3. **Flat Gouraud vertex lighting** — no per-pixel shading, no specular; lighting
   is smooth per-vertex and baked.
4. **Native ~640×480** rendered then scaled to the window (chunky, aliased edges).
5. **Per-pixel linear fog** toward the map's horizon color.
6. **No backface culling; color-key (not alpha) cutouts.**

## Per-vertex lighting (engine-baked, since D3D LIGHTING=FALSE)

`vertexColor = clamp01(ambientRGB + max(0, dot(N, sunDir)) · sunRGB)`, then
`pixel = texel(bilinear) · vertexColor`, then fog, then 565+dither. Matches the
per-map `ambientRGB` / `sunRGB` already in `maplist.dat`.

Reproduced by `macos/Sources/Cannonballs/SoftRaster.swift`.

## Live integration

Wired into the game as a toggle: press **R** in-game to swap the SceneKit 3D layer
for the software rasterizer. `SceneRasterizer.renderFrame` runs each frame on the
render thread over the live `SCNScene` + game camera; the 640×480 RGB565 result is
shown as a nearest-upscaled full-screen background behind the (unchanged) SpriteKit
HUD. Off by default; the SceneKit path stays the stable default.
