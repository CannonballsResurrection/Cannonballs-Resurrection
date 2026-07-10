#!/usr/bin/env python3
"""
Export the SKIES/* actor geometry (sky dome + HORIZON ISLAND billboards + moon/
stars/outcrops) to MODELS/SKY/<NAME>.json + textures, using the solved geom
parser. Per-sky material -> texture mapping read off the resources folders.
"""
import json, os, struct, subprocess, sys
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from wsgo_export_skinned import parse_geom

WT = os.environ.get("CB_WTEXTRACT", "/tmp/cb/WTExtractor/pywttools/wtextract.py")
SRC = os.path.join(os.environ.get("CB_MEDIA", "/tmp/cb/assets/120302/MEDIA"), "SKIES")
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "shared/Resources/MODELS/SKY")

# material name -> (texture wsbm stem, alpha wsbm stem or None), per sky
TEXMAP = {
    "BLUE":   {"skydome2": ("bluesky", None), "isle1": ("island1", "island1ao_alpha"),
               "isle2": ("island2", "island2ao_alpha")},
    "NIGHT":  {"skydome2": ("nightsky", None), "skytop": ("starso", None),
               "isle1": ("island1night", "island1ao_alpha"), "moon": ("moon", "moonao_alpha"),
               "isle2": ("outcrop", "outcropao_alpha")},
    "PURPLE": {"skydome2": ("purplesky", None), "isle1": ("island1night", "island1ao_alpha"),
               "isle2": ("volcano", "volcanoa_alpha")},
    "GREEN":  {"skydome2": ("ngreenskyo", None), "isle2": ("outcropg", "outcropao_alpha")},
    "DESERT": {"skydome2": ("desertsky", None)},
}


def decode_wsbm(path):
    subprocess.run([sys.executable, WT, path, "/tmp/_sb.bin", "-q"],
                   capture_output=True)
    d = open("/tmp/_sb.bin", "rb").read()
    for sig in (b"\xff\xd8\xff", b"\x89PNG"):
        i = d.find(sig)
        if i >= 0:
            return d[i:]
    return None


def export_sky(sky):
    from PIL import Image
    import io
    res = f"{SRC}/{sky}/resources"
    subprocess.run([sys.executable, WT, f"{res}/actor.wsgo", "/tmp/_skg.bin", "-q"],
                   capture_output=True)
    g = parse_geom(open("/tmp/_skg.bin", "rb").read())
    os.makedirs(f"{OUT}/textures", exist_ok=True)
    parts = []
    for name, mesh in zip(g["matnames"], g["meshes"]):
        stem, alpha = TEXMAP[sky].get(name, (None, None))
        if stem is None:
            print(f"  {sky}/{name}: no texture mapping, skipped"); continue
        texfile = f"{sky}_{stem}.png"
        raw = decode_wsbm(f"{res}/{stem}.wsbm")
        img = Image.open(io.BytesIO(raw)).convert("RGB")
        if alpha:
            araw = decode_wsbm(f"{res}/{alpha}.wsbm")
            a = Image.open(io.BytesIO(araw)).convert("L").resize(img.size)
            img = img.convert("RGBA"); img.putalpha(a)
        img.save(f"{OUT}/textures/{texfile}")
        tris = mesh["tris"].reshape(-1, 3)
        tris = tris[(tris[:, 0] != tris[:, 1]) & (tris[:, 1] != tris[:, 2]) & (tris[:, 0] != tris[:, 2])]
        parts.append({"name": name, "texture": texfile, "alpha": alpha is not None,
                      "verts": [round(float(v), 4) for v in mesh["pos"].flatten()],
                      "uvs": [round(float(v), 5) for v in mesh["uv"].flatten()],
                      "tris": [int(i) for i in tris.flatten()]})
        print(f"  {sky}/{name}: {mesh['N']}v {len(tris)}t -> {texfile}")
    json.dump({"name": sky, "parts": parts}, open(f"{OUT}/{sky}.json", "w"))


if __name__ == "__main__":
    for sky in TEXMAP:
        print(sky)
        export_sky(sky)
