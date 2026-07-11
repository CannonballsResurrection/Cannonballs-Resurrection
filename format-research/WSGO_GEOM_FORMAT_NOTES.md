# WildTangent `.wsgo` "geom" model format — reverse-engineering notes

Status: **container + structure + faces cracked; vertex quantization still open.**
The WTExtractor open-source decoder does NOT cover this format (it targets a
different WT variant, magic `\x00\x00\x00\x13`). These are original findings.

## Pipeline to get the geometry blob
`.wsgo` → WLD3 container decode (the cracked pipeline: key-from-metadata → rolling
XOR → CAB/MSZIP) → a raw "geom" blob. Textures (`.wsbm`) decode the same way
straight to JPEG (fully working — 33 extracted).

## geom blob layout (confirmed)
- `00`: class hash `17 fc 8e 07`; `04`: version `02 00 00 00`; `08`: zeros;
  `0C`: tag `6d 6f 65 67` = "moeg" = **"geom" reversed** (all 4-char tags are
  little-endian / reversed).
- **Node/frame tree** follows: per node → u32 namelen (incl trailing NUL), name,
  body, an `ff ff ff ff` marker, then a **16-float row-major 4×4 transform**.
  Multi-part models (e.g. CANNON = base+barrel+stone+wheels) use these transforms
  to assemble parts. Named nodes seen: "base", "cann"(on), etc.
- **`ViPm` visuals chunk** (bytes `56 69 50 6d`): tag + u32 flag(=1) + repeated
  (vcount, fcount) u32 pairs, then a byte-aligned **bbox** = 3×f32 min + 3×f32 max.
  Confirmed counts: arrow 316v/1140, chest 552v/1266, barrel 783v/2016, stone
  192v/780. (fcount appears to be an *index* count: arrow 1140 indices = 380 tris.)
- After the bbox: the packed vertex / normal / UV streams, then faces, then
  per-face detail.

## FACES — cracked ✅
Stored as **int16 little-endian triples**. In the arrow, at offset 5742: values
`0,1,2, 2,3,0, 4,5,6, 6,7,4, 8,9,10, 10,11,8, …` — i.e. **quads split into two
triangles** (verts {4k..4k+3} form quad k). Read int16 until a value ≥ vcount;
that delimits the face block. Arrow = 380 triangles from 1140 indices.

## ViPm chunk exact layout (confirmed, 2026-07-06)
For the arrow (ViPm tag at offset 442):
```
+0   'ViPm'                    (4 bytes)
+4   flag = 1                  (u32 LE)
+8   (vcount, fcount) pairs    3× (316,1140) then (0,1140) then zeros
     -> vcount=316, fcount=1140 (index count = 380 tris)
...  a triple of counts again (316,316,316) at 554, then 8 zero bytes
+506 bbox as TWO vec4 LE floats:  [min.x,min.y,min.z, 0]  [max.x,max.y,max.z, 0]
     = (-0.17755,-0.61733,-1.22026,0) (0.17755,0.61733,1.19338,0)
+538 4 more floats (12.0, ~0, -0.01344, ~0) — likely LOD/scale, unconfirmed
+574 two clean f32: -0.088776, +0.088776  (exactly half the X-span; role unknown)
+582 packed vertex stream begins (pattern `2c d3 d3 2c` = A,~A,~A,A repeating)
```
The whole geom blob is **little-endian** (floats and the bit stream), unlike the
open-source PWT variant which is big-endian.

## VERTICES — still OPEN ❌ (serious bounded search done 2026-07-06, no clean decode)
Vertices sit in the ~5.2 KB between the bbox (packed stream starts ~offset 582 in
the arrow) and the face block (5742). Bounding box is known exactly. UVs and
normals are believed bit-packed in the same region (no raw-float run of texcoords
in [0,1] exists anywhere in 570–5742, so UVs are NOT stored as plain floats).

### What was tried and RULED OUT (arrow, 316 verts, scored vs cracked faces)
Search axes swept, all combinations: stream start (byte 560–600, every bit
offset), accuracy 6–20, per-axis "unused bits" (3×6-bit header) AND uniform width,
AoS (xyzxyz…) AND SoA (all-X | all-Y | all-Z), byte bit-order {LSB-first,
MSB-first}, field endianness {MSB-field, LSB-field}, scale {single global
max-dimension, per-axis span}, local vs global bbox min. Also ran the **genuine**
`pwtdecode.BitfieldReader` (exact WT byte-reversal) directly.
- **Every bbox-scaled interpretation produces a uniform-random point cloud that
  FILLS the bbox** — the documented false positive. Rendered point clouds (XZ/YZ/XY)
  show noise, never an arrow silhouette.
