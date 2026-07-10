#!/usr/bin/env python
"""Color-key foliage atlas textures to transparent RGBA PNGs and crop sub-regions.

Foliage atlases use a solid color-key background (black for most, RED for
ferntree). Pixels near the key become alpha=0, with a small feather ramp so
edges don't show a hard halo. Some atlases are split into sub-region crops.

Run once; outputs are committed into the app Resources/PROPTEX tree.
"""
import os
from PIL import Image

SRC = os.environ.get("CB_CONVERTED", "/tmp/cb/converted/models")
DST = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "shared/Resources/PROPTEX")


def key_black(im, thresh=30, feather=45):
    """Make near-black pixels transparent with a feathered alpha ramp."""
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            m = max(r, g, b)
            if m <= thresh:
                px[x, y] = (r, g, b, 0)
            elif m < feather:
                # ramp 0..255 across the feather band
                aa = int((m - thresh) / (feather - thresh) * 255)
                px[x, y] = (r, g, b, aa)
    return im


def key_red(im, feather=60):
    """Make red-key pixels transparent (ferntree uses bright red bg)."""
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            # red key: high red, low green+blue
            if r > 150 and g < 90 and b < 90:
                px[x, y] = (r, g, b, 0)
            elif r > 110 and g < 110 and b < 110 and r - max(g, b) > 40:
                # feather band around the key
                dist = r - max(g, b)
                aa = max(0, int((1 - dist / 120) * 255))
                px[x, y] = (r, g, b, aa)
    return im


def save(im, model, name):
    d = os.path.join(DST, model)
    os.makedirs(d, exist_ok=True)
    p = os.path.join(d, name)
    im.save(p)
    print("wrote", p, im.size)


def main():
    # PALM2 palmo.png: top half = frond, bottom half = trunk bark. black key.
    palm = Image.open(os.path.join(SRC, "PALM2/textures/palmo.png"))
    w, h = palm.size
    # frond sits in the top ~47%; trunk bark below. Trim the seam so the frond
    # crop doesn't drag a strip of trunk with it.
    frond = key_black(palm.crop((0, 0, w, int(h * 0.47))))
    trunk = key_black(palm.crop((0, h // 2, w, h)))
    save(frond, "PALM2", "frond.png")
    save(trunk, "PALM2", "trunk.png")

    # FERNTREE ferntreeo.png: RED key. left half = trunk, right half = fern leaf.
    ft = Image.open(os.path.join(SRC, "FERNTREE/textures/ferntreeo.png"))
    w, h = ft.size
    ft_trunk = key_red(ft.crop((0, 0, w // 2, h)))
    ft_frond = key_red(ft.crop((w // 2, 0, w, h)))
    save(ft_trunk, "FERNTREE", "trunk.png")
    save(ft_frond, "FERNTREE", "frond.png")

    # BRUSH2 ferno.png: black key, single frond.
    brush = key_black(Image.open(os.path.join(SRC, "BRUSH2/textures/ferno.png")))
    save(brush, "BRUSH2", "brush.png")

    # TAILS cattailso.png: black key, cattails.
    tails = key_black(Image.open(os.path.join(SRC, "TAILS/textures/cattailso.png")))
    save(tails, "TAILS", "cattails.png")


if __name__ == "__main__":
    main()
