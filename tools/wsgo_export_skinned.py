#!/usr/bin/env python3
"""
Skinned-actor exporter for WildTangent `.wsgo` geoms (format 100% solved 2026-07-09
by disassembling WDENGINE.dll's VIPM_CreateFromFile / ReadPackedLVertArray2 /
geVFileUtil_ReadFloatArray16 — see format-research/GEOM_MESH_FORMAT_SOLVED.md).

Geom container (after WLD3 container decode):
  0x0C "moeg", 0x10 version, 0x14 A=chunkCount, 0x18 B, 0x1C C=materialCount,
  0x20 5×u32, 0x34 A×64B inverse-bind matrices (3x4 rows padded to vec4),
  then B material names (u32 len + bytes), then A chunk headers:
     name, 32B bbox (min vec3+pad, max vec3+pad), u32 a, u32 parentIndex(-1=root),
     64B local matrix (exporter junk in pads),
  then C × material data: u32 K, K×u32 chunk indices (matrix-palette slot -> chunk),
     u32, then C VIPM blobs.

VIPM blob:
  "ViPm", u32 version(<=1), 124B header: +8 N verts, +12 numIndices,
  +48..60 four u32s (extraCount@+60 ×4B, collapseCount@+52 ×12B follow the arrays),
  +64 bbox 32B, +96 16B, +112/116/120 u32 flags (116: normals, 120: skin).
  Then 9 packed components (x,y,z, r,g,b, a, u,v), then 3 normal components.
  Packed component = [f32 min][f32 max]; if max-min < EPS: 4 filler bytes ("futs"),
  all values = min; else N hi-bytes + N lo-bytes IN 8192-ELEMENT BATCHES,
  v = min + ((hi<<8)|lo)/65535*(max-min).
  Then extraCount×4B, collapseCount×12B, numIndices×u16 tris, and per-vertex skin
  records: [u8 n][n × (u8 paletteSlot, u8 weight255)]  (weights sum to 255).

Output: MODELS/<NAME>/skinned.json — bind-pose mesh + bones (parent, bindLocal,
invBind row-major 4x4) + 2-influence-per-vertex skin, SceneKit-ready.

Usage: wsgo_export_skinned.py <actor.wsgo> <out.json> [--name NAME]
"""
import json, struct, subprocess, sys
import numpy as np

WT = "/tmp/cb/WTExtractor/pywttools/wtextract.py"
EPS = 0.000797


def read_component(d, o, N):
    mn, mx = struct.unpack_from("<ff", d, o)
    o += 8
    if mx - mn < EPS:
        return np.full(N, mn, np.float32), o + 4        # 4 filler bytes ("futs")
    vals = np.empty(N, np.float32)
    done = 0
    while done < N:                                      # 8192-element batches
        c = min(8192, N - done)
        hi = np.frombuffer(d[o:o+c], np.uint8).astype(np.uint32)
        lo = np.frombuffer(d[o+c:o+2*c], np.uint8)
        vals[done:done+c] = mn + (hi*256 + lo)/65535.0 * (mx - mn)
        done += c; o += 2*c
    return vals, o


def readstr(d, o):
    ln, = struct.unpack_from("<I", d, o)
    return d[o+4:o+4+ln].split(b"\0")[0].decode(), o + 4 + ln


def mat_rows(d, o):
    """64B padded 3x4 -> 4x4 (rows 0-2 = rotation rows, floats 12-14 = translation)."""
    f = np.frombuffer(d[o:o+64], np.float32)
    M = np.eye(4, dtype=np.float64)
    M[0, :3] = f[0:3]; M[1, :3] = f[4:7]; M[2, :3] = f[8:11]; M[:3, 3] = f[12:15]
    return M


