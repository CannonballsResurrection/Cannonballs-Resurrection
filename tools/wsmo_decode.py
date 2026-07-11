#!/usr/bin/env python3
"""
Decoder for the WildTangent `.wsmo` actor-motion format (SOLVED 2026-07-09).

A .wsmo is a WLD3 container (decode with WTExtractor first) holding one geMotion
(Genesis3D lineage): a named set of per-bone gePaths. Decoded layout:

  u32 0x078efc17 magic, u32 version(2), u32 0
  "TOMW" u32 1
  "MTNB" u32 0xF0, u16 nameLen(incl NUL), u8 1, u8 2, motion name (e.g. "loop")
  "SBKB" u32 boneCount, u32 nameBlobSize,
         boneCount * u32 name offsets (relative to blob start = right here),
         packed NUL-terminated bone names            <- the name blob
  boneCount * gePath, each:
      u32 tag = 0x20040426
      2 tracks (translation vec3 first, rotation quat second), each:
        u32 size, u16 flags, u16 interp(=1), u32 keyCount
        flags == 0:     f32 times[keyCount], then raw f32 keys
        flags == 0x200: f32 t0, f32 dt (uniform sampling), then raw f32 keys
      key stride: translation = vec3 (12B), rotation = quat WXYZ (16B)
      (stride is derived from size, never assumed)

Verified byte-exact (parse ends at EOF) on all four motions in the game:
CHEST/loop, CANNON/fire, LIGHTBEAM/loop, MENUS/WT/animation.

Usage: wsmo_decode.py file.wsmo out.json
"""
import json, struct, subprocess, sys

WT = "/tmp/cb/WTExtractor/pywttools/wtextract.py"


def parse_motion(d):
    # MTNB: u32 0xF0, u16 nameLen(incl NUL), u8 1, u8 2, name bytes
    m = d.find(b"MTNB")
    nlen = struct.unpack_from("<H", d, m + 8)[0]
    name = d[m + 12:m + 12 + nlen - 1].decode() if m >= 0 else "?"

    p = d.find(b"SBKB")
    if p < 0:
        raise ValueError("no SBKB bone block")
    cnt, blobsz = struct.unpack_from("<II", d, p + 4)
    blob = p + 12
    offs = struct.unpack_from(f"<{cnt}I", d, blob)
    names = [d[blob + o:d.index(b"\0", blob + o)].decode() for o in offs]

    bones, o, dur = {}, blob + blobsz, 0.0
    for i in range(cnt):
        tag = struct.unpack_from("<I", d, o)[0]
        if tag != 0x20040426:
            raise ValueError(f"path {i}: bad tag {tag:#x} at {o:#x}")
        o += 4
        tracks = []
        for _ in range(2):
            size, flags, interp, kc = struct.unpack_from("<IHHI", d, o)
            body = o + 12
            if flags & 0x200:                     # uniform-dt sampled track
                t0, dt = struct.unpack_from("<ff", d, body)
                times = [t0 + dt * k for k in range(kc)]
                stride = (size - 16) // kc
                kbase = body + 8
            else:                                 # explicit time array
                times = list(struct.unpack_from(f"<{kc}f", d, body))
                stride = (size - 8 - 4 * kc) // kc
                kbase = body + 4 * kc
            n = stride // 4
            keys = [list(struct.unpack_from(f"<{n}f", d, kbase + stride * k))
                    for k in range(kc)]
            tracks.append({"times": [round(t, 6) for t in times],
                           "keys": [[round(v, 6) for v in k] for k in keys],
                           "stride": n})
            dur = max(dur, times[-1])
            o += 4 + size
        tr = next((t for t in tracks if t["stride"] == 3), None)
        rt = next((t for t in tracks if t["stride"] == 4), None)
        bones[names[i]] = {
            "translation": {"times": tr["times"], "values": tr["keys"]} if tr else None,
            "rotation": {"times": rt["times"], "values": rt["keys"]} if rt else None,  # WXYZ
        }
    if o != len(d):
        raise ValueError(f"parse ended at {o:#x}, file is {len(d):#x}")
    return {"name": name, "duration": round(dur, 6), "bones": bones}


if __name__ == "__main__":
    src, out = sys.argv[1], sys.argv[2]
    if src.endswith(".wsmo"):
        subprocess.run([sys.executable, WT, src, "/tmp/_m.bin", "-q"], check=True)
        d = open("/tmp/_m.bin", "rb").read()
    else:
        d = open(src, "rb").read()
    motion = parse_motion(d)
    json.dump(motion, open(out, "w"), indent=1)
    kb = {b: (len(v['translation']['times']) if v['translation'] else 0,
              len(v['rotation']['times']) if v['rotation'] else 0)
          for b, v in motion["bones"].items()}
    print(f"'{motion['name']}' {motion['duration']}s, {len(kb)} bones: {kb}")
