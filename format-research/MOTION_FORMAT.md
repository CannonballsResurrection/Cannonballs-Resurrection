# Actor motion (`.wsmo`) — SOLVED (2026-07-09)

WildTangent actors animate via baked skeletal motion: a `.wsmo` is a WLD3
container holding one **geMotion** (Genesis3D lineage) = a named set of per-bone
`gePath`s. Decoder: `tools/wsmo_decode.py` (byte-exact on all four motions in
the game). Playback: `macos/.../SkinnedModel.swift`.

## The game's four motions

| File | Motion | Bones | Duration | What it is |
|---|---|---|---|---|
| `OBJECTS/CHEST/resources/loop.wsmo` | `loop` | 6 | 1.5s | chest hop + lid rattle |
| `OBJECTS/CANNON/resources/fire.wsmo` | `fire` | 9 | 0.73s | barrel recoil squash (plays per shot) |
| `OBJECTS/LIGHTBEAM/resources/loop.wsmo` | `loop` | 1 | 13.3s | lighthouse beam sweep (400 keys) |
| `MENUS/WT/resources/animation.wsmo` | `animation` | 9 | 3.0s | WildTangent splash (not used in clone) |

## Decoded layout

```
u32 0x078efc17, u32 version(2), u32 0
"TOMW" u32 1
"MTNB" u32 0xF0, u16 nameLen(incl NUL), u8 1, u8 2, motion name
"SBKB" u32 boneCount, u32 nameBlobSize,
       boneCount×u32 name offsets (relative to blob start = the offsets array),
       packed NUL-terminated bone names
boneCount × gePath:
    u32 tag 0x20040426
    2 tracks (translation vec3 first, rotation quat second), each:
        u32 size, u16 flags, u16 interp(=1), u32 keyCount
        flags==0    : f32 times[keyCount], then raw f32 keys
        flags==0x200: f32 t0, f32 dt (uniform sampling), then raw f32 keys
    key stride derived from size: 12 = vec3 translation, 16 = quat WXYZ
```

Quats are **WXYZ**, |q|=1. Static tracks are 2 keys (t=0, t=dur) of the same
value. Motion values are **relative to the bind pose**: k0 ≈ identity/zero.

## Composition (how motion drives bones) — from Genesis3D pose.c:256

```
World(bone,t) = World(parent,t) × BindLocal(bone) × MotionSample(bone,t)
```

BindLocal comes from the geom skeleton: `inv(worldBind[parent]) · worldBind[bone]`
where `worldBind = inv(inverse-bind matrix table)` in the geom header (see
`GEOM_MESH_FORMAT_SOLVED.md` §Skeleton). In SceneKit we mirror this with a
static bindNode → animated motionNode pair per bone; verified by seam-
disagreement metric (dual-weighted verts) and by rendering: bind pose at t=0 is
pixel-identical to the static mesh, and the chest/cannon/lightbeam animate
coherently (`/tmp/cb/skin_strip.png`, `barrel_t015.png`).

## Pipeline

1. `tools/wsmo_decode.py in.wsmo out.json` → per-bone translation/rotation tracks.
2. `tools/wsgo_export_skinned.py actor.wsgo skinned.json` → mesh + skeleton + skin.
3. `SkinnedModel.load/loadMotion/animate/playOnce/pose` in the clone.

Shipped: CHEST loop (looping, random phase), CANNON fire (playOnce on every
shot), LIGHTBEAM loop (looping sweep on Moonlight Cove).
