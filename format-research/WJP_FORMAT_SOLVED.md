# `.wjp` format — SOLVED

**TL;DR: `.wjp` is not a novel codec. It is a WLD3 container wrapping a plain
JPEG (EncodeType 200). The WTExtractor container peel already yields a complete,
standard JFIF/JPEG file. No wavelet codec had to be cracked.**

## What we expected vs. what it is

The roadmap feared `.wjp` was WildTangent's proprietary **WJ2 wavelet** image
codec (the `wtImageWJ20100.dll` / `cmCreateImageWJ2Serializer` path), analogous
to how the `.wsgo` mesh format needed the VIPM reader disassembled.

It isn't. Cannonballs' menu `.wjp` files use **EncodeType 200**, which is the
container's "store a normal image" path. The body chunk is a byte-for-byte
standard JPEG:

```
$ wtextract.py MAIN_SCREEN/image.wjp out.jpg
FTYPE: jpg   EncodeType: 200   BodyMarker: .BODY
$ xxd out.jpg | head -1
00000000: ffd8 ffe0 0010 4a46 4946 ...   # FF D8 FF E0  JFIF  (+ "Ducky"/"Adobe")
```

The outer file even self-describes: its header text is
`WLD3.jpg WildTangent 3D 300 Compressed and Patented / Converted by XtoWT`. The
`.jpg` in `WLD3.jpg` is the original source type; XtoWT wrapped the JPEG in the
WT resource container (UUIDs, license, expiry, `.START`/`.BODY` sections) without
re-encoding the pixels. (EncodeType **300** would be the true WJ2 wavelet path;
Cannonballs does not use it.)

## Decode recipe

```
for f in MEDIA/MENUS/*/*.wjp; do
  python WTExtractor/pywttools/wtextract.py "$f" out.jpg -q   # peels container -> JPEG
done
# then any JPEG decoder (PIL/ImageIO) -> PNG
```

## The four menu backdrops (all 800×600)

| File | Screen |
|---|---|
| `MENUS/MAIN_SCREEN/image.wjp`    | Main menu (Cannonballs! logo + tropical island + skull dubloon) |
| `MENUS/LOBBY_SCREEN/image.wjp`   | Lobby / game-setup island |
| `MENUS/LOBBY_SCREEN/join.wjp`    | Join screen (volcano island) |
| `MENUS/RESULTS_SCREEN/image.wjp` | Results screen (logo on stormy sky) |

Decoded PNGs live in `decoded-assets/menus/`. The clone bundles them under
`Resources/MENUS/` and the main-menu scene now uses `MAIN.png` as its backdrop.
