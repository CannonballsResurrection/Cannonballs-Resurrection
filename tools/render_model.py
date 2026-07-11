#!/usr/bin/env python3
"""Render an assembled model.json (all parts, transforms applied) to a PNG grid
from a couple of angles. Matplotlib offscreen (Agg)."""
import sys, os, json
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection

MODELS = '/tmp/cb/converted/models_final'


def load_obj(path):
    V = []
    F = []
    for ln in open(path):
        if ln.startswith('v '):
            _, x, y, z = ln.split()[:4]
            V.append((float(x), float(y), float(z)))
        elif ln.startswith('f '):
            idx = [int(p.split('/')[0]) - 1 for p in ln.split()[1:]]
            if len(idx) >= 3:
                F.append(idx[:3])
    return np.array(V), np.array(F)


def rowmajor(t):
    m = np.array(t, float).reshape(4, 4)
    return m


def apply(V, m):
    H = np.hstack([V, np.ones((len(V), 1))])
    # row-major: point row-vector * M^T? We stored row-major 4x4 where translation
    # is m[0][3],m[1][3],m[2][3]. Transform p' = M @ [x,y,z,1].
    out = (m @ H.T).T
    return out[:, :3]


def render(name):
    mdir = os.path.join(MODELS, name)
    man = json.load(open(os.path.join(mdir, 'model.json')))
    allV = []
    tris = []
    base = 0
    for p in man['parts']:
        objp = os.path.join(mdir, p['mesh'])
        if not os.path.exists(objp):
            continue
        V, F = load_obj(objp)
        if len(V) == 0:
            continue
        t = p.get('transform')
        if t and len(t) == 16:
            V = apply(V, rowmajor(t))
        allV.append(V)
        for f in F:
            tris.append([f[0] + base, f[1] + base, f[2] + base])
        base += len(V)
    if not allV:
        print('no geometry', name)
        return
    V = np.vstack(allV)
    tris = np.array(tris)
    # model is Y-up; matplotlib plots Z vertical -> swap Y and Z
    V = V[:, [0, 2, 1]]
    # center + scale
    c = (V.max(0) + V.min(0)) / 2
    V = V - c
    r = np.abs(V).max()

    fig = plt.figure(figsize=(9, 4.5))
    for i, (az, el) in enumerate([(30, 20), (120, 15)]):
        ax = fig.add_subplot(1, 2, i + 1, projection='3d')
        polys = V[tris]
        pc = Poly3DCollection(polys, facecolor=(0.7, 0.75, 0.8), edgecolor=(0.2, 0.2, 0.25),
                              linewidths=0.15, alpha=1.0)
        ax.add_collection3d(pc)
        ax.set_xlim(-r, r); ax.set_ylim(-r, r); ax.set_zlim(-r, r)
        ax.view_init(elev=el, azim=az)
        ax.set_box_aspect((1, 1, 1))
        ax.set_axis_off()
        ax.set_title(f'{name}  az={az}', fontsize=8)
    outp = os.path.join(MODELS, '_previews', f'{name}.png')
    os.makedirs(os.path.dirname(outp), exist_ok=True)
    fig.tight_layout()
    fig.savefig(outp, dpi=90, facecolor='white')
    plt.close(fig)
    print('->', outp, f'({len(V)} verts, {len(tris)} tris)')


if __name__ == '__main__':
    for n in sys.argv[1:]:
        render(n)
