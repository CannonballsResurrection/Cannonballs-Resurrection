#!/usr/bin/env python3
"""wsgo2obj — WildTangent .wsgo geom -> OBJ/MTL/JPEG/manifest.

STATUS (2026-07-06): container decode, node tree (names + 4x4 transforms),
ViPm chunk (counts + bbox), FACES (int16 quad-split triples), and .wsbm
textures (-> JPEG) are all SOLVED and emitted correctly. The per-vertex
QUANTIZATION bit layout is NOT yet solved (see WSGO_GEOM_FORMAT_NOTES.md).
`decode_vertices()` is the single isolated hook: when the bit layout is
cracked, only that function changes and the whole pipeline lights up.

Until then, models are emitted with `vertices_solved=false` in the manifest and
a best-effort placeholder vertex decode (documented, not visually correct).

Run:  /tmp/cb/wtvenv/bin/python /tmp/cb/tools/wsgo2obj.py [MODEL ...]
"""
import os, sys, glob, json, struct, traceback
import numpy as np

sys.path.insert(0, '/tmp/cb/tools')
import container  # noqa

ASSETS_OBJ = '/tmp/cb/assets/120302/MEDIA/OBJECTS'
ASSETS_PROP = '/tmp/cb/assets/120302/MEDIA/PROPS'
OUT_ROOT = '/tmp/cb/converted/models'

GEOM_MAGIC = bytes.fromhex('17fc8e07')


# ---------------------------------------------------------------------------
# geom blob parsing (SOLVED layers)
# ---------------------------------------------------------------------------
def read_cstr_len(d, off):
    """u32 namelen (incl trailing NUL) + name. Returns (name, next_off)."""
    ln = struct.unpack('<I', d[off:off+4])[0]
    if ln == 0 or ln > 64 or off+4+ln > len(d):
        return None, off
    raw = d[off+4:off+4+ln]
    if raw[-1:] != b'\x00':
        return None, off
    try:
        name = raw[:-1].decode('latin1')
    except Exception:
        return None, off
    return name, off+4+ln


def find_vipm(d):
    i = d.find(b'ViPm')
    return i


def parse_vipm(d, i):
    """Return dict with counts and bbox. i = offset of 'ViPm'."""
    o = i + 4
    flag = struct.unpack('<I', d[o:o+4])[0]; o += 4
    # repeated (vcount,fcount) u32 pairs; first pair holds the real counts
    vcount, fcount = struct.unpack('<II', d[o:o+8])
    # bbox: two vec4 (min.xyz,0)(max.xyz,0). Locate by scanning for a plausible
    # float sextuple where min<max on every axis, near the counts region.
    bbmin = bbmax = None
    bbox_off = None
    for off in range(o, min(o + 200, len(d) - 32)):
        try:
            vals = struct.unpack('<8f', d[off:off+32])
        except Exception:
            continue
        mn = vals[0:3]; w0 = vals[3]; mx = vals[4:7]; w1 = vals[7]
        if w0 == 0.0 and w1 == 0.0 and all(mn[k] < mx[k] for k in range(3)) \
           and all(abs(mn[k]) < 100 and abs(mx[k]) < 100 for k in range(3)) \
           and any(abs(mn[k]) > 1e-4 for k in range(3)):
            bbmin = np.array(mn); bbmax = np.array(mx); bbox_off = off
            break
    return {'flag': flag, 'vcount': vcount, 'fcount': fcount,
            'bbmin': bbmin, 'bbmax': bbmax, 'bbox_off': bbox_off,
            'after_bbox': (bbox_off + 32) if bbox_off is not None else o}


def find_faces(d, vcount, search_from, fcount=None):
    """Faces = int16-LE triples. Find the longest run of int16 in [0,vcount)
    that (a) starts at v[0]==0, (b) references index 0 and covers a good spread
    of the vertex range, and (c) length is a multiple of 3. fcount (index count
    from ViPm) is used to prefer the run whose length matches."""
    best = None  # (score, off, tris, end)
    o = search_from
    while o < len(d) - 6:
        # candidate run start: must begin with 0 and stay in range for >= 9 ints
        try:
            head = struct.unpack('<3h', d[o:o+6])
        except Exception:
            break
        if head[0] == 0 and all(0 <= h < vcount for h in head):
            idx = []
            p = o
            while p + 2 <= len(d):
                v = struct.unpack('<h', d[p:p+2])[0]
                if v < 0 or v >= vcount:
                    break
                idx.append(v); p += 2
            n3 = len(idx) - len(idx) % 3
            if n3 >= 9:
                uniq = len(set(idx[:n3]))
                # prefer run length matching fcount, then coverage, then length
                match = 0 if (fcount and n3 == fcount) else 1
                score = (match, -(uniq / max(vcount, 1)), -n3)
                if best is None or score < best[0]:
                    tris = [tuple(idx[k:k+3]) for k in range(0, n3, 3)]
                    best = (score, o, tris, p)
            o = p + 2
        else:
            o += 2
    if best:
        return best[1], best[2], best[3]
    return None, [], search_from


