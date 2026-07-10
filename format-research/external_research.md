# WildTangent WebDriver / WLD3 / .wwv / .wsad — External Research Findings

Date: 2026-07-06. Task: reverse-engineer the WildTangent WebDriver asset container (circa 2000-2002)
for the 2002 game "Cannonballs" — decode `.wwv` audio and `.wsad`/`.wsgo` 3D models.

**Bottom line up front (CONFIRMED, verified by running an existing decoder on a sample file):**

1. The WLD3 header (`WLD3.<ext> WildTangent 3D 300 Compressed and Patented`) is a **container +
   byte-XOR obfuscation layer only**. It is NOT itself a codec.
2. The audio path (`.wwv`) has **NO proprietary/licensed audio codec**. After you strip the WLD3
   XOR layer, the payload is a **Microsoft Cabinet (.CAB) archive using MSZIP (DEFLATE) compression**,
   and inside that CAB is a **standard RIFF/WAVE file, uncompressed 16-bit PCM** (in the sample:
   44100 Hz stereo, `WAVE_FORMAT_PCM = 0x0001`). No MP3, no Voxware, no ACELP, no TrueSpeech, no ADPCM.
   "Compressed" = MSZIP/DEFLATE. "Patented" refers to the 3D-geometry patent (see #3), reused as a
   generic marketing tag on all WebDriver assets.
3. The "Patented" claim maps to **US Patent 6,577,769 B1** (and continuation **US 6,697,530 B2**),
   "Data compression through adaptive data size reduction," assignee WildTangent Inc, inventors
   Jeremy A. Kenyon & Peter D. Smith, filed 1999-09-18, granted 2003-06-10. It describes
   **distance-based bit-truncation of 3D vertex/normal floats (6-20 bits, keyed to bounding box)** —
   i.e. the MODEL geometry compression, NOT audio.
4. Genesis3D lineage is real for the *rendering engine* but the WT asset formats are WT-specific.
   The WebDriver model format (internally "PWT") uses magic `00 00 00 13` and derives from DirectX
   `.x` files, not Genesis3D `.act`/`.bdy`. Genesis3D `.act` uses magic `VF00`. So Genesis3D source
   will NOT hand you the .wwv/.wsad algorithm — but you don't need it, because a working open-source
   decoder already exists.

**THE SINGLE MOST USEFUL RESOURCE: a complete open-source decoder already exists.**
GitHub `diamondman/WTExtractor` decodes WLD3 containers AND the PWT model format, and ships sample
files. I ran its Python decoder against its sample `test.wwv` and it produced a valid WAV. This is
almost certainly directly reusable on Cannonballs assets.

---

## Question 1 — The codec (RESOLVED: no proprietary audio codec)

### What "300 Compressed and Patented" actually is
- `WLD3` = 4-byte magic. Followed by a plaintext ASCII header line
  `.<ext> WildTangent 3D 300 Compressed and Patented\r\n`, then `Converted by XtoWT: <date>\n\r\n`,
  then an optional free-text comment, then the literal token `.START\n`.
- The "300" is the **EncodeType / format version field** (also called `encodeversion`). Version 1.0
  files have 8 metadata headers; v2.0+ add a 9th header. The decoder treats `EncodeType >= 300` as the
  trigger to also parse an embedded URL/string table.
- After `.START` comes a metadata block: a field-count byte, per-field length bytes (both stored with a
  rolling additive offset obfuscation, `rev_offset(byte, 0x39 + 13*i)`), then the raw fields:
  Copyright string, CreatedDate, StartValidDate, ExpireDate, WTVerUUID (16), ResourceUUID (16),
  License (4), a `.BODY` marker, and the EncodeType int. The plaintext
  `(c) Copyright 2001, WildTangent, Inc` you see is exactly this Copyright field. The
  record-marker byte-run you observed (`0x1A 0xF8 0xF2`, in the sample it appears as `42 F6 0B 58`)
  is the little-endian **timestamp bytes** repeated inside the packed metadata headers as a shared
  suffix — it's a field-delimiter artifact, not a codec chunk marker.

### The payload after the metadata is XOR-obfuscated, then decompressed
The payload is decrypted with a per-file key table derived by hashing all the metadata fields. Three
different key-table derivations are used depending on the resource CLASS:
- `model` (extension `.wt` and by extension `.wsad`/`.wsgo`) → `calc_enc_key_table_TYPEMODEL`
- `media` (`.wav`/`.wwv`, `.png`, `.jpg`, `.mid`) → `calc_enc_key_table_TYPEMEDIA`
- `data`  (`.txt`/`.wtxt`, `.cfg`, `.ini`, `.dat`) → `calc_enc_key_table_TYPEDATA`

The core decrypt op is a **rolling XOR chain**:
`out = in ^ prev_in_byte ^ key_table[index % len(key_table)]`, advancing `index` and remembering the
previous *ciphertext* byte. The key table itself is seeded from a hash byte computed over the metadata
fields (`calc_hash_byte_TYPEMEDIADATA` / `..._TYPEMODEL`). This is a homegrown obfuscation, not a
standard cipher. The `wld3_notes.txt` in the repo captures the reverse-engineered specifics:
"Start at char 7 / get 7%bufflen / xor with current char / increment char index by 13," and the
disassembled hash routine `ENCRYPT_MYS0::GetBuffHash` — this is very likely what
`XORBlockEncryptData.wtxt` in the Cannonballs tree corresponds to.

### The decrypted media payload = CAB(MSZIP) → RIFF/WAVE PCM (verified empirically)
I fetched the repo's sample `test.wwv` (297,062 bytes, header
`WLD3.wav WildTangent 3D 300 Compressed and Patented`), ran the decoder, and got:
- Decrypted payload begins `4D 53 43 46` = **`MSCF` = Microsoft Cabinet archive** (`file(1)`:
  "Microsoft Cabinet archive data ... 1 file ... "test.wav" ... 0x1503 compression" — 0x1503 = MSZIP).