- **Triangle-area coefficient-of-variation never drops below ~0.65** (correct mesh
  would be <0.3); combined planarity+CoV score never found a config with BOTH low.
- Low planarity is trivially gamed by giving one axis a tiny bit-width (collapses a
  dimension), so planarity alone is not a valid discriminator here.
- The genuine PWT reader gives CoV ~0.93 → this variant is genuinely NOT the PWT
  scheme byte-for-byte.
- Byte-aligned 3-bytes/vertex: CoV ~1.4 (worse).

### Leading hypothesis for why it resists (untested, needs the DLL)
`WTExtractor/docs/pwt_vertex_reorder_simulator.py` proves WT **spatially reorders
vertices** (a distance-based sort/flip). If the stream stores vertices in sorted
order while faces reference ORIGINAL indices, no contiguous decode can match the
faces — you'd need the inverse permutation, which isn't recoverable by sweeping.
A delta/predictive step (store first vertex, then bit-packed deltas) is also
consistent with the `A,~A,~A,A` alternating-complement byte pattern. Either makes
the blind search fundamentally unable to converge, which matches the observations.

### The decisive next lever: run the original decoder
`wt3d.dll` / `actorobject.dll` from WebDriver 4.1 (archive.org
`3DWildtangentWebdriver41`) decode these natively. A Wine + COM host that loads the
DLL and dumps the vertex array for the arrow would give ground-truth positions,
letting us (a) confirm the reorder/delta hypothesis and (b) fit the exact bit
layout against known-good output in minutes. This is the recommended path; the
patent (US6577769B1) is conceptual only and does NOT give the bit layout.

## Deliverable state (2026-07-06)
`/tmp/cb/tools/wsgo2obj.py` runs the whole pipeline over every model:
container-decode → node tree (names + 4×4 transforms) → ViPm counts+bbox →
FACES → textures→JPEG → OBJ/MTL/model.json. `decode_vertices()` is the single
isolated hook; when the bit layout is solved, only that function changes. Manifests
carry `vertices_solved: false` and a placeholder (bbox-scaled bytes, geometrically
wrong) so downstream plumbing is exercised. Faces recovered for ALL parts.

## Additional negative results (2026-07-06, second deep session)
Committed a focused second effort on the vertex block; all still negative:
- **Normal array via constant-magnitude**: scanned int16 AoS and SoA across the
  whole region for a run of triples with ~constant magnitude (unit normals). Best
  magnitude CoV ~0.31–0.36 (a real normal array would be <0.05). No clean normal
  block found → normals are NOT stored as plain int16 unit vectors here (or are
  computed at load, or packed differently).
- **Cross-part byte-diff (TAILS tail1/tail2/tail3)**: the 3 fins are NOT
  byte-identical (only ~half the bytes match: shared faces topology + headers,
  differing positions), so diffing did not cleanly isolate the position block.
  The 3 tails are different shapes, not instances.
- **libwld3 port does NOT apply**: `libwld3`/`pwt_decode.c` decode the PWT variant
  (big-endian, magic `\x00\x00\x00\x13`, bit-packed faces). This `geom` format is
  little-endian with a `ViPm` chunk and int16 faces — a genuinely different format.
  Porting libwld3 is not a route to decoding these `.wsgo` files.
- Confirmed positions are NOT contiguous float32 (longest plausible-float run is
  ~11, not vcount*3) and NOT clean int16 AoS/SoA (renders as noise / spaghetti).
  The region interleaves position/normal/UV int16 streams densely; every
  automated discriminator (bbox-fill, quad-planarity, mirror-symmetry,
  normal-magnitude, face-surface CoV) is defeated by the format's many
  zeros/repeats and probable vertex reordering.

**Honest status: the vertex quantization is unsolved and has resisted two
sustained multi-approach sessions. Recovering it likely needs either ground-truth
from the original DLL, or substantially more RE than attempted.** Everything else
in the geom format is solved.