def parse_geom(d):
    assert d[0xC:0x10] == b"moeg"
    ver, A, B, C = struct.unpack_from("<4I", d, 0x10)
    invbind = [mat_rows(d, 0x34 + 64*i) for i in range(A)]
    o = 0x34 + 64*A
    matnames = []
    for _ in range(B):
        s, o = readstr(d, o); matnames.append(s)
    chunks = []
    for _ in range(A):
        name, o = readstr(d, o)
        o += 32                                          # bbox
        _, parent = struct.unpack_from("<Ii", d, o); o += 8
        chunks.append({"name": name, "parent": parent})
        o += 64                                          # local matrix (unused; junk pads)
    materials = []
    for _ in range(C):
        K, = struct.unpack_from("<I", d, o); o += 4
        palette = list(struct.unpack_from(f"<{K}I", d, o)); o += 4*K + 4
        materials.append(palette)

    meshes = []
    for palette in materials:
        vip = d.find(b"ViPm", o)
        N, numIdx = struct.unpack_from("<II", d, vip+8)
        c30, collapseCnt, c38, extraCnt = struct.unpack_from("<4I", d, vip+48)
        normFlag, skinFlag = struct.unpack_from("<II", d, vip+116)
        p = vip + 132
        comps = []
        for _ in range(9):                               # x,y,z r,g,b a u,v
            v, p = read_component(d, p, N); comps.append(v)
        normals = []
        if normFlag:
            for _ in range(3):
                v, p = read_component(d, p, N); normals.append(v)
        p += extraCnt*4 + collapseCnt*12
        tris = np.frombuffer(d[p:p+2*numIdx], "<u2").astype(int); p += 2*numIdx
        skin = None
        if skinFlag:
            skin = []
            for _ in range(N):
                n = d[p]; p += 1
                infl = [(palette[d[p+2*i]], d[p+2*i+1]/255.0) for i in range(n)]
                p += 2*n
                skin.append(infl)
        meshes.append({"N": N, "pos": np.stack(comps[0:3], 1),
                       "rgb": np.stack(comps[3:6], 1), "a": comps[6],
                       "uv": np.stack(comps[7:9], 1),
                       "normals": np.stack(normals, 1) if normals else None,
                       "tris": tris, "skin": skin, "end": p})
        o = p
    return {"chunks": chunks, "invbind": invbind, "meshes": meshes,
            "matnames": matnames}


def export(geom, name, texture, partIndex=0):
    chunks, invbind = geom["chunks"], geom["invbind"]
    worldbind = [np.linalg.inv(m) for m in invbind]
    bones = []
    for i, ch in enumerate(chunks):
        p = ch["parent"]
        local = worldbind[i] if p < 0 else np.linalg.inv(worldbind[p]) @ worldbind[i]
        bones.append({"name": ch["name"], "parent": p,
                      "bindLocal": [round(float(x), 6) for x in local.flatten()],
                      "invBind": [round(float(x), 6) for x in invbind[i].flatten()]})
    m = geom["meshes"][partIndex]
    tris = m["tris"].reshape(-1, 3)
    tris = tris[(tris[:, 0] != tris[:, 1]) & (tris[:, 1] != tris[:, 2]) &
                (tris[:, 0] != tris[:, 2])]              # drop degenerate padding tris
    per = max(len(r) for r in m["skin"])                 # influences per vertex
    idx2, w2 = [], []
    for infl in m["skin"]:
        infl = sorted(infl, key=lambda t: -t[1])
        s = sum(w for _, w in infl)
        infl += [(infl[0][0], 0.0)] * (per - len(infl))
        idx2 += [i for i, _ in infl]
        w2 += [round(w/s, 4) for _, w in infl]
    return {"name": name, "texture": texture, "influencesPerVertex": per,
            "verts": [round(float(v), 5) for v in m["pos"].flatten()],
            "normals": [round(float(v), 4) for v in m["normals"].flatten()],
            "uvs": [round(float(v), 5) for v in m["uv"].flatten()],
            "tris": [int(i) for i in tris.flatten()],
            "bones": bones,
            "skinIndices": idx2, "skinWeights": w2}


if __name__ == "__main__":
    src, out = sys.argv[1], sys.argv[2]
    name = sys.argv[sys.argv.index("--name")+1] if "--name" in sys.argv else "MODEL"
    tex = sys.argv[sys.argv.index("--tex")+1] if "--tex" in sys.argv else ""
    part = int(sys.argv[sys.argv.index("--part")+1]) if "--part" in sys.argv else 0
    if open(src, "rb").read(4) == bytes.fromhex("17fc8e07"):
        d = open(src, "rb").read()                       # already container-decoded
    else:
        subprocess.run([sys.executable, WT, src, "/tmp/_sg.bin", "-q"], check=True)
        d = open("/tmp/_sg.bin", "rb").read()
    geom = parse_geom(d)
    doc = export(geom, name, tex, partIndex=part)
    json.dump(doc, open(out, "w"))
    m = geom["meshes"][part]
    infl_counts = {}
    for r in m["skin"] or []:
        infl_counts[len(r)] = infl_counts.get(len(r), 0) + 1
    print(f"{name}: {m['N']} verts, {len(doc['tris'])//3} tris, "
          f"{len(doc['bones'])} bones {[b['name'] for b in doc['bones']]}, "
          f"influences {infl_counts}, blob end {m['end']:#x}/{len(d):#x}")