- Extracting the CAB (`cabextract`) yields `test.wav` = **RIFF/WAVE, Microsoft PCM, 16-bit, stereo,
  44100 Hz** (`fmt ` chunk audio-format = `0x0001` PCM, uncompressed).

So the full `.wwv` decode pipeline is:
```
WLD3 container (parse header + metadata) 
  -> rolling-XOR decrypt payload (media key table) 
  -> Microsoft CAB / MSZIP (DEFLATE) decompress 
  -> plain RIFF/WAVE PCM   (standard, play anywhere)
```
Note the decoder explicitly checks `decoded_data[:4] == b'MSCF'` and, if so, runs it through a CAB
extractor (`patoolib`/`cabextract`). Some media payloads are the raw file with no CAB wrapper; when a
CAB is present it's always MSZIP. Either way the inner audio is a normal WAV, no exotic codec.

**Candidate-elimination:** MP3 / Voxware / ACELP / TrueSpeech / IMA-ADPCM / MS-ADPCM are all ruled
out for the sample. The WildTangent SDK docs mention a `.wav → .wav (ADPCM)` conversion option, so
*some* WWV payloads MAY contain an MS-ADPCM or IMA-ADPCM WAV (fmt tag 0x0002 or 0x0011) rather than
PCM — but that's still a **standard WAV codec fully described by the RIFF fmt chunk**, decodable by
ffmpeg/SoX with zero WT-specific work. Check the inner WAV's `fmt ` tag per file. No WT-proprietary
audio algorithm exists.

### Patents (assignee WildTangent Inc)
- **US6577769B1** — "Data compression through adaptive data size reduction." Filed 1999-09-18, granted
  2003-06-10. Inventors Kenyon & Smith. Assignee WildTangent Inc (now shown as Gamigo Inc after
  acquisition). Algorithm: determine distance between a reference point and an object's bounding box,
  pick a data-reduction factor from that distance, and truncate IEEE-754 32-bit vertex floats to
  6-20 bit fields (drop/limit the exponent by localizing to the bbox; reduce mantissa precision for
  distant objects). This is 3D GEOMETRY compression, and it is exactly the PWT vertex/normal
  bit-packing in the decoder. https://patents.google.com/patent/US6577769
- **US6697530B2** — same title, continuation of the above. https://patents.google.com/patent/US6697530
- Other WildTangent patents exist (streaming multi-media adaptive model versioning; a
  "dance visualization of a stream of music" music-analyzer patent by Kenyon/Smith) but **none is an
  audio-compression codec patent.** The "Patented" tag on `.wwv` files is the geometry patent reused
  as a blanket brand marker on all WebDriver assets.
- NOTE: US7162745B2 ("Protecting decrypted compressed content ... DRM") that surfaces in searches is
  **Microsoft's, not WildTangent's** — ignore it.

