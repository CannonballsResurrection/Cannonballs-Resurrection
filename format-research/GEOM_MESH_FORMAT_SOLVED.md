# `.wsgo` "geom" / VIPM mesh format — SOLVED

Reverse-engineered from `actorobject.dll` (`geGeometryRes::ReadData`) +
`WDENGINE.dll` (`VIPM_CreateFromFile` @ RVA 0x6db55, `geVFileUtil_ReadPackedVecArray2`,
`geVFileUtil_ReadFloatArray16` @ RVA 0x5af3a). Verified: arrow, cannon, chest, torch
all decode into clean coherent meshes (`arrow_PLANAR.png`, `CRACKED_models.png`).

## The key that broke 5 sessions of failure
16-bit quantized values are stored **byte-PLANAR**: all HIGH bytes, then all LOW
bytes — NOT interleaved little-endian int16. Every prior attempt read them as
contiguous int16 and got noise. From `geVFileUtil_ReadFloatArray16` (0x5af8a):
```
scale = (max - min) * (1/65535)          # asm constant 1.5259021893143654e-05
for i in count:
    u16    = (hi_plane[i] << 8) + lo_plane[i]
    out[i] = min + u16 * scale
```
Degenerate axis: if `(max-min) < ~0.000797` the component is constant (= min).

## Container → mesh
1. `.wsgo` → WLD3 container decode (WTExtractor) → "geom" blob (`17 fc 8e 07`, ver 2, "moeg").
2. `geGeometryRes::ReadData`: "geom" magic, version(≤4), chunk count, materials, bones
   (64-byte 4×4 matrices), chunk headers; each material's mesh via `VIPM_CreateFromFile`.
3. **VIPM blob** = "ViPm" magic + u32 version(≤1) + ~124-byte header struct.
   - **render vertex count N = u32 at ViPm+8**.
   - positions begin at **ViPm+132**.
4. **Positions** = 3 per-component blocks (SoA), each block = `[f32 min][f32 max][N hi-bytes][N lo-bytes]`, block size = `8 + 2N`. Dequant per the formula above.
   (Normals + UVs follow in the same packed form: `ReadPackedLVertArray2` reads
   pos(vec3), normal(vec3), then 3 `ReadFloatArray16` floats @LVert +0x1c/+0x20/+0x24;
   LVert stride 0x30. UVs are the +0x20/+0x24 pair.)
5. **Faces** = int16 LE triangle indices (values < N), quad-split winding.

## Minimal working decoder
See `../tools/wsgo_decode_SOLVED.py` (positions + faces → OBJ). Extend with the
normal/UV blocks (they follow positions, same packed layout) for full materials.

## Full blob layout — 100% SOLVED (2026-07-09, from VIPM_CreateFromFile disassembly)

The complete VIPM blob, byte-exact (parse ends exactly at EOF on every model tested):

```
ViPm+0    : "ViPm", u32 version (<=1)
ViPm+8    : 124-byte header struct read field-by-field:
            +8   u32 N (vertex count, <=65536)     +12  u32 numIndices
            +48  u32, +52 u32 collapseCount, +56 u32, +60 u32 extraCount
            +64  32B bounding box (min vec3+pad, max vec3+pad)
            +96  16B (error/LOD params; f32 at +100)
            +112 u32, +116 u32 normalsPresent, +120 u32 skinPresent
ViPm+132  : LVert array via ReadPackedLVertArray2 = 9 packed components in order:
            X, Y, Z,  R, G, B,  A,  U, V          (engine LVert stride 0x30)
          : if normalsPresent: 3 more packed components (Nx, Ny, Nz)
          : extraCount*4 bytes, collapseCount*12 bytes   (VIPM LOD records)
          : numIndices * u16   triangle list
          : if skinPresent: N per-vertex skin records (see below)
```

**Packed component** (`geVFileUtil_ReadFloatArray16`, RVA 0x5af3a):
`[f32 min][f32 max]`, then IF `min + EPS >= max` (degenerate/constant): **4 filler
bytes** (the writer stuffs the literal "stuf"/"futs" tag) and every value = min;
ELSE hi/lo byte planes **in 8192-element batches** (hi[8192] lo[8192] hi[...]...),
`v = min + ((hi<<8)|lo)/65535 * (max-min)`.

This resolves the old "extra per-vertex array" mystery: those models (HUT,
LIGHTHOUSE, FIREHEAD, MOUND, CANNON/stone, TAILS, LIGHTBEAM, debris) simply have
real per-vertex COLOR data (non-constant R/G/B/A blocks) — most models have
constant white (4 degenerate blocks = the 48-byte "header" previously skipped).

