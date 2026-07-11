# WildTangent `.wsgo` "geom" decode spec (static disassembly)

**Binaries analyzed**
- `actorobject.dll` (WildTangent fork of Genesis3D). ImageBase `0x66d00000`, `.text` RVA `0x1000`.
- `webdriver.dll` (712 KB) — checked; does **not** contain the geometry reader (only 11 exports, none geometry).
- Import DLL `WDENGINE.dll` — the real engine. **Not present in the folder or anywhere on this machine.**

**Tooling:** capstone 5.0.7 + pefile via `/tmp/cb/wtvenv/bin/python`. Helper script: `/tmp/cb/analysis/wtdis.py`. Every `call dword [0x...]` below was resolved against actorobject.dll's IAT.

---

## TL;DR / bottom line up front

The `.wsgo` "geom" **container header** (magic, version, counts, chunk headers, materials, bone matrices) is read entirely by
`geGeometryRes::ReadData` (RVA `0x7125`) in **actorobject.dll**, and that part is now fully documented below.

The actual **per-mesh vertex positions, triangle indices, UVs, and normals are NOT read by actorobject.dll.** For a serialized geom file they are consumed in one call to:

```
WDENGINE.dll!@VIPM_CreateFromFile@4        (IAT slot 0x66d0e400)
```

called once per material at RVA `0x73b6` inside the ReadData tail loop. That function is the ground-truth vertex/face/uv/normal decoder, and **`WDENGINE.dll` is not available** — so the exact byte layout, data types, endianness, and any bit-packing/dequantization of the mesh blob **cannot be recovered from the binaries provided.** I will not guess it. The conceptual reference (Genesis3D `geBody`) and everything I *can* prove are documented below, with the gap called out explicitly.

---

## 1. Container header format (fully resolved — `geGeometryRes::ReadData`, RVA 0x7125)

`this`/`edi` = `geGeometryRes*`. `esi` is loaded once as the read primitive:
`esi = [0x66d0e2a8] = WDENGINE.dll!@geVFile_Read@12` (thiscall: `ecx`=vfile, `edx`=dest buffer, `push` count). All multi-byte values little-endian.

`vfile` variable: `[ebp+0xc]`. It starts as the incoming file (`[ebp+8]`), but for **version < 3** it is replaced by a decompression substream opened with `@geVFile_OpenNewSystem@20` (RVA 0x7189); for version ≥ 3 reads come straight from the incoming file.

Read order:

| Order | RVA | Bytes | Type | Destination | Meaning |
|---|---|---|---|---|---|
| 1 | 0x7148 | — | — | — | base `geResource::ReadData` (IAT 0x66d0e40c) reads the resource header first |
| 2 | 0x715f | 4 | u32 | `[ebp-0x20]` | **magic**, must == `0x67656F6D` ("geom", little-endian on disk = `6D 6F 65 67`) |
| 3 | 0x7171 | 4 | u32 | `[ebp-0x18]` | **version**, must be ≤ 4. Branches: `<3` (decompress substream), `>1`, `≥4` |
| 4 | 0x71b9 | 4 | u32 | `this+0x88` = **A** | **chunk count** |
| 5 | 0x71c7 | 4 | u32 | `[ebp-0x1c]` = **B** | **material-name / geGeometry_Material construct count** |
| 6 | 0x71d4 | 4 | u32 | `this+0x98` = **C** | **material count** (VIPM count) |
| 7 | 0x71e4 | 4 | u32 | `this+0x78` | present only when version > 1 (else zero-filled) |
| 8 | 0x7205 | 4 | u32 | `this+0x60` | present only when version ≥ 4 (else set to 4) |
| 9 | 0x7215 | — | — | — | `call 0x7c97` with B (reserves/reserves material vector) |

Then three allocations (all via WDENGINE `geRam`):
- `A * 0x168` (360 B) → `this+0x8c` — **chunk array** (`geGeometry_Chunk`, 360 B each), `geRam_AllocateClear`.
- `C * 0x134` (308 B) → `this+0x9c` — **material array** (`geGeometry_Material`, 308 B each), `geRam_AllocateClear`.
- `A * 0x40` (64 B) → `this+0x48` — **A×(4×4 float matrix)**; RVA 0x725f reads `A*0x40` bytes **in one shot** into this buffer (one 64-byte transform per chunk).

### 1a. Material-name loop — B iterations (RVA 0x7268–0x72c5)
Per iteration:
- `@geVFileUtil_ReadString@8` → temp `[ebp-0x150]` (length-prefixed string; see §4).
- construct a `geGeometry_Material` from the name (`??0geGeometry_Material...` RVA 0x7fdc) and append to the material vector at `this+0x94` (`call 0x7d55`).