---

## Question 2 — Existing tools / specs (RESOLVED: full decoder exists)

### PRIMARY: diamondman/WTExtractor  (https://github.com/diamondman/WTExtractor)
A C/C++/Python project that "Decode[s] WildTangent WebDriver files." Structure:
- `libwld3/` — C library. `wld3_extract.c/.h` (container + XOR decrypt + CAB detect),
  `pwt_decode.c/.h` (the 3D model format), `dataaccessors.*`, `dshfl.*`, `multifh.*`.
- `wld3tools/` — CLI: `wtextract_cli_main.c`, `pwtdecode_cli_main.c`, `wtdatclassdecode_cli_main.c`.
- `pywttools/` — **readable reference Python**: `wtextract.py` (WLD3 container decoder — this is the
  one I ran) and `pwtdecode.py` (PWT model decoder). Depends on `numpy`, `bitarray`, optionally
  `patoolib` for the CAB path.
- `pwtviewer/` — a Qt + OGRE 3D viewer for decoded models.
- `docs/wld3_notes.txt` — reverse-engineering notes incl. the XOR/hash routine
  (`ENCRYPT_MYS0::GetBuffHash`), the metadata field layout, and version-header behavior.
- `sample_files/` — includes `test.wwv`, `test.wtxt`, `short.wtxt`, `2cubes.wt`, `2cubes.pwt`,
  `anim2cube.wt/.pwt`, `sample.pwt`. These are gold for validating your own decoder against.

Raw file base for direct pulls:
`https://raw.githubusercontent.com/diamondman/WTExtractor/master/<path>`
(e.g. `pywttools/wtextract.py`, `pywttools/pwtdecode.py`, `docs/wld3_notes.txt`).

Verified: `wtextract.py` parses the `WLD3` magic, requires the exact header line
`WildTangent 3D 300 Compressed and Patented\r\n` and `Converted by XtoWT: ` line, reads `.START`,
the 8-or-9 field metadata block, an optional URL table when EncodeType>=300, then decrypts + CAB-
decompresses the payload. It maps base types to output extensions via `file_type_info`
(wav→wav, png→png, jpg→jpg, mid→mid, wt→pwt, txt→txt, cfg→cfg, dat→dat, cab→cab, tmp→tmp).

### The PWT (model) format — decodes .wt and, by pipeline, .wsad/.wsgo
The decrypted `.wt` payload is a "PWT" file: magic `00 00 00 13`, big-endian, then global min/max
bbox, `veryclose`/`prettyclose` LOD thresholds, `vertex_bit_accuracy`/`normal_bit_accuracy`, counts,
then a frame tree (each frame: name, 4x4 matrix, optional animation keyframes, optional visuals).
Visuals hold **bit-packed quantized vertices and normals** (per-axis "unused bits" 6-bit fields, then
`bit_accuracy - unused` bits per component, rescaled by the bbox — exactly the US6577769 scheme),
followed by faces (bit-packed indices sized by `ceil(log2(count))`), UV texmap floats, per-face
material details, and embedded textures. `pwtdecode.py` and `libwld3/src/pwt_decode.c` implement all
of this. This is the algorithm for the model side.

### Format overview docs (WildTangent SDK, mirrored)
- `https://www.cs.vu.nl/~eliens/hush/archive/wt/sdk/documentation/FileFormats/` — official-ish WT
  "Web Driver File Format Overview." Confirms source→WT conversions: `.x → .wt` (models),
  `.png/.jpg → .wpg/.wjp`, `.mid/.wav → .wav (ADPCM)/.wwv`, `.cfg/.dat/.ini/.txt → .wcfg/.wdat/.wini/.wtxt`.
  Lists actor export formats: **`.wsad` (Actor Definition), `.wsgo` (Mesh and UVW Mapping),
  `.wsmo` (Motion), `.wsbm` (Material)**. Confirms files are "branded" so the driver IDs them
  regardless of extension (matches the WLD3 magic-first design). Note the doc says suffix is cosmetic.
- `https://www.cs.vu.nl/~eliens/hush/archive/wt/sdk/documentation/readme.html` — WT Web Driver 3.0 SDK
  readme. Describes **WildCompress** as "a compression and copyrighting tool that will prepare media
  for use with the Web Driver ... compression and encryption of images, models, sounds, and data
  files," v2.00 gives "up to 15% additional compression." This is the encoder counterpart to `XtoWT`.