def parse_nodes(d):
    """Walk the node/frame tree collecting (name, 4x4 transform). The tree sits
    between the geom header (0x10) and the ViPm chunk. Each node: u32 namelen,
    name, body, 0xffffffff marker, 16 float row-major matrix. We locate nodes by
    their name strings and read the 16 floats after the following ffffffff."""
    nodes = []
    vipm = find_vipm(d)
    end = vipm if vipm > 0 else len(d)
    off = 0x10
    while off < end - 4:
        name, no = read_cstr_len(d, off)
        if name is None:
            off += 1
            continue
        # find the next 0xffffffff after the name
        marker = d.find(b'\xff\xff\xff\xff', no, end)
        transform = None
        if marker != -1 and marker + 4 + 64 <= len(d):
            try:
                transform = list(struct.unpack('<16f', d[marker+4:marker+4+64]))
            except Exception:
                transform = None
        if name.strip('\x00'):
            nodes.append({'name': name, 'transform': transform,
                          'name_off': off, 'marker_off': marker})
        off = no
    return nodes


# ---------------------------------------------------------------------------
# VERTICES — NOT SOLVED. Isolated hook.
# ---------------------------------------------------------------------------
VERTICES_SOLVED = False


def decode_vertices(d, vipm, vcount):
    """Return (Nx3 float array, solved: bool).

    NOT SOLVED. Placeholder: emits the raw quantized integers under the closest
    tested parameters (SoA, per-axis 12-bit, scaled by bbox). Produces a point
    cloud that FILLS the bbox but is NOT the true surface. Do not trust for
    rendering; present only so downstream plumbing is exercised.
    """
    bbmin = vipm['bbmin']; bbmax = vipm['bbmax']
    if bbmin is None:
        bbmin = np.zeros(3); bbmax = np.ones(3)
    span = bbmax - bbmin
    # best-effort placeholder: interpret bytes after bbox as 3*vcount bytes
    start = vipm['after_bbox']
    # skip the trailing count triple + zero padding that precedes the stream
    raw = d[start:start + 3 * vcount]
    if len(raw) < 3 * vcount:
        raw = raw + b'\x00' * (3 * vcount - len(raw))
    vi = np.frombuffer(raw, dtype=np.uint8).reshape(vcount, 3).astype(float)
    verts = vi * span / 255.0 + bbmin
    return verts, False


def parse_geom(geom_bytes):
    d = geom_bytes
    if d[:4] != GEOM_MAGIC:
        raise ValueError('not a geom blob (magic %s)' % d[:4].hex())
    nodes = parse_nodes(d)
    vi = find_vipm(d)
    if vi < 0:
        raise ValueError('no ViPm chunk')
    vipm = parse_vipm(d, vi)
    vcount = vipm['vcount']
    face_off, tris, face_end = find_faces(d, vcount, vipm['after_bbox'],
                                          vipm['fcount'])
    verts, solved = decode_vertices(d, vipm, vcount)
    return {
        'nodes': nodes,
        'vcount': vcount,
        'fcount': vipm['fcount'],
        'bbmin': None if vipm['bbmin'] is None else vipm['bbmin'].tolist(),
        'bbmax': None if vipm['bbmax'] is None else vipm['bbmax'].tolist(),
        'faces': tris,
        'face_off': face_off,
        'vertices': verts,
        'uvs': [],  # UVs not yet located (bitpacked with vertices)
        'vertices_solved': solved,
    }


# ---------------------------------------------------------------------------
# emit
# ---------------------------------------------------------------------------
def write_obj(path, verts, faces, mtl_name=None, part='mesh'):
    lines = []
    if mtl_name:
        lines.append('mtllib %s' % mtl_name)
    lines.append('o %s' % part)
    for v in verts:
        lines.append('v %.6f %.6f %.6f' % (v[0], v[1], v[2]))
    if mtl_name:
        lines.append('usemtl mat0')
    for f in faces:
        lines.append('f %d %d %d' % (f[0]+1, f[1]+1, f[2]+1))
    with open(path, 'w') as fh:
        fh.write('\n'.join(lines) + '\n')