## BREAKTHROUGH (2026-07-06, third session): vertex VALUE encoding CRACKED
The vertex stream is NOT one packed blob — it is **three per-component blocks**, one
each for X, Y, Z, laid out as:
```
[float32 min][float32 max][VC × 16-bit slot]      # one block per axis
```
Blocks are contiguous at stride = 8 + 2*VC. For the arrow (VC=316) the three
headers are at offsets 574 / 1214 / 1854; for TAILS (VC=16) at 575 / 615 / 655.
The header min/max is the component's extent (≈ half the padded ViPm bbox).

Each 16-bit slot is an **8-bit quantized value**: the low byte b (0–255) maps
linearly `coord = min + b/255 * (max-min)`. Small models store it byte-replicated
(`b,b` → 0x6868) or complemented (`b,~b` → 0x2c,0xd3); either way the low byte is b.
This is why every prior contiguous/SoA/AoS read produced noise — the interleaved
per-axis float headers threw off every fixed-stride interpretation.

**Verified value ranges:** every model decodes to the correct per-axis extents and
a correctly-bounded point set. Faces are the already-cracked int16 quad-split
triples.

**CORRECTION (don't overclaim):** only trivially-FLAT models (TAILS cattail =
crossed billboards) render cleanly, because flatness hides ordering errors. TORCH
(30v), a genuine 3-D shape, renders as a spiky mess when filled — so the vertex
values are right but the vertex-to-face ORDER is wrong even at 30 verts, not just
for big models.

### The one remaining blocker: vertex REORDER on larger models
Models above ~a few dozen verts (OBELISK 62v, TNT 93v, CHEST 552v, ARROW 316v)
decode to the correct value RANGES and the point set is bounded correctly, but the
faces (which use the same consecutive-quad indexing that works for small models)
connect non-adjacent vertices → long spanning triangles / spikes when rendered
filled. i.e. the stored vertex ORDER no longer matches the face indices for large
models. Small models are stored in face order (no reorder); large ones are
spatially reordered. The repo's `WTExtractor/docs/pwt_vertex_reorder_simulator.py`
(a selection-sort-style in-place swap keyed on a per-vertex scalar) is almost
certainly this reorder — replaying/inverting it on the decoded stream is the last
mile. Verified NOT a value-decode error: even-indexed quads are ~planar (0.03,
= 8-bit quantization noise); the breakage is purely ordering.

**Status: value encoding solved; mesh assembly blocked by a whole-vertex reorder.**

### Definitive finding on the blocker (third session, exhaustive)
- Faces are a real shared-vertex indexed mesh (arrow: 316 verts, 264 reused across
  380 tris — NOT independent quads), so face indices are meaningful.
- Per-axis value ranges decode correctly for every model; components are NOT
  individually sorted (X alternates its two planes, etc.).
- Yet face-area coefficient-of-variation stays ~0.955 under EVERY mapping tried
  (8-bit low-byte, full u16, signed-centered, and per-axis mixed 8/16-bit), and the
  order-independent point cloud is not a clean silhouette. So the stored vertex
  order does not correspond to the face index space — a whole-vertex spatial
  REORDER (Morton/radix-style, cf. `pwt_vertex_reorder_simulator.py`). Reversing it
  needs the original pre-sort order, which is not recoverable from the file data
  alone (and may be compounded by a residual quantization detail).
- Only trivially-flat models (cattail billboards) reconstruct regardless of order.

**Conclusion: the vertex VALUE format is cracked; faithful mesh reconstruction for
non-flat models is blocked by an apparently data-irreversible vertex reorder. The
shipped high-fidelity result remains the textured-proportioned procedural props.**

## What this unblocks once vertices are solved
Faces ✅ + node transforms ✅ + textures ✅ (JPEG) + UVs (per-vertex, locate near
vertices) → export OBJ+MTL per part → the SceneKit `ModelLibrary` (already built
and wired) loads them with zero further plumbing.

## Per-model extraction table (2026-07-06, via wsgo2obj.py)

22 models / 42 parts. Container decode, node tree, ViPm counts, FACES, and
textures→JPEG all succeed for every part. Vertex positions are placeholder
(geometrically wrong) pending the vertex-quantization crack.