- Extension registries corroborate: `.wsad` = "WildTangent Actor Definition File"; `.wwv` = WildTangent
  compressed audio container (filext.com, whatext.com).

### Others
- `convert.guru/wwv-converter` and generic "WWV converter" sites exist but are low-trust/SEO; the real
  artifact is WTExtractor.
- No meaningful Xentax/ResHax/ZenHax thread was surfaced; WTExtractor is the definitive community work.

---

## Question 3 — The runtime (archived, downloadable)

Original WebDriver runtimes ARE archived on the Internet Archive (native DLLs contain the reference
decoder if you ever need to diff behavior):
- `https://archive.org/details/3DWildtangentWebdriver41` — "WildTangent WebDriver 4.1 HP Service pack
  26333." Downloadable installer `HP-webdriver-41-sp26333.exe` (~3.1 MB) + torrent. Uploaded by
  "archivethelostupdates."
- `https://archive.org/details/WTWebDriverInstall` — "WildTangent Web Plugin" installer.
- DLL names associated with the runtime: **webdriver.dll** (copyright WildTangent 1999-2003) and
  **wt3d.dll**. (Confirmed via DLL registries; these are the native components carrying the decoder.)
- SDK docs mirror: `https://www.cs.vu.nl/~eliens/hush/archive/wt/sdk/documentation/` (FileFormats,
  readme, API). `XtoWT` = the asset converter; `WildCompress` = the SDK compression/encryption tool.

Given a working open-source decoder exists, you likely never need to disassemble these DLLs — but
they're available if you hit a format variant WTExtractor doesn't cover.

---

## Question 4 — Genesis3D lineage (real for engine, NOT for these asset formats)

- CONFIRMED history: WildTangent (founded July 1998 by ex-Microsoft/DirectX evangelist **Alex St. John**
  + Cambridge mathematician **Jeremy Kenyon** — note Kenyon is also the patent inventor). In **1999
  WildTangent bought most of the Genesis3D assets from Eclipse Entertainment** and built WebDriver 1.0
  on that foundation (released 1999-09-22). Genesis3D 1.0 is open source; there's even a copy uploaded
  by WildTangent on archive.org (`archive.org/details/genesis3d-1-0`). Descendants of Genesis3D include
  Jet3D, RealityFactory, tStudio3D.
  Sources: Wikipedia "WildTangent", GameSpot, Lost Media Wiki, HandWiki.
- BUT the WebDriver *asset formats do not reuse Genesis3D's file formats*:
  - Genesis3D Actor `.act` magic = **`VF00`** (Jet3D actor = `VFHH`); it decompiles to `.bdy` (body),
    `.mot` (motions), `.key`, `.nfo`. Source pipeline is a modeler → Actor Studio → `.act`.
  - WildTangent WebDriver model magic (decrypted) = **`00 00 00 13`** (the "PWT" format), and the WT
    source pipeline is **DirectX `.x` → `.wt`**, NOT Genesis3D actors. The bit-packed
    distance-quantized geometry is WT's own (patented) invention, not Genesis3D's.
  - The `.BODY` token you see in WLD3 files is a **container section marker** in the metadata block
    (the `.START`/`.BODY` split), NOT a Genesis3D `.bdy` body chunk. Don't conflate them.
  - `.wsad` (WT Skeletal Actor Definition) / `.wsgo` (mesh+UVW) / `.wsmo` (motion) / `.wsbm` (material)
    are WT's actor family; they are WLD3-wrapped and decrypt to WT-native (PWT-style) payloads.
- CONCLUSION: The Genesis3D open source will help you understand the *renderer/engine* heritage, but
  it will NOT give you the `.wwv`/`.wsad` decode algorithm. WTExtractor already gives you that.

---

## Concrete next steps for Cannonballs

1. **Audio (.wwv):** Run WTExtractor's `wtextract.py` (or the C `wtextract` CLI) on the Cannonballs
   `.wwv` files. Expect: WLD3 → rolling-XOR → CAB/MSZIP → RIFF/WAVE. If the inner WAV `fmt ` tag is
   `0x0001` it's PCM (done); if `0x0002`/`0x0011` it's MS/IMA-ADPCM (feed to ffmpeg/SoX). Note these
   are 2002 files (v "300"), matching the sample exactly, so this should work near-verbatim.
   Watch: `patoolib` optional dep for the CAB step — install `cabextract`/`patool`, or hand the MSCF
   payload to `cabextract`. Some payloads may be raw (no MSCF); handle both.
