#!/usr/bin/env python3
"""
Bulk prop re-export through the SOLVED pipeline (multi-part "parts" format).

The 2026-07-09 model audit (model_audit_grid.png) showed the old heuristic OBJ
exports corrupted many props the same way the PALM was (UV flips, wrong
material routing, un-keyed chroma textures). This exports each listed model's
geom via the byte-exact parser into MODELS/<NAME>/skinned.json with a `parts`
array (one entry per material/ViPm, each with its own texture + skin), sharing
the geom's bone table.
"""
import json, os, subprocess, sys
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from wsgo_export_skinned import parse_geom

WT = os.environ.get("CB_WTEXTRACT", "/tmp/cb/WTExtractor/pywttools/wtextract.py")
SRC = os.environ.get("CB_MEDIA", "/tmp/cb/assets/120302/MEDIA")
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "shared/Resources/MODELS")

# model -> (geom path, [texture per material index], keyed flags)
SPECS = {
    "BRUSH2":      ("OBJECTS/BRUSH2/resources/actor.wsgo", ["ferno.png"], [True]),
    "FERNTREE":    ("OBJECTS/FERNTREE/resources/actor.wsgo", ["ferntreeo.png"], [True]),
    "TIKKI1":      ("OBJECTS/TIKKI/resources/tikki1.wsgo", ["tikki1o.jpg"], [False]),
    "TIKKI2":      ("OBJECTS/TIKKI/resources/tikki2.wsgo", ["tikki2o.jpg"], [False]),
    "TIKKI3":      ("OBJECTS/TIKKI/resources/tikki3.wsgo", ["tikki3o.jpg"], [False]),
    "BRIDGE":      ("OBJECTS/BRIDGE/resources/actor.wsgo", ["bridge.jpg"], [False]),
    "MOUND":       ("OBJECTS/MOUND/resources/mound.wsgo", ["moundo.jpg", "moundo.jpg"], [False, False]),
    "TORCHBEARER": ("OBJECTS/TORCHBEARER/resources/actor.wsgo", ["torch_bearer.jpg"], [False]),
    "BOUNCEBALL":  ("OBJECTS/BOUNCEBALL/resources/actor.wsgo", ["rollballo.jpg"], [False]),
    "LIGHTHOUSE":  ("OBJECTS/LIGHTHOUSE/resources/actor.wsgo",
                    ["rail.png", "lighto.jpg", "lighthouseo.jpg"], [True, False, False]),
    "SHIP":        ("OBJECTS/SHIP/resources/base.wsgo", ["boato.jpg"], [False]),
    "MAST":        ("OBJECTS/SHIP/resources/mast.wsgo", ["boato.jpg"], [False]),
    "TAILS":       ("OBJECTS/TAILS/resources/tail1.wsgo", ["cattailso.png"], [True]),
    "TNT":         ("OBJECTS/TNT/resources/actor.wsgo", ["barrelo.jpg"], [False]),
    "OBELISK":     ("OBJECTS/OBELISK/resources/actor.wsgo", ["obelisk.jpg"], [False]),
    "MOUNDBEAM":   ("OBJECTS/MOUND/resources/beam.wsgo", ["moundo.jpg"], [False]),
    "FIREHEAD":    ("OBJECTS/FIREHEAD/resources/actor.wsgo", ["firehovelo.jpg"], [False]),
    "HUT":         ("OBJECTS/HUT/resources/actor.wsgo", ["hut.jpg"], [False]),
    # torch.wsbm decodes to PNG (magic 89504E47) -> chroma-keyed
    "TORCH":       ("OBJECTS/TORCH/resources/actor.wsgo", ["torch.png"], [True]),
    # green bush decoration; texture is the palm atlas
    "BRUSH1":      ("OBJECTS/PALM2/resources/brush1.wsgo", ["palmo.png"], [True]),
    "TAILS2":      ("OBJECTS/TAILS/resources/tail2.wsgo", ["cattailso.png"], [True]),
    "TAILS3":      ("OBJECTS/TAILS/resources/tail3.wsgo", ["cattailso.png"], [True]),
    # debris pieces (Chunk_Object tumble on prop destruction); exported into a
    # DEBRIS/ folder keyed by the prop.dat <DEBRIS> path stem
    "DEBRIS/HUT_debris1":     ("OBJECTS/HUT/resources/debris1.wsgo", ["huto.jpg"], [False]),
    "DEBRIS/OBELISK_debris1": ("OBJECTS/OBELISK/resources/debris1.wsgo", ["obelisko.jpg"], [False]),
    "DEBRIS/OBELISK_debris2": ("OBJECTS/OBELISK/resources/debris2.wsgo", ["obelisko.jpg"], [False]),
    "DEBRIS/OBELISK_debris3": ("OBJECTS/OBELISK/resources/debris3.wsgo", ["obelisko.jpg"], [False]),
    "DEBRIS/PALM2_debris1":   ("OBJECTS/PALM2/resources/debris1.wsgo", ["palmo.png"], [False]),
    "DEBRIS/PALM2_debris2":   ("OBJECTS/PALM2/resources/debris2.wsgo", ["palmo.png"], [False]),
    "DEBRIS/PALM2_debris3":   ("OBJECTS/PALM2/resources/debris3.wsgo", ["palmo.png"], [False]),
    "DEBRIS/PALM2_debris4":   ("OBJECTS/PALM2/resources/debris4.wsgo", ["palmo.png"], [False]),
    "DEBRIS/TIKKI_debris1":   ("OBJECTS/TIKKI/resources/debris1.wsgo", ["tikki1o.jpg"], [False]),
    "DEBRIS/TIKKI_debris2":   ("OBJECTS/TIKKI/resources/debris2.wsgo", ["tikki2o.jpg"], [False]),
    "DEBRIS/TIKKI_debris3":   ("OBJECTS/TIKKI/resources/debris3.wsgo", ["tikki3o.jpg"], [False]),
}


