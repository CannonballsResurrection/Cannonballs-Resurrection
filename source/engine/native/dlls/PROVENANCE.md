# Decoder binaries — provenance

These are the WildTangent WebDriver 3D runtime DLLs, **the native decoder for the
`.wsgo`/`.wsad` model format**. They were the single missing artifact that blocked
cracking the mesh geometry for multiple sessions.

## Where they came from
Extracted from a **Windows XP disk image** (`xp.img`) inside the user's 86Box VM at
`~/VMs/cannonballs-86box/xp.img` — the VM where the game was installed and launched.
The 3D codec is NOT in the game's own files and NOT in WebDriver 4.1's base installer
(that ships only image codecs; the 3D codec was an on-demand download from
now-dead WildTangent servers). It only exists on a machine where 3D WT content ran.

## How they were extracted (no booting needed)
The raw image has an NTFS partition at LBA 63. Read directly with **sleuthkit**:
```
fls -r -o 63 xp.img | grep -iE 'actorobject|wt3d|webdriver'   # find inodes
icat -o 63 xp.img <inode> > actorobject.dll                    # extract
```

## The files
- `actorobject.dll` (98KB) — **THE decoder**. WildTangent fork of the **Genesis3D**
  engine ("ge" prefixed C++ symbols, still present). Decode entry:
  `geGeometryRes::ReadData(geVFile*, geResourceIndex*)` at RVA 0x7125
  (ImageBase 0x66d00000). `BuildVipmsFromGeoChunkIdx` at 0x73ea proves the VIPM
  progressive-mesh reorder is BUILT AT LOAD TIME, not stored — so the file holds
  plain triangle geometry (this invalidates the earlier "unrecoverable reorder"
  dead end).
- `webdriver.dll` (712KB) — engine core; likely holds the geVFile primitives and
  the geGeometry_Chunk vertex reader.
- `dx5drv.dll`, `dx7drv.dll` — DirectX render drivers (rendering only; not needed
  to decode geometry).
- `wt3d.dll` — 59-byte legacy stub, kept for completeness. NOT the decoder.

Backup source installers also preserved by the user:
`~/Documents/Claude Code/Cannonballs.old/{WebDriverFullInstall.exe, Web Driver 2004 v4.1, WindowsXP_SP3.iso}`.

## UPDATE — WDENGINE.dll (the actual mesh-blob decoder) recovered
Disassembling `actorobject.dll` showed the per-material mesh (verts/faces/uvs/normals)
is read by `@VIPM_CreateFromFile@4`, which lives in **`WDENGINE.dll`** (imported by
actorobject/webdriver/dx7drv). Extracted from the same XP image (inode 11457/11477,
708608 bytes, has C++/stdcall symbols).

**This is the final piece.** Key exports (ImageBase 0x66200000):
- `@VIPM_CreateFromFile@4` @ RVA **0x6db55** — reads a serialized VIPM mesh from a geVFile.
- getters: `@VIPM_GetTriList@12`, `@VIPM_GetVertexBuffer@4`, `@VIPM_GetNormals@4`.
- packing primitives it uses: `@geVFileUtil_ReadFloatArray16@24`, `_2@16`,
  `_LZ@24`, `_LZ2@16` — **16-bit quantized float arrays, some LZ-compressed.** The
  vertex/normal/uv data is stored this way. Disassemble `VIPM_CreateFromFile` +
  these readers to get the exact element order + dequant math, then reimplement.
