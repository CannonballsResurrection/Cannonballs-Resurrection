# raw — original inputs (provenance root)

The unmodified source material everything else is derived from. © WildTangent /
Gamigo; kept here for preservation and to make the pipeline reproducible. See
[`../LEGAL.md`](../LEGAL.md).

| File | What it is |
|---|---|
| `cannonballs-setup.exe` | The original **Cannonballs!** installer (NSIS). The distributable product; top of the pipeline. |
| `game.jar` | The compiled game, extracted from the install. CFR-decompiling this produced [`../source/`](../source/). |
| `game-media/` | The extracted `MEDIA/` asset tree (maps, props, images, audio) in WildTangent's encoded formats — the inputs the decoders in [`../tools/`](../tools/) consume. |

### Checksums (SHA-256)

```
game.jar               c9a5d9985f8216ecc9b2fb4523d04811101d39b5ce6a65d1370f3e043ed77745
cannonballs-setup.exe  c47b7f84f6bf58c1c920bbab035810096907a5c76fa547e0d45b95683aa834e7
```

`game-media/` holds 131 encoded originals (`.wwv` audio, `.wsgo` meshes, `.wjp`
images, plus textures/maps/props) — decode them with the scripts in `../tools/`.
