# Extracting the WildTangent NSIS installer payload

The WildTangent installers (`Cannonballs 1.0.exe`, `cannonballs-setup.exe`) are **Nullsoft (NSIS)
self-extracting archives** from ~2002, bundling the WLD3 runtime (`WLD3.cab`) and the game files.

Modern 7-Zip / p7zip can't decode this old NSIS script — they just expose the raw payload blob `[0]`.
`tools/nsis_extract.py` decodes it directly: the payload is a sequence of `[uint32 size][deflate data]`
blocks (high bit of the size = compressed). The script raw-inflates each block (`zlib.decompress(d, -15)`)
and dumps all of them (~511 files for Cannonballs): the launcher exe, params.dll, the WLD3 CAB, the
HTML/JS launcher UI, and the `.wt`/`.wpg`/`.wwv` game assets.

Usage (point it at the `[0]` blob you extracted with 7-Zip, or adapt to read the .exe overlay):
```sh
python3 tools/nsis_extract.py
```

This is only needed for inspection/forensics. To actually play, install the game normally in Windows
or a VM — you don't need to hand-extract anything.

## File formats seen (WildTangent WLD3)
- `.wt` — compressed 3D mesh (from DirectX `.x`), sometimes a plain ZIP internally
- `.wpg`/`.wjp` — compressed PNG/JPG
- `.wwv` — ADPCM audio
- `.wsad/.wsgo/.wsmo/.wsbm` — actor/mesh/motion/material exports
- `WLD3.cab` — the WildTangent 3D runtime, MS Cabinet