| Model | Part | Verts | Tris | Container | Nodes | Faces | Vtx-solved |
|---|---|---|---|---|---|---|---|
| ARROW | actor | 316 | 380 | ✅ | ✅ | ✅ | ❌ |
| BOUNCEBALL | actor | 380 | 378 | ✅ | ✅ | ✅ | ❌ |
| BRIDGE | actor | 164 | 126 | ✅ | ✅ | ✅ | ❌ |
| BRUSH2 | actor | 180 | 200 | ✅ | ✅ | ✅ | ❌ |
| CANNON | barrel | 783 | 673 | ✅ | ✅ | ✅ | ❌ |
| CANNON | base | 626 | 560 | ✅ | ✅ | ✅ | ❌ |
| CANNON | stone | 192 | 260 | ✅ | ✅ | ✅ | ❌ |
| CHEST | actor | 552 | 422 | ✅ | ✅ | ✅ | ❌ |
| FERNTREE | actor | 418 | 456 | ✅ | ✅ | ✅ | ❌ |
| FIREHEAD | actor | 444 | 436 | ✅ | ✅ | ✅ | ❌ |
| HUT | actor | 150 | 96 | ✅ | ✅ | ✅ | ❌ |
| HUT | debris1 | 41 | 30 | ✅ | ✅ | ✅ | ❌ |
| LIGHTBEAM | actor | 32 | 16 | ✅ | ✅ | ✅ | ❌ |
| LIGHTHOUSE | actor | 144 | 72 | ✅ | ✅ | ✅ | ❌ |
| MORTAR | actor | 435 | 397 | ✅ | ✅ | ✅ | ❌ |
| MOUND | beam | 128 | 64 | ✅ | ✅ | ✅ | ❌ |
| MOUND | mound | 157 | 188 | ✅ | ✅ | ✅ | ❌ |
| OBELISK | actor | 62 | 46 | ✅ | ✅ | ✅ | ❌ |
| OBELISK | debris1 | 22 | 14 | ✅ | ✅ | ✅ | ❌ |
| OBELISK | debris2 | 24 | 12 | ✅ | ✅ | ✅ | ❌ |
| OBELISK | debris3 | 64 | 44 | ✅ | ✅ | ✅ | ❌ |
| PALM2 | actor | 439 | 532 | ✅ | ✅ | ✅ | ❌ |
| PALM2 | brush1 | 216 | 240 | ✅ | ✅ | ✅ | ❌ |
| PALM2 | debris1 | 64 | 70 | ✅ | ✅ | ✅ | ❌ |
| PALM2 | debris2 | 64 | 70 | ✅ | ✅ | ✅ | ❌ |
| PALM2 | debris3 | 184 | 208 | ✅ | ✅ | ✅ | ❌ |
| PALM2 | debris4 | 216 | 240 | ✅ | ✅ | ✅ | ❌ |
| SHIP | base | 307 | 268 | ✅ | ✅ | ✅ | ❌ |
| SHIP | mast | 122 | 84 | ✅ | ✅ | ✅ | ❌ |
| SPIKEBALL | actor | 370 | 224 | ✅ | ✅ | ✅ | ❌ |
| TAILS | tail1 | 16 | 8 | ✅ | ✅ | ✅ | ❌ |
| TAILS | tail2 | 16 | 8 | ✅ | ✅ | ✅ | ❌ |
| TAILS | tail3 | 16 | 8 | ✅ | ✅ | ✅ | ❌ |
| TIKKI | debris1 | 60 | 46 | ✅ | ✅ | ✅ | ❌ |
| TIKKI | debris2 | 46 | 44 | ✅ | ✅ | ✅ | ❌ |
| TIKKI | debris3 | 50 | 30 | ✅ | ✅ | ✅ | ❌ |
| TIKKI | tikki1 | 113 | 100 | ✅ | ✅ | ✅ | ❌ |
| TIKKI | tikki2 | 84 | 96 | ✅ | ✅ | ✅ | ❌ |
| TIKKI | tikki3 | 93 | 102 | ✅ | ✅ | ✅ | ❌ |
| TNT | actor | 93 | 116 | ✅ | ✅ | ✅ | ❌ |
| TORCH | actor | 30 | 42 | ✅ | ✅ | ✅ | ❌ |
| TORCHBEARER | actor | 222 | 216 | ✅ | ✅ | ✅ | ❌ |
