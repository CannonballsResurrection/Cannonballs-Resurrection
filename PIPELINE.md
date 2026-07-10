# The full pipeline

End-to-end, from the original commercial installer to playable native
builds — every stage lives in this repo.

```
raw/cannonballs-setup.exe              original installer (NSIS)
        │  NSIS extract  (tools/, third-party/WTExtractor)
        ▼
raw/game.jar  +  raw/game-media/       compiled game + encoded assets
        │                     │
 CFR decompile          WLD3 decode  (tools/ + third-party/WTExtractor/libwld3)
 (cfr.jar)                    │
        ▼                     ▼
source/  ── imports ──►  decoded-assets/ + shared/Resources/
source/engine/            format-research/     readable game logic + decoded
        │                     │                assets (how each format works)
        └──────── understanding ───────┐
                                        ▼
                          macos/  +  windows/   downstream rebuilds running on
                                                the shared decoded assets
```

## Stages

1. **Installer → files.** `raw/cannonballs-setup.exe` is an NSIS installer. The
   extractor (`tools/`, backed by `third-party/WTExtractor`) unpacks it into
   `raw/game.jar` and the `raw/game-media/` asset tree.
2. **Bytecode → source.** `raw/game.jar` is decompiled with CFR `0.152` into
   [`source/`](source/) (84 game classes). The engine API it calls lives in
   [`source/engine/`](source/engine/); the native engine is in
   [`source/engine/native/dlls/`](source/engine/native/dlls/).
3. **Encoded assets → usable data.** The WildTangent formats in `raw/game-media/`
   are cracked by the scripts in [`tools/`](tools/) (documented in
   [`format-research/`](format-research/)):
   - `.wwv` audio → WAV (`tools/wwv2wav.py`)
   - `.wsgo` meshes → OBJ (`tools/wsgo_decode_SOLVED.py`)
   - `.wjp` images → PNG (WLD3 container → JPEG)
   - textures / maps / props → PNG / heightmaps
   Masters land in [`decoded-assets/`](decoded-assets/); the game-ready tree the
   builds consume lands in [`shared/Resources/`](shared/Resources/) (the export
   tools write there directly).
4. **Understanding → rebuilds.** The build folders reimplement the game logic
   from `source/`, running on `shared/Resources/`: [`macos/`](macos/) (Swift/
   SceneKit, playable) and [`windows/`](windows/) (reserved). They're the
   downstream proof that stages 1–3 are complete and correct.

## Reproducing it

- Decode one asset: `python tools/wwv2wav.py raw/game-media/MUSIC/TITLE/sound.wwv out.wav`
- Re-decompile: `java -jar cfr.jar game.jar --outputdir source/` (CFR is a free,
  separately-downloaded tool: https://www.benf.org/other/cfr/)
- Build the macOS rebuild: `cd macos && swift run -c release`
- Package the macOS app: `macos/tools/package_app.sh`