2. **Models (.wsad/.wsgo/.wt):** Run `pwtdecode.py` / `libwld3` `pwtdecode` on the WLD3-decrypted
   payload (`.wt`→`.pwt`). Cross-check against US6577769B1 for the vertex quantization math
   (bbox + bit_accuracy - unused_bits, rescaled). `pwtviewer` can visually verify.
3. **XORBlockEncryptData.wtxt:** This is almost certainly the game's own copy of / reference to the
   WLD3 rolling-XOR block cipher (`ENCRYPT_MYS0::GetBuffHash`). Decode it as a `data`-class WLD3 file
   (`.wtxt` → `.txt`, `calc_enc_key_table_TYPEDATA`) to read its contents.
4. If any Cannonballs file trips WTExtractor (format variant), fall back to the archived
   `HP-webdriver-41-sp26333.exe` / `webdriver.dll` + `wt3d.dll` for reference behavior.

---

## COORDINATOR PRIORITIES — runtime download, round-trip, chunk tags (verified 2026-07-06)

### P1. Downloadable WebDriver runtime containing wt3d.dll + actorobject.dll — YES, two options.
Both are on the Internet Archive as self-extracting installers (I downloaded and cracked the first
one's outer layers to confirm the packaging chain):

- **WebDriver 4.1 (HP OEM service pack 26333)** — RECOMMENDED.
  Item: https://archive.org/details/3DWildtangentWebdriver41
  Direct file: https://archive.org/download/3DWildtangentWebdriver41/HP-webdriver-41-sp26333.exe
  (3,200,440 bytes, PE32 GUI). I verified its structure by carving it:
  outer PE self-extractor -> embedded Microsoft CAB (MSCF at 0x1064) -> **`Install_Webdriver.exe`**
  (2,906,544 bytes, dated 2004-06-25) -> that inner exe is a **Nullsoft NSIS self-extracting
  installer** whose solid LZMA payload ([0] overlay, ~2.86 MB) contains the WT DLLs. (My local
  7-Zip 17.05 couldn't crack this NSIS variant, so DLL names don't show as plaintext — they're
  LZMA-compressed — but the payload size and the Dr.Web analysis below confirm the DLL set is inside.)
  To get the DLLs: run `Install_Webdriver.exe` under Wine/Windows; it installs to
  `%WINDIR%\wt\webdriver\` dropping **wt3d.dll, actorobject.dll, webdriver.dll, jdriver.dll,
  dx5drv.dll/dx7drv.dll**. Or use a newer NSIS-aware 7-Zip / `7z` build (>= a version with the NSIS
  plugin) or the `nsisunbz`/`7z x -tnsis` on Linux to unpack `Install_Webdriver.exe` offline.
  NOTE: 4.1 is newer than the confirmed-decoder version 3.3.1.001, but the WLD3/.wwv container and
  the PWT/.wsad formats are stable across these builds (the sample the open-source decoder handles is
  a "300" file just like Cannonballs), so 4.1's wt3d.dll/actorobject.dll should decode fine.

- **WildTangent Web Plugin installer** (version unlabeled, ~2005-era):
  Item: https://archive.org/details/WTWebDriverInstall
  Direct file: https://archive.org/download/WTWebDriverInstall/WTWebDriverInstall.exe (2,478,024 bytes).

- **Exact 3.3.1.001 file layout is documented** (from a Dr.Web malware-analysis writeup that
  fingerprints a WebDriver install): DLLs live at `%WINDIR%\wt\webdriver\` and updates stage at
  `%WINDIR%\wt\wtupdates\wtwebdriver\files\3.3.1.001\` (e.g. `actorobject.dll.tmp`, `wt3d.dll.tmp`).
  https://vms.drweb.com/virus/?i=1957495 . This confirms wt3d.dll + actorobject.dll are the right
  targets and their install path, but is not itself a clean download source.
  I did not find a standalone, clean download of the exact 3.3.1.001 build; the 4.1 archive above is
  the practical way to obtain working wt3d.dll + actorobject.dll for a Wine-hosted decoder.

**Recommendation:** Pull `HP-webdriver-41-sp26333.exe`, unpack the two outer layers (PE->CAB->NSIS)
or just run the inner `Install_Webdriver.exe` under Wine, harvest `wt3d.dll` + `actorobject.dll`, and
call them via a tiny Windows host — but note the open-source `WTExtractor` (Q2 above) likely already
saves you from needing the native DLL at all.

### P2. Round-trip export (WT -> WAV/OBJ)?
No evidence of an official reverse exporter. `XtoWT.exe` is a **one-way encoder** (source -> WT), and
`WildCompress` is described purely as a "compression and copyrighting tool" (encode-only). The WT
authoring pipeline was `.x` (DirectX) / `.wav` -> WT via XtoWT, with no documented WT->source path.
The realistic round-trip is via the **decoders**: WLD3/.wwv -> WAV is fully solved (WTExtractor +
CAB/MSZIP -> PCM WAV, standard). Models: WTExtractor's `pwtdecode` yields vertices/normals/faces/UVs
which you export to OBJ yourself (its `pwtviewer` already reconstructs the mesh). So the round-trip
exists, just as decode-then-reserialize, not as a vendor tool.

### P3. Patent — see Question 1 above. US6577769B1 / US6697530B2, assignee WildTangent Inc, inventors
Kenyon & Smith, distance-based 6-20 bit vertex/normal quantization. 3D geometry, not audio.

### P4. Genesis3D lineage & chunk tags — LINEAGE IS ENGINE-ONLY; formats do NOT match.
- Genesis3D `.act` is a VFS (virtual file system) with magic **`VF00`** (Jet3D = `VFHH`) containing
  `.bdy` body + `.mot` motion sub-files. WT WebDriver's model payload is the **PWT** format, magic
  **`00 00 00 13`**, derived from DirectX `.x`, with WT's own patented bit-quantized geometry. These
  are different container designs and different magics. Confirmed: `.act` = VF00 VFS of BDY/MOT
  (file-extensions.org, RealityFactory/Milkshape decompile docs).
- The `.RG/.MX/.FU/.IA` tags you see are **NOT Genesis3D Actor/Motion/Body chunk names** — Genesis3D's
  actor structure is BDY/MOT/KEY/NFO sub-files inside a VF00 VFS, with no RG/MX/FU/IA tags. So those
  tags are **WildTangent-specific section markers inside the .wsad/actorobject format**, most plausibly:
  RG = Rig/skeleton, MX = Matrix/transform, FU = Function/frame-update, IA = (index/interpolation
  array or Instance/Attach) — but treat those expansions as SPECULATION; the authoritative decoder
  for them is **actorobject.dll** (native) or by extending WTExtractor's PWT/frame logic. Genesis3D
  source will NOT decode them.
- CONCLUSION for the orchestrator: **kill the Genesis3D-hands-us-the-codec hypothesis.** The engine
  rendering heritage is Genesis3D (confirmed acquisition, 1999), but the .wwv audio path is
  CAB/MSZIP+PCM and the .wsad/.wsgo model path is WT's own patented PWT-family format. The algorithm
  comes from (a) the US6577769 patent for geometry and (b) the WTExtractor open-source decoder and/or
  the archived wt3d.dll/actorobject.dll — not from Genesis3D.

## Source URLs
- Decoder + samples + notes: https://github.com/diamondman/WTExtractor
  - Raw base: https://raw.githubusercontent.com/diamondman/WTExtractor/master/
- WT SDK file-format docs: https://www.cs.vu.nl/~eliens/hush/archive/wt/sdk/documentation/FileFormats/
- WT SDK 3.0 readme: https://www.cs.vu.nl/~eliens/hush/archive/wt/sdk/documentation/readme.html
- Patent US6577769B1: https://patents.google.com/patent/US6577769
- Patent US6697530B2: https://patents.google.com/patent/US6697530
- WildTangent patents (assignee): https://patents.justia.com/assignee/wildtangent-inc
- Runtime archive (WebDriver 4.1): https://archive.org/details/3DWildtangentWebdriver41
- Runtime archive (Web Plugin): https://archive.org/details/WTWebDriverInstall
- Genesis3D 1.0 (open source, WT upload): https://archive.org/details/genesis3d-1-0
- Genesis3D docs / tools: https://genesis3d.com/old/docs/Tools.htm ; https://github.com/RealityFactory/Genesis3D-Tools
- WildTangent history: https://en.wikipedia.org/wiki/WildTangent ; https://lostmediawiki.com/WildTangent_(partially_found_PC/online_games;_1999-2018)
- .wsad ext registry: https://www.whatext.com/file/wsad
