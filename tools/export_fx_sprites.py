#!/usr/bin/env python3
"""
Export the original effect sprite sheets (MEDIA/IMAGES + MEDIA/EFFECTS) into
the clone's Resources/IMAGES/FX/, merging separate alpha.png channels where
the original shipped one (SMOKEPUFF, COIN, SHADOW). Sheets without an alpha
are additive-blended in the engine (black background) and are kept RGB.

Frame layouts (from decompiled Particle_Object_*.java):
  EXPLOSION1     256x256  4x4 grid, 16 frames @ 20fps, additive
  SMOKEPUFF      512x256  4x2 grid of 128px: cols 2-3 = Smoke(0), cols 0-1 = SmokeBlack(1)
  COIN           256x256  4x4 grid, 16 frames @ 20fps looping, alpha
  SPLASHRING     64x64    single frame, alpha-fading water ring
  SPLASH         16x16    droplet sprite
  CLOUDSHADOW / SCORCH / SHORELINE / WATERANIMATION / SHADOW: world dressing
  EFFECTS/FLARE + SUN + CIRCLE1/2 + DOT1/2: lens flare elements
"""
import os, sys
from PIL import Image

_MEDIA = os.environ.get("CB_MEDIA", "/tmp/cb/assets/120302/MEDIA")
_REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_IMAGES = os.path.join(_MEDIA, "IMAGES")
SRC_FX = os.path.join(_MEDIA, "EFFECTS")
OUT = os.path.join(_REPO, "shared/Resources/IMAGES/FX")

NAMES = ["EXPLOSION1", "SMOKEPUFF", "COIN", "SPLASH", "SPLASH2", "SPLASHRING",
         "CHUNKS", "GRIT", "FIREFLY",
         "SPARKLE", "SHOCKWAVE", "SPLAT", "STAR", "RAY",
         "CLOUDSHADOW", "SCORCH", "SHORELINE", "WATERANIMATION", "SHADOW"]
FX = ["FLARE", "SUN", "CIRCLE1", "CIRCLE2", "DOT1", "DOT2"]


def export(src_dir, name, out_name):
    files = os.listdir(src_dir)
    imgf = next((f for f in files if f.startswith("image") or f.startswith("black")), None)
    alphaf = next((f for f in files if f.startswith("alpha")), None)
    img = Image.open(os.path.join(src_dir, imgf)).convert("RGB")
    if alphaf:
        a = Image.open(os.path.join(src_dir, alphaf)).convert("L")
        img = img.convert("RGBA")
        img.putalpha(a.resize(img.size))
    img.save(os.path.join(OUT, out_name + ".png"))
    print(f"{out_name}: {img.size} {'RGBA' if alphaf else 'RGB(additive)'}")


os.makedirs(OUT, exist_ok=True)
for n in NAMES:
    export(os.path.join(SRC_IMAGES, n), n, n)
for n in FX:
    export(os.path.join(SRC_FX, n), n, "FX_" + n)
