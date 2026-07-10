#!/usr/bin/env python3
"""wsgo2obj_final — WildTangent .wsgo geom -> OBJ (pos+normal+uv) + MTL + textures + model.json.

Built on the SOLVED byte-planar 16-bit dequant decoder (see wsgo_decode_SOLVED.py
and format-research/GEOM_MESH_FORMAT_SOLVED.md). Layout inside the ViPm blob:

  ViPm+8   : u32 render vertex count N
  ViPm+132 : positions = 3 SoA blocks  (block = [f32 min][f32 max][N hi][N lo], size 8+2N)
  <header> : a variable material-chunk header (0..~256B; auto-detected)
  then     : normals = 3 SoA blocks, UVs = 2 SoA blocks
  then     : faces = int16 LE triangle indices (< N), quad-split winding

Some models (HUT, LIGHTHOUSE, FIREHEAD, MOUND/mound, CANNON/stone, TAILS, a few
debris) carry an extra per-vertex array (vertex color) between positions and
normals that we don't fully model; for those we fall back to positions+faces
only (correct geometry, no UV/normal) so they still render as the right shape.

Run:  /tmp/cb/wtvenv/bin/python /tmp/cb/tools/wsgo2obj_final.py [MODEL ...]
"""
import os, sys, glob, json, struct, traceback
import numpy as np

sys.path.insert(0, '/tmp/cb/tools')
import container  # noqa

ASSETS_OBJ = '/tmp/cb/assets/120302/MEDIA/OBJECTS'
OUT_ROOT = '/tmp/cb/converted/models_final'
GEOM_MAGIC = bytes.fromhex('17fc8e07')


# ---------------------------------------------------------------------------
# byte-planar 16-bit dequant  (THE solved key)
# ---------------------------------------------------------------------------
def read_comp(d, o, N):
    mn, mx = struct.unpack_from('<ff', d, o)
    hi = np.frombuffer(d[o + 8:o + 8 + N], np.uint8).astype(np.uint32)
    lo = np.frombuffer(d[o + 8 + N:o + 8 + 2 * N], np.uint8)
    if len(hi) < N or len(lo) < N:
        raise ValueError('short block')
    return mn + (hi * 256 + lo) / 65535.0 * (mx - mn)


def face_run(d, start, N):
    """Longest int16 run of indices < N starting at/after `start`."""
    o = start
    idx = []
    while o + 2 <= len(d):
        v = struct.unpack_from('<H', d, o)[0]
        if v >= N:
            break
        idx.append(v)
        o += 2
    return idx


def best_face_run(d, vip, N):
    """Generic: scan for the longest int16 run of indices < N (SOLVED finder)."""
    NN = len(d)
    best = (0, 0)
    o = vip
    while o + 2 < NN:
        j = o
        c = 0
        while j + 2 <= NN and struct.unpack_from('<H', d, j)[0] < N:
            c += 1
            j += 2
        if c > best[0]:
            best = (c, o)
        o += max(1, c * 2) if c > 3 else 1
    return best[1]


# ---------------------------------------------------------------------------
# node / transform tree  (from wsgo2obj — SOLVED plumbing)
# ---------------------------------------------------------------------------
def read_cstr_len(d, off):
    ln = struct.unpack('<I', d[off:off + 4])[0]
    if ln == 0 or ln > 64 or off + 4 + ln > len(d):
        return None, off
    raw = d[off + 4:off + 4 + ln]
    if raw[-1:] != b'\x00':
        return None, off
    try:
        name = raw[:-1].decode('latin1')
    except Exception:
        return None, off
    return name, off + 4 + ln


def parse_nodes(d):
    nodes = []
    vipm = d.find(b'ViPm')
    end = vipm if vipm > 0 else len(d)
    off = 0x10
    while off < end - 4:
        name, no = read_cstr_len(d, off)
        if name is None:
            off += 1
            continue
        marker = d.find(b'\xff\xff\xff\xff', no, end)
        transform = None
        if marker != -1 and marker + 4 + 64 <= len(d):
            try:
                transform = list(struct.unpack('<16f', d[marker + 4:marker + 4 + 64]))
            except Exception:
                transform = None
        if name.strip('\x00'):
            nodes.append({'name': name, 'transform': transform})
        off = no
    return nodes


def root_transform(nodes):
    """First node with a finite, plausible 4x4. Force homogeneous row (0,0,0,1)."""
    for n in nodes:
        t = n.get('transform')
        if not t or len(t) != 16:
            continue
        if not all(np.isfinite(t)):
            continue
        # sanity: linear part magnitudes reasonable
        lin = [t[0], t[1], t[2], t[4], t[5], t[6], t[8], t[9], t[10]]
        if all(abs(x) < 1e4 for x in lin) and any(abs(x) > 1e-6 for x in lin):
            m = list(t)
            m[3] = m[7] = m[11] = 0.0  # translation column stays; row-major so [3],[7],[11] are tx,ty,tz
            m[12] = m[13] = m[14] = 0.0
            m[15] = 1.0
            return m
    return [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]