**Per-vertex skin record** (matrix-palette skinning, read at RVA 0x6de7b):
`[u8 n][n x (u8 paletteSlot, u8 weight)]` — weight/255, weights sum to 255,
n = 1..4 observed. paletteSlot indexes the material's K-array (see below).

## Skeleton + skinning (geom container header) — SOLVED

- The A "chunks" ARE the bones. Per chunk header: name, 32B bbox, u32, **u32
  parentIndex** (0xFFFFFFFF = root), 64B local matrix (exporter junk in pads —
  derive bind locals from the inverse-bind table instead).
- The A x 64B matrix table at header offset 0x34 (this+0x48, used by PrepVBSkin's
  `geEngineVB_SetSkinMatrix`) = **inverse world bind matrix per chunk**. 64B
  matrix = padded 3x4: rotation rows at floats 0-2/4-6/8-10, translation 12-14,
  pads garbage.
- Per material: `u32 K, K x u32 chunkIndex, u32` — the **matrix palette**:
  vertex paletteSlot s → chunk `arr[s]`. (CHEST: K=4 arr=[6,5,3,4]; CANNON
  barrel: K=7.)
- Bind local = inv(worldBind[parent]) @ worldBind[chunk]; motion composes AFTER
  the bind local (Genesis3D pose.c:256: World = ParentWorld x Attachment x
  MotionSample). Verified: skinned CHEST + CANNON barrel render byte-identical
  to bind pose at t=0 and animate coherently.
- Exporter: `../tools/wsgo_export_skinned.py` → `MODELS/<NAME>/skinned.json`;
  playback: `macos/.../SkinnedModel.swift` (SCNSkinner + bindNode→motionNode
  hierarchy). Motion format: `MOTION_FORMAT.md` + `../tools/wsmo_decode.py`.

## Mesh -> macOS clone pipeline (done)
1. `wsgo2obj_final.py` exports each OBJECT to `models_final/<NAME>/` (OBJ+MTL+
   textures + `model.json`), identity part transforms (parts share a coordinate
   space — verified by rendering assembled CANNON/PALM2/SHIP), debris/brush
   parts written to disk but excluded from the assembled manifest.
2. A staging step maps objects.dat/PROPS prop names -> model folders
   (PALM->PALM2, TIKKI1/2/3->TIKKI/tikkiN, MOUNDBEAM->MOUND/beam, etc.), splits
   the 3 TIKKI variants into separate folders, and sets `scale = prop.dat
   HEIGHT / geom-bbox-height` (the geom is authored at ~half world scale).
3. Copied into `shared/Resources/MODELS/<PROP>/` (the asset tree all builds consume);
   `ModelLibrary.swift` + `PropGeometry.node(for:)` load them automatically.
   Verified in-game: Tropicali/Voodoo/OldGods render real palm/tiki/hut/obelisk
   meshes (`model-previews/ingame_*.png`).

## Actor material render style — the `.wsad` `taMg` ("gMat") chunk (solved 2026-07-09)

The geom (`.wsgo`) carries NO blend/render info; that lives in the actor
container (`.wsad`, WLD3-decode it first) as one `taMg` chunk per material:

```
'taMg' u32(7?) classHash(17 fc 8e 07) u32 version(2)
then: 3 zeros, u32, 3 floats (ambient RGB), 3 floats (emissive RGB, 0-255),
      u32 1, u32 1, u32 RENDER STYLE, u32 1, u32 1, 2 zeros, f32 400.0, ...
```

The **render style** word (15th u32 after the tag) observed per actor:

| actor material | style | emissive | ground truth |
|---|---|---|---|
| LIGHTBEAM `lightbeam` | **2** | 0,0,0 | lighthouse beam — light, beyond doubt |
| MOUND `beam` (MOUNDBEAM) | **2** | 255,255,255 | the green eye-beams |
| MOUND `mound` | 0 | 0,0,0 | opaque stone disc |
| MOUND `moundillum` | 5 | 0,0,0 | second mound material (unsolved; cf. water/terrain `setLayerType(…,5)`) |
| PALM2, TORCH | 0 | 0,0,0 | opaque structures |

Pin: LIGHTBEAM (visually indisputable light source) solves **style 2 =
additive/light**; every opaque structure carries 0. Same enum family as the WT
shader layer types the game itself sets (`Island.java:955` `setLayerType(1,5)`
water, `Cannon.java:1514` type 4). Style 5 and the exact meaning of the other
words remain unpinned. The skinned exporter does NOT carry this flag into
`skinned.json`; the clone applies it in `Props.swift`
(`applyBeamLightMaterials`, used by LIGHTBEAM + MOUNDBEAM).