def write_mtl(path, texture):
    lines = ['newmtl mat0', 'Kd 0.8 0.8 0.8']
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
            # decoded blob should be JPEG (FFD8) or bitmap; write as .jpg if JPEG
            stem = os.path.splitext(os.path.basename(w))[0]
            if data[:2] == b'\xff\xd8':
                fn = stem + '.jpg'
            else:
                fn = stem + '.bin'
            with open(os.path.join(tex_dir, fn), 'wb') as fh:
                fh.write(data)
            out.append(fn)
        except Exception as e:
            print('  tex FAIL %s: %s' % (os.path.basename(w), e))
    return out


def process_model(name, base):
    mdir = os.path.join(base, name)
    rdir = os.path.join(mdir, 'resources')
    if not os.path.isdir(rdir):
        return {'name': name, 'status': 'no_resources'}
    out_dir = os.path.join(OUT_ROOT, name)
    os.makedirs(out_dir, exist_ok=True)
    tex_files = decode_textures(rdir, os.path.join(out_dir, 'textures'))

    parts = []
    status = []
    any_solved = True
    for gp in sorted(glob.glob(os.path.join(rdir, '*.wsgo'))):
        part = os.path.splitext(os.path.basename(gp))[0]
        try:
            gb = container.decode_file(gp)
            if gb[:4] != GEOM_MAGIC:
                status.append((part, 'not_geom', gb[:4].hex()))
                continue
            g = parse_geom(gb)
        except Exception as e:
            status.append((part, 'parse_fail', str(e)))
            continue
        any_solved = any_solved and g['vertices_solved']
        obj_name = '%s.obj' % part
        mtl_name = '%s.mtl' % part
        write_obj(os.path.join(out_dir, obj_name), g['vertices'], g['faces'],
                  mtl_name, part)
        tex = ('textures/%s' % tex_files[0]) if tex_files else None
        write_mtl(os.path.join(out_dir, mtl_name), tex)
        node0 = g['nodes'][0] if g['nodes'] else {}
        transform = node0.get('transform') or _identity()
        parts.append({
            'mesh': obj_name,
            'textures': [os.path.basename(t) for t in tex_files],
            'transform': transform,
            'vcount': g['vcount'], 'faces': len(g['faces']),
            'nodes': [{'name': n['name'], 'transform': n['transform']}
                      for n in g['nodes']],
            'vertices_solved': g['vertices_solved'],
            'bbmin': g['bbmin'], 'bbmax': g['bbmax'],
        })
        status.append((part, 'ok', '%dv/%dtris solved=%s'
                       % (g['vcount'], len(g['faces']), g['vertices_solved'])))

    manifest = {
        'name': name,
        'upAxis': 'Y',
        'scale': 1.0,
        'vertices_solved': all(p['vertices_solved'] for p in parts) and bool(parts),
        'parts': [{'mesh': p['mesh'], 'textures': p['textures'],
                   'transform': p['transform']} for p in parts],
        '_debug_parts': parts,
        '_status': status,
    }
    with open(os.path.join(out_dir, 'model.json'), 'w') as fh:
        json.dump(manifest, fh, indent=2, default=str)
    return {'name': name, 'status': status, 'parts': len(parts)}


def _identity():
    return [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]


def main():
    args = sys.argv[1:]
    targets = []
    if args:
        for a in args:
            for base in (ASSETS_OBJ, ASSETS_PROP):
                if os.path.isdir(os.path.join(base, a)):
                    targets.append((a, base))
    else:
        for base in (ASSETS_OBJ, ASSETS_PROP):
            for d in sorted(os.listdir(base)):
                if os.path.isdir(os.path.join(base, d, 'resources')):
                    targets.append((d, base))
    summary = []
    for name, base in targets:
        print('== %s (%s)' % (name, os.path.basename(base)))
        try:
            r = process_model(name, base)
        except Exception as e:
            traceback.print_exc()
            r = {'name': name, 'error': str(e)}
        for s in r.get('status', []):
            print('   %-12s %-14s %s' % s)
        summary.append(r)
    with open(os.path.join(OUT_ROOT, '_wsgo2obj_summary.json'), 'w') as fh:
        json.dump(summary, fh, indent=2, default=str)
    print('\nSummary -> %s/_wsgo2obj_summary.json' % OUT_ROOT)


if __name__ == '__main__':
    main()