# ---------------------------------------------------------------------------
# full mesh decode
# ---------------------------------------------------------------------------
def decode_mesh(d):
    vip = d.find(b'ViPm')
    if vip < 0:
        raise ValueError('no ViPm')
    N = struct.unpack_from('<I', d, vip + 8)[0]
    st = 8 + 2 * N
    ps = vip + 132
    # positions (proven)
    V = np.stack([read_comp(d, ps, N),
                  read_comp(d, ps + st, N),
                  read_comp(d, ps + 2 * st, N)], 1)
    posend = ps + 3 * st

    # try to locate normals + UVs by scanning the material-chunk header size.
    normals = uvs = None
    face_start = None
    for hdr in range(0, 520):
        no = posend + hdr
        if no + 5 * st + 6 > len(d):
            break
        try:
            nx = read_comp(d, no, N)
            ny = read_comp(d, no + st, N)
            nz = read_comp(d, no + 2 * st, N)
        except Exception:
            continue
        L = np.sqrt(nx * nx + ny * ny + nz * nz)
        if not np.isfinite(L).all() or not (0.5 < L.mean() < 1.5):
            continue
        uo = no + 3 * st
        try:
            u = read_comp(d, uo, N)
            v = read_comp(d, uo + st, N)
        except Exception:
            continue
        if not (np.isfinite(u).all() and np.isfinite(v).all()):
            continue
        if not (u.min() > -2.1 and u.max() < 3 and v.min() > -2.1 and v.max() < 3):
            continue
        end = uo + 2 * st
        run = face_run(d, end, N)
        # A valid layout: the int16 run right after the UV blocks is the face
        # index list (>= N indices for any closed mesh). Don't require exact
        # multiple of 3 (runs can over-read a trailing scalar; we floor later).
        if len(run) >= N:
            normals = np.stack([nx, ny, nz], 1)
            uvs = np.stack([u, v], 1)
            face_start = end
            break

    if face_start is not None:
        idx = face_run(d, face_start, N)
    else:
        # fallback: generic longest run (positions+faces only, no normal/uv)
        fs = best_face_run(d, vip, N)
        idx = face_run(d, fs, N)

    n3 = (len(idx) // 3) * 3
    faces = [tuple(idx[i:i + 3]) for i in range(0, n3, 3)]
    return {'V': V, 'N': normals, 'UV': uvs, 'faces': faces, 'vcount': N,
            'has_uv': uvs is not None}


# ---------------------------------------------------------------------------
# emit
# ---------------------------------------------------------------------------
def write_obj(path, m, mtl_name, part):
    lines = ['mtllib %s' % mtl_name, 'o %s' % part]
    V = m['V']
    for v in V:
        lines.append('v %.6f %.6f %.6f' % (v[0], v[1], v[2]))
    UV = m['UV']
    if UV is not None:
        for uv in UV:
            # flip V for OBJ/OpenGL convention (origin bottom-left)
            lines.append('vt %.6f %.6f' % (uv[0], 1.0 - uv[1]))
    NN = m['N']
    if NN is not None:
        for nv in NN:
            lines.append('vn %.6f %.6f %.6f' % (nv[0], nv[1], nv[2]))
    lines.append('usemtl mat0')
    for f in m['faces']:
        a, b, c = f[0] + 1, f[1] + 1, f[2] + 1
        if UV is not None and NN is not None:
            lines.append('f %d/%d/%d %d/%d/%d %d/%d/%d' % (a, a, a, b, b, b, c, c, c))
        elif UV is not None:
            lines.append('f %d/%d %d/%d %d/%d' % (a, a, b, b, c, c))
        elif NN is not None:
            lines.append('f %d//%d %d//%d %d//%d' % (a, a, b, b, c, c))
        else:
            lines.append('f %d %d %d' % (a, b, c))
    with open(path, 'w') as fh:
        fh.write('\n'.join(lines) + '\n')


def write_mtl(path, texture):
    lines = ['newmtl mat0', 'Ka 0.4 0.4 0.4', 'Kd 0.9 0.9 0.9', 'Ks 0 0 0', 'd 1.0', 'illum 1']
    if texture:
        lines.append('map_Kd %s' % texture)
    with open(path, 'w') as fh:
        fh.write('\n'.join(lines) + '\n')


def decode_textures(rdir, tex_dir):
    os.makedirs(tex_dir, exist_ok=True)
    out = []
    for w in sorted(glob.glob(os.path.join(rdir, '*.wsbm'))):
        try:
            data = container.decode_file(w)
            stem = os.path.splitext(os.path.basename(w))[0]
            if data[:2] == b'\xff\xd8':
                fn = stem + '.jpg'
            elif data[:8] == b'\x89PNG\r\n\x1a\n':
                fn = stem + '.png'
            else:
                fn = stem + '.bin'
            with open(os.path.join(tex_dir, fn), 'wb') as fh:
                fh.write(data)
            if fn.endswith(('.jpg', '.png')):
                out.append(fn)
        except Exception as e:
            print('  tex FAIL %s: %s' % (os.path.basename(w), e))
    return out


def _identity():
    return [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]


_REFLECT = ('reflection', 'breflection', 'refraction', 'white', 'reflect')


def choose_texture(part, tex_files, rdir):
    """Pick the best diffuse texture for a part. Prefer a name match; skip
    reflection/refraction/white masks; else the largest remaining file."""
    if not tex_files:
        return None
    cands = [t for t in tex_files
             if not any(k in os.path.splitext(t)[0].lower() for k in _REFLECT)]
    if not cands:
        cands = list(tex_files)
    pl = part.lower()
    # direct-ish name match: part 'barrel' -> 'cannonbase'? not reliable; try
    # matching the part stem or the '<name>o' object-texture convention.
    for t in cands:
        stem = os.path.splitext(t)[0].lower()
        if stem == pl or stem == pl + 'o' or pl in stem or stem.rstrip('o') == pl:
            return t
    # object-texture (ends in 'o') is usually the main skin
    for t in cands:
        if os.path.splitext(t)[0].lower().endswith('o'):
            return t
    # largest file
    cands.sort(key=lambda t: os.path.getsize(os.path.join(rdir, '..', 'resources',
               os.path.splitext(t)[0] + '.wsbm')) if os.path.exists(
               os.path.join(rdir, os.path.splitext(t)[0] + '.wsbm')) else 0, reverse=True)
    return cands[0]


def bbox(V):
    return V.min(0).tolist(), V.max(0).tolist()


def process_model(name, out_name=None):
    out_name = out_name or name
    rdir = os.path.join(ASSETS_OBJ, name, 'resources')
    if not os.path.isdir(rdir):
        return {'name': name, 'status': 'no_resources'}
    out_dir = os.path.join(OUT_ROOT, out_name)
    os.makedirs(out_dir, exist_ok=True)
    tex_files = decode_textures(rdir, os.path.join(out_dir, 'textures'))

    parts = []
    status = []
    # Parts are authored in a shared coordinate space -> identity composes them
    # correctly (verified by rendering assembled CANNON / PALM2 / SHIP). Debris
    # and decorative brush meshes are separate destroy/scatter effects, not part
    # of the standing prop, so they're exported to disk but excluded from the
    # assembled model manifest.
    def is_primary(part):
        pl = part.lower()
        return not (pl.startswith('debris') or pl.startswith('brush'))

    for gp in sorted(glob.glob(os.path.join(rdir, '*.wsgo'))):
        part = os.path.splitext(os.path.basename(gp))[0]
        try:
            gb = container.decode_file(gp)
            if gb[:4] != GEOM_MAGIC:
                status.append((part, 'not_geom', gb[:4].hex()))
                continue
            m = decode_mesh(gb)
        except Exception as e:
            status.append((part, 'FAIL', str(e)))
            continue
        obj_name = '%s.obj' % part
        mtl_name = '%s.mtl' % part
        chosen = choose_texture(part, tex_files, rdir)
        tex = ('textures/%s' % chosen) if chosen else None
        write_obj(os.path.join(out_dir, obj_name), m, mtl_name, part)
        write_mtl(os.path.join(out_dir, mtl_name), tex)
        transform = _identity()
        bmin, bmax = bbox(m['V'])
        if not is_primary(part):
            status.append((part, 'debris', '%dv (excluded from model)' % m['vcount']))
            continue
        # world bbox after transform (approx: apply linear+translation)
        parts.append({
            'mesh': obj_name,
            'textures': [os.path.basename(t) for t in tex_files],
            'transform': transform,
            '_vcount': m['vcount'], '_faces': len(m['faces']),
            '_has_uv': m['has_uv'], '_bbmin': bmin, '_bbmax': bmax,
        })
        status.append((part, 'ok', '%dv/%dtris uv=%s'
                       % (m['vcount'], len(m['faces']), m['has_uv'])))

    manifest = {
        'name': out_name,
        'upAxis': 'Y',
        'scale': 1.0,
        'parts': [{'mesh': p['mesh'], 'textures': p['textures'],
                   'transform': p['transform']} for p in parts],
        '_debug': parts,
        '_status': status,
    }
    with open(os.path.join(out_dir, 'model.json'), 'w') as fh:
        json.dump(manifest, fh, indent=2, default=str)
    return {'name': name, 'out': out_name, 'status': status, 'parts': len(parts)}


def main():
    args = sys.argv[1:]
    targets = args if args else sorted(
        d for d in os.listdir(ASSETS_OBJ)
        if os.path.isdir(os.path.join(ASSETS_OBJ, d, 'resources')))
    os.makedirs(OUT_ROOT, exist_ok=True)
    summary = []
    for name in targets:
        print('== %s' % name)
        try:
            r = process_model(name)
        except Exception as e:
            traceback.print_exc()
            r = {'name': name, 'error': str(e)}
        for s in r.get('status', []):
            print('   %-14s %-6s %s' % s)
        summary.append(r)
    with open(os.path.join(OUT_ROOT, '_summary.json'), 'w') as fh:
        json.dump(summary, fh, indent=2, default=str)
    print('\nSummary -> %s/_summary.json' % OUT_ROOT)


if __name__ == '__main__':
    main()
