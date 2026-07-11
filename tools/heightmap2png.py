#!/usr/bin/env python3
"""
heightmap2png.py - Convert WildTangent Cannonballs heightmap*.dat terrain to grayscale PNG.

FORMAT (reverse-engineered from decompiled Island.java, parseData(), lines ~819-838):
  - File is raw, uncompressed: WIDTH*HEIGHT bytes, 1 unsigned byte per sample.
  - Cannonballs ships 96x96 => heightmap96.dat is 9216 bytes. (Width==Height==96 in Island.java.)
  - Read order: inner loop advances X (0..Width-1), then Z increments. So the byte at
    linear index i maps to:  x = i % WIDTH,  z = i // WIDTH   (row-major, X fastest).
    In-engine it is stored as HeightMap[x][z].
  - Sample value b (0..255) becomes world height:
        height = b/256.0 * MapScale - 4.0
    where MapScale is per-map (maplist.dat field index 6: e.g. 64/75/100/128/110) and
    MaxTerrainHeight is clamped to 100. b < ~ (4/ MapScale *256) is below water (height<0).
  - For visualization we render the raw byte 0..255 directly as luminance (higher = taller).
    Origin: we write PNG row 0 = z 0. Flip vertically with --flip if coastline is mirrored
    vs image.png.

USAGE:
  python3 heightmap2png.py            # convert all maps under the assets tree
  python3 heightmap2png.py <in.dat> <out.png> [width]
"""
import sys, os, glob
from PIL import Image

ASSETS = "/tmp/cb/assets/120302/MEDIA/MAPS"
OUT    = "/tmp/cb/converted/maps"

def dat_to_png(path, out, width=96, flip=False):
    data = open(path, "rb").read()
    n = len(data)
    # infer square dimension if not the standard 96
    height = n // width
    if width * height != n:
        # try perfect square
        import math
        s = int(round(math.sqrt(n)))
        if s * s == n:
            width = height = s
        else:
            raise ValueError(f"{path}: {n} bytes not divisible by width {width}")
    img = Image.frombytes("L", (width, height), data)  # row-major, X fastest = exactly our layout
    if flip:
        img = img.transpose(Image.FLIP_TOP_BOTTOM)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)
    return width, height, n

def main():
    if len(sys.argv) >= 3:
        w = int(sys.argv[3]) if len(sys.argv) > 3 else 96
        print(dat_to_png(sys.argv[1], sys.argv[2], w))
        return
    os.makedirs(OUT, exist_ok=True)
    for mapdir in sorted(glob.glob(os.path.join(ASSETS, "*"))):
        if not os.path.isdir(mapdir):
            continue
        name = os.path.basename(mapdir)
        for dat in glob.glob(os.path.join(mapdir, "heightmap*.dat")):
            # width from filename: heightmap96.dat -> 96
            base = os.path.basename(dat)
            digits = "".join(c for c in base if c.isdigit())
            w = int(digits) if digits else 96
            out = os.path.join(OUT, f"{name}_height.png")
            wd, ht, nb = dat_to_png(dat, out, w)
            print(f"{name}: {base} ({nb} bytes) -> {out}  {wd}x{ht}")

if __name__ == "__main__":
    main()