### 1b. Chunk-header loop — A iterations (RVA 0x72d7–0x7332)
`ebx` walks the chunk array (`this+0x8c`, stride 0x168). Per chunk:
- `@geVFileUtil_ReadString@8` → **chunk name** (into `chunk+0` region).
- read **0x20 (32) bytes** → `chunk+0x144`.
- read **4 bytes** (u32) → `chunk+0x164`.
- read **4 bytes** (u32) → `chunk+0x140`.
- read **0x40 (64) bytes** → `chunk+0x100` — a 4×4 float matrix (chunk transform).

This loop reads **only the chunk header** (name + 32-byte blob + two u32s + one matrix). **No vertex/face/uv data here.** The 32 bytes at `chunk+0x144` are read verbatim; their internal meaning is not decoded by actorobject.dll.

### 1c. Material-data loop — C iterations (RVA 0x7344–0x7387)
`ebx` walks the material array (`this+0x9c`, stride 0x134). Per material:
- read **4 bytes** (u32) → `mat+4` = **K** (a count).
- read **K*4 bytes** → `mat+8` (array of K u32/float — e.g. bone/bitmap index list).
- read **4 bytes** (u32) → `mat+0`.

### 1d. Close substream (version < 3 only)
RVA 0x7392: `@geVFile_Close@4` on the decompression substream.

### 1e. VIPM build loop — C iterations (RVA 0x73aa–0x73d9) — **THE VERTEX/FACE READ**
`esi` walks the material array (`this+0x9c`, stride 0x134); `ecx = [ebp+8]` = the **outer** file.
```
esi = material_array + i*0x134
mat->[0x88] = WDENGINE.dll!@VIPM_CreateFromFile@4(  ecx = original vfile )   ; IAT 0x66d0e400
if result == 0  -> ReadData fails (jump to error at 0x7196)
```
So the geom container header is a directory; the **mesh geometry for each material is a self-contained VIPM blob that `VIPM_CreateFromFile` parses from the file stream, one per material, in order.** The returned handle is stored at `mat+0x88`. Return value 1 = success.

---

## 2. The vertex/face/uv/normal decode — WHERE IT LIVES AND WHY IT'S UNRESOLVED

**Single call site:** RVA `0x73b6`, `call dword [0x66d0e400]` → `WDENGINE.dll!@VIPM_CreateFromFile@4` (thiscall, `ecx`=vfile, no other args — signature `@...@4` = 4 bytes of args = just the `this`/vfile in ecx plus the implicit thiscall... it takes the vfile pointer). This is the **only** function that reads mesh coordinates for a serialized geom.

Everything downstream also delegates to WDENGINE and never exposes the raw layout:
- `PrepVBSkin` (RVA 0x6426) gets vertices via `WDENGINE.dll!@VIPM_GetVertexBuffer@4` (IAT 0x66d0e330) — an opaque engine vertex buffer. actorobject only reads a **64-byte** per-vertex skin record (`shl esi,6` at 0x648c indexing `this+0x48`) for the skin-matrix path; that is the transform table from §1, not the mesh vertices.
- Related WDENGINE VIPM imports (all in WDENGINE, none defined here): `VIPM_Create32@36` (0xe410), `VIPM_Create16@36` (0xe3f8), `VIPM_GetVertexBuffer@4`, `VIPM_GetTriList@12`, `VIPM_GetFullNumVerts@4`, `VIPM_GetNumVertsForError@8`, `VIPM_Render@28`, `VIPM_SetMaterial@8`, `VIPM_Destroy@4`, `VIPM_CreateDuplicate@4`.

**Why the layout is not in these binaries:**
- `WDENGINE.dll` is imported by BOTH `actorobject.dll` and `webdriver.dll` (and `dx7drv.dll`), but the file is **not in `/webdriver_dlls/from_xp_image/`** and not found anywhere on this machine (`mdfind`/`find` returned nothing). It is a separate module I was not given.
- `webdriver.dll` exports only 11 symbols (BoneObject ctor/dtor/vtable, CWT window/download helpers, DllRegisterServer, etc.) — it does **not** contain a VIPM or geVFile reader.
- The Genesis3D source snapshot in `/tmp/cb/genesis3d/` contains **no** VIPM / progressive-mesh module (`grep -rn VIPM` → nothing). VIPM was WildTangent's proprietary addition on top of Genesis3D, shipped inside WDENGINE.dll.