def export_model(name, rel, textures, keyed):
    subprocess.run([sys.executable, WT, f"{SRC}/{rel}", "/tmp/_pp.bin", "-q"],
                   capture_output=True)
    g = parse_geom(open("/tmp/_pp.bin", "rb").read())
    worldbind = [np.linalg.inv(m) for m in g["invbind"]]
    bones = []
    for i, ch in enumerate(g["chunks"]):
        p = ch["parent"]
        local = worldbind[i] if p < 0 else np.linalg.inv(worldbind[p]) @ worldbind[i]
        bones.append({"name": ch["name"], "parent": p,
                      "bindLocal": [round(float(x), 6) for x in local.flatten()],
                      "invBind": [round(float(x), 6) for x in g["invbind"][i].flatten()]})
    parts = []
    for mi, m in enumerate(g["meshes"]):
        tris = m["tris"].reshape(-1, 3)
        tris = tris[(tris[:, 0] != tris[:, 1]) & (tris[:, 1] != tris[:, 2]) &
                    (tris[:, 0] != tris[:, 2])]
        per = max(len(r) for r in m["skin"]) if m["skin"] else 1
        idx2, w2 = [], []
        for infl in (m["skin"] or [[(0, 1.0)]] * m["N"]):
            infl = sorted(infl, key=lambda t: -t[1])
            sw = sum(w for _, w in infl) or 1
            infl += [(infl[0][0], 0.0)] * (per - len(infl))
            idx2 += [i for i, _ in infl]
            w2 += [round(w / sw, 4) for _, w in infl]
        parts.append({
            "texture": textures[mi] if mi < len(textures) else textures[-1],
            "keyed": bool(keyed[mi] if mi < len(keyed) else keyed[-1]),
            "influencesPerVertex": per,
            "verts": [round(float(v), 5) for v in m["pos"].flatten()],
            "normals": [round(float(v), 4) for v in m["normals"].flatten()],
            "uvs": [round(float(v), 5) for v in m["uv"].flatten()],
            "tris": [int(i) for i in tris.flatten()],
            "skinIndices": idx2, "skinWeights": w2,
        })
    doc = {"name": name, "bones": bones, "parts": parts}
    os.makedirs(f"{OUT}/{name}", exist_ok=True)
    json.dump(doc, open(f"{OUT}/{name}/skinned.json", "w"))
    print(f"{name}: {len(parts)} parts, "
          + ", ".join(f"{m['N']}v/{p['texture']}" for m, p in zip(g["meshes"], parts)))


if __name__ == "__main__":
    for name, (rel, tex, keyed) in SPECS.items():
        export_model(name, rel, tex, keyed)