**Honest conclusion:** the exact `.wsgo` mesh element order, data types (float32 vs int16 vs bit-packed), any accuracy/unused-bits quantization scheme, count acquisition, and dequant math are defined **entirely inside `VIPM_CreateFromFile` in WDENGINE.dll**, which is unavailable. I cannot state them from disassembly, and I will not infer them from sample bytes (the explicitly-forbidden path). **To finish this spec, obtain `WDENGINE.dll` from the same XP image** and disassemble `@VIPM_CreateFromFile@4` and its `@geVFile_Read@12` / `@geVFileUtil_ReadFloatArray16_2@16` call sequence.

### 2a. Strong hint at the quantization scheme (not proof)
actorobject.dll imports `WDENGINE.dll!@geVFileUtil_ReadFloatArray16_2@16` (IAT `0x66d0e448`) — a helper that reads a **16-bit-quantized float array** (the "2" likely = 2-component / stride, "16" = 16-bit). This is the WildTangent analogue of Genesis3D's packed-float scheme and is the most likely primitive `VIPM_CreateFromFile` uses for positions/UVs. **But in actorobject.dll it is only called from the MOTION reader (`geMotionRes`), at RVA 0x b9d5 and 0x c73e — NOT from geometry.** So it is evidence of the engine's packing style, not proof of the geom mesh layout. Do not treat the geom mesh as ReadFloatArray16-packed without confirming inside WDENGINE.

---

## 3. Conceptual reference — Genesis3D `geBody_ReadGeometry` (`/tmp/cb/genesis3d/Actor/body.c:1403`)

This is the **ancestor** format WDENGINE forked from. It is a **different serialization** from VIPM (raw structs, not progressive-mesh, not bit-packed), so use it only to understand the element *set*, not the byte layout. Order in `geBody`:
1. u32 file-type magic (`GE_BODY_FILE_TYPE`), u32 version.
2. `BoundingBoxMin` (geVec3d, 3×float32), `BoundingBoxMax` (3×float32).
3. u32 `XSkinVertexCount`, then `count * sizeof(geBody_XSkinVertex)` raw (position + UV + bone-weight refs).
4. u32 `SkinNormalCount`, then `count * sizeof(geBody_Normal)` raw (normals).
5. u32 `BoneCount`, then `count * sizeof(geBody_Bone)` raw; then bone-name StrBlock.
6. u32 `MaterialCount`, then `count * sizeof(geBody_Material)` raw; then material-name StrBlock.
7. u32 `LevelsOfDetail`; per LOD: u32 `FaceCount` then `count * sizeof(geBody_Triangle)` (index triples).

Genesis3D `geBody` faces are **int index triangles**, vertices are **float32 position + float UV**, normals are a separate float array. WildTangent's VIPM reorders this into a progressive-mesh with collapse records and (probably) 16-bit-quantized attributes, and stores it as the per-material blob read by `VIPM_CreateFromFile`. The specific field widths differ and are not recoverable here.

---

## 4. `geVFileUtil_ReadString@8` (name reader) — used for material names (§1a) and chunk names (§1b)
Defined in WDENGINE (not disassemblable here). Genesis3D convention (`VFile/`): a length-prefixed string (u32 length + bytes, or NUL-terminated depending on version). Names are read into a stack temp then copied into the object.

---

## 5. What I traced vs. could not resolve

**Traced (actorobject.dll, ground truth):**
- `geGeometryRes::ReadData` (RVA 0x7125) — full container header, all counts, chunk-header loop, material-data loop, VIPM build loop. (Appendix A.)
- `BuildVipmsFromGeoChunkIdx` (RVA 0x73ea) — the *alternate* in-memory build path: calls `VIPM_Create32@36` from material fields (`+0x94` vert count, `+0x9c` index count ÷3, `+0xa4` vert ptr, `+0xac` idx ptr, `+0xa8`, `+0x90`). Used when geometry is assembled in RAM (importer path), **not** the file-load path.
- `AddTriToGeoChunks` (RVA 0x74ca) — grows per-chunk vertex/index scratch arrays (caps: verts < 0x30000, indices < 0xFFDF) during in-memory build; index stride 0x134 material records, `+0x64` = vertex stride count.
- `PrepVBSkin` (RVA 0x6426) — render-time; pulls vertices via opaque `VIPM_GetVertexBuffer`, applies skin matrices from `this+0x48` (64 B stride).
- Full IAT resolved (Appendix B).

**Could NOT resolve (missing WDENGINE.dll):**
- The mesh blob byte layout: element order (positions/normals/uvs/faces), data types, sizes, endianness, and any dequantization formula. This is 100% inside `WDENGINE.dll!@VIPM_CreateFromFile@4`. **No guess given.**
- Exact semantics of the 32 bytes at `chunk+0x144` and the two u32s at `chunk+0x140` / `chunk+0x164` (read verbatim by actorobject; interpreted elsewhere).
- `geVFileUtil_ReadString@8` exact wire format (length-prefix vs NUL) — inside WDENGINE.

---

## Appendix A — annotated `geGeometryRes::ReadData` (RVA 0x7125), call targets resolved

Key resolved calls:
- 0x7148 `call [0x66d0e40c]` → `WDENGINE.dll!geResource::ReadData` (base header)
- 0x715f/0x7171/... `call esi` where `esi=[0x66d0e2a8]` → `WDENGINE.dll!@geVFile_Read@12`
- 0x7189 `call [0x66d0e408]` → `WDENGINE.dll!@geVFile_OpenNewSystem@20` (decompress substream, ver<3)
- 0x7222/0x723a `call [0x66d0e254]` → `WDENGINE.dll!@geRam_AllocateClear@4`
- 0x724b `call [0x66d0e2b0]` → `WDENGINE.dll!@geRam_Allocate@4`
- 0x7271/0x72e5 `call [0x66d0e2a4]` → `WDENGINE.dll!@geVFileUtil_ReadString@8`
- 0x7294 `call 0x7fdc` → `??0geGeometry_Material@@...` (material ctor)
- 0x7392 `call [0x66d0e404]` → `WDENGINE.dll!@geVFile_Close@4`
- **0x73b6 `call [0x66d0e400]` → `WDENGINE.dll!@VIPM_CreateFromFile@4`  ← the mesh reader**

(Full instruction listing: `/tmp/cb/analysis/readdata.txt`.)

## Appendix B — relevant WDENGINE.dll imports (actorobject.dll IAT)

```
0x66d0e2a8  @geVFile_Read@12                     (mesh/header byte reader)
0x66d0e2a4  @geVFileUtil_ReadString@8            (names)
0x66d0e448  @geVFileUtil_ReadFloatArray16_2@16   (16-bit packed floats; used by MOTION reader only)
0x66d0e408  @geVFile_OpenNewSystem@20            (decompress substream, ver<3)
0x66d0e404  @geVFile_Close@4
0x66d0e400  @VIPM_CreateFromFile@4               *** the vertex/face/uv/normal decoder (missing DLL) ***
0x66d0e410  @VIPM_Create32@36                    (in-memory build; 32-bit indices)
0x66d0e3f8  @VIPM_Create16@36                    (in-memory build; 16-bit indices)
0x66d0e330  @VIPM_GetVertexBuffer@4              (opaque decoded vertex buffer)
0x66d0e3e0  @VIPM_GetTriList@12
0x66d0e328  @VIPM_GetFullNumVerts@4
0x66d0e254  @geRam_AllocateClear@4
0x66d0e2b0  @geRam_Allocate@4
0x66d0e40c  geResource::ReadData
```

## Appendix C — struct offsets recovered

`geGeometryRes` (this):
- `+0x48` = ptr to A × 64-byte transform matrices (one per chunk)
- `+0x60` = u32 (ver≥4 field; else 4)
- `+0x78` = u32 (ver>1 field; else 0)
- `+0x88` = A, chunk count
- `+0x8c` = ptr to chunk array (stride 0x168 = 360 B)
- `+0x94` = material vector (name-constructed materials)
- `+0x98` = C, material count
- `+0x9c` = ptr to material array (stride 0x134 = 308 B)

`geGeometry_Chunk` (360 B, stride 0x168):
- `+0x00..` name region
- `+0x100` = 64-byte 4×4 float matrix (chunk transform)
- `+0x140` = u32
- `+0x144` = 32-byte blob (verbatim; meaning decoded elsewhere)
- `+0x164` = u32

`geGeometry_Material` (308 B, stride 0x134):
- `+0x00` = u32 (read last in material-data loop)
- `+0x04` = K (count)
- `+0x08` = K × u32 array
- `+0x88` = VIPM handle (result of `VIPM_CreateFromFile`)
- `+0x90` = in-mem index array ptr (build path)
- `+0x94` = vertex count (build path)
- `+0x9c` = index count (build path; ÷3 → tri count)
- `+0xa4` = vertex array ptr (build path)
- `+0xa8`, `+0xac` = build-path scratch ptrs
- `+0x10` = material res ptr (used by VIPM_SetMaterial)
