#!/usr/bin/env python3
"""wsgo_decode -- SOLVED WildTangent .wsgo "geom" (ViPm) decoder.

Pipeline:  container-decode -> parse header (bones/materials/node transforms)
           -> locate ViPm -> read (vcount,fcount) per-LOD -> parse the packed
           axis blocks (positions, normals, UVs) -> faces (int16 triples).

KEY CRACK (2026-07-06, from the recovered actorobject.dll + empirical fit):
The packed vertex data is stored as **per-axis SoA blocks**, each block:
    [float32 min][float32 max][ vcount high-bytes ][ vcount low-bytes ]
i.e. each 16-bit quantized value is stored **byte-PLANAR** (all high bytes,
then all low bytes), NOT interleaved.  value = (hi<<8)|lo, then
    coord = min + value/65535 * (max-min).
This byte-plane transposition is exactly what defeated every prior interleaved
u16 / 8-bit read (those mixed adjacent vertices, producing the "scrambled"
point cloud). There is NO vertex reorder -- faces index the stored order
directly once the planar decode is applied.

Block order per part: 3 position axes, a per-LOD material descriptor
("futs" tag records), 3 normal axes, 2 UV axes, then int16 face triples
(quads split 0,1,2 / 2,3,0), then a per-vertex trailer (LODmask + bone).

Public: decode_part(geom_bytes) -> dict, and decode_wsgo(path) -> dict.
"""
import struct
import numpy as np

GEOM_MAGIC = bytes.fromhex('17fc8e07')


def _f32(d, o):
    return struct.unpack('<f', d[o:o + 4])[0]


def _u32(d, o):
    return struct.unpack('<I', d[o:o + 4])[0]


def _planar_block(d, start, vc):
    """Decode one [f32 min][f32 max][vc hi][vc lo] axis block -> vc floats."""
    mn = _f32(d, start)
    mx = _f32(d, start + 4)
    b = np.frombuffer(d[start + 8:start + 8 + 2 * vc], dtype='<u1')
    if len(b) < 2 * vc:
        b = np.concatenate([b, np.zeros(2 * vc - len(b), np.uint8)])
    hi = b[:vc].astype(np.uint16)
    lo = b[vc:2 * vc].astype(np.uint16)
    v = (hi << 8) | lo
    return mn + v.astype(np.float64) / 65535.0 * (mx - mn)


def _valid_block(d, o, vc):
    if o + 8 + 2 * vc > len(d):
        return False
    mn = _f32(d, o)
    mx = _f32(d, o + 4)
    return (mn < mx and -1e4 < mn < 1e4 and -1e4 < mx < 1e4
            and (mx - mn) > 1e-5)


def _find_faces(d, vc, start_from):
    """Longest int16-LE triple run whose values are all in [0,vc)."""
    best = None
    o = start_from
    while o < len(d) - 6:
        t = struct.unpack('<3h', d[o:o + 6])
        if t[0] == 0 and all(0 <= x < vc for x in t):
            p = o
            idx = []
            while p + 2 <= len(d):
                v = struct.unpack('<h', d[p:p + 2])[0]
                if v < 0 or v >= vc:
                    break
                idx.append(v)
                p += 2
            n = len(idx) - len(idx) % 3
            if n >= 3 and (best is None or n > best[1]):
                best = (o, n)
            o = p if p > o else o + 2
        else:
            o += 2
    return best


def decode_part(d):
    """d = raw geom blob (starts with GEOM_MAGIC). Returns dict."""
    if d[:4] != GEOM_MAGIC:
        raise ValueError('not a geom blob: %s' % d[:4].hex())
    nodes = _parse_nodes(d)
    vi = d.find(b'ViPm')
    if vi < 0:
        raise ValueError('no ViPm chunk')
    vc = _u32(d, vi + 8)
    fc = _u32(d, vi + 12)
    if vc <= 0 or vc > 200000:
        raise ValueError('bad vcount %d' % vc)
    bs = 8 + 2 * vc

    # faces first (they bound the packed-block region)
    fres = _find_faces(d, vc, vi + 16)
    if not fres:
        raise ValueError('no face run found')
    foff, fn = fres
    faces = np.frombuffer(d[foff:foff + fn * 2], dtype='<i2').reshape(-1, 3)

    # collect every candidate axis block between ViPm header and faces
    cand = []
    o = vi + 16
    while o < foff - 8:
        if _valid_block(d, o, vc):
            cand.append(o)
        o += 1

    # positions: the 3 consecutive (stride bs) blocks giving the most
    # coherent mesh (smallest median triangle max-edge / bbox diagonal).
    pos_start = _best_position_triple(d, vc, bs, faces, foff)
    P = np.stack([_planar_block(d, pos_start + bs * k, vc) for k in range(3)],
                 axis=1)

    # normals: first stride-bs triple AFTER positions whose 3 axes all sit in
    # ~[-1,1] and whose decoded vectors are ~unit length.
    N = _find_normals(d, vc, bs, pos_start, foff)
    # UVs: a stride-bs pair after normals, axes in a bounded range.
    UV = _find_uvs(d, vc, bs, pos_start, foff)

    return {
        'nodes': nodes, 'vcount': vc, 'fcount': fc,
        'faces': faces, 'positions': P, 'normals': N, 'uvs': UV,
        'pos_start': pos_start, 'face_off': foff,
    }


def _triple_score(d, vc, bs, start, faces):
    if not all(_valid_block(d, start + bs * k, vc) for k in range(3)):
        return None
    V = np.stack([_planar_block(d, start + bs * k, vc) for k in range(3)],
                 axis=1)
    diag = np.linalg.norm(V.max(0) - V.min(0))
    if diag < 1e-4:
        return None
    per = np.array([
        max(np.linalg.norm(V[f[0]] - V[f[1]]),
            np.linalg.norm(V[f[1]] - V[f[2]]),
            np.linalg.norm(V[f[2]] - V[f[0]])) for f in faces]) / diag
    return float(np.median(per))


def _best_position_triple(d, vc, bs, faces, foff):
    """Positions are the FIRST coherent block-triple in file order.
    Normal/UV blocks are also coherent but come after positions; picking the
    globally-minimal score wrongly selects a normalized normal/UV triple, so we
    take the earliest triple that clears a coherence threshold. Fall back to the
    global minimum if none clears it."""
    lo = _find_first_block(d, vc)
    THRESH = 0.20
    best = None
    for start in range(lo, foff - 3 * bs):
        s = _triple_score(d, vc, bs, start, faces)
        if s is None:
            continue
        if s < THRESH:
            return start
        if best is None or s < best[0]:
            best = (s, start)
    if best is None:
        raise ValueError('no position triple found')
    return best[1]


def _find_first_block(d, vc):
    o = d.find(b'ViPm') + 16
    while o < len(d) - 8:
        if _valid_block(d, o, vc):
            return o
        o += 1
    return o


def _find_normals(d, vc, bs, pos_start, foff):
    start = pos_start + 3 * bs
    while start < foff - 3 * bs:
        if all(_valid_block(d, start + bs * k, vc) for k in range(3)):
            axes = []
            good = True
            for k in range(3):
                mn = _f32(d, start + bs * k)
                mx = _f32(d, start + bs * k + 4)
                if mn < -1.2 or mx > 1.2:
                    good = False
                    break
                axes.append(_planar_block(d, start + bs * k, vc))
            if good:
                N = np.stack(axes, axis=1)
                mag = np.linalg.norm(N, axis=1)
                if 0.7 < np.median(mag) < 1.3:
                    return N
        start += 1
    return None


def _find_uvs(d, vc, bs, pos_start, foff):
    # UVs are the last stride-bs pair before the faces.
    start = foff - 2 * bs
    if start >= pos_start and all(_valid_block(d, start + bs * k, vc)
                                  for k in range(2)):
        return np.stack([_planar_block(d, start + bs * k, vc)
                         for k in range(2)], axis=1)
    return None


# --- node / transform tree (names + 4x4 matrices) ---------------------------
def _read_cstr_len(d, off):
    if off + 4 > len(d):
        return None, off
    ln = _u32(d, off)
    if ln == 0 or ln > 128 or off + 4 + ln > len(d):
        return None, off
    raw = d[off + 4:off + 4 + ln]
    if raw[-1:] != b'\x00':
        return None, off
    try:
        return raw[:-1].decode('latin1'), off + 4 + ln
    except Exception:
        return None, off


def _parse_nodes(d):
    nodes = []
    vi = d.find(b'ViPm')
    end = vi if vi > 0 else len(d)
    off = 0x10
    while off < end - 4:
        name, no = _read_cstr_len(d, off)
        if name is None:
            off += 1
            continue
        marker = d.find(b'\xff\xff\xff\xff', no, end)
        transform = None
        if marker != -1 and marker + 4 + 64 <= len(d):
            try:
                transform = list(struct.unpack('<16f', d[marker + 4:marker + 68]))
            except Exception:
                transform = None
        if name.strip('\x00') and all(32 <= ord(c) < 127 for c in name):
            nodes.append({'name': name, 'transform': transform})
        off = no
    return nodes


def decode_wsgo(path):
    import sys
    sys.path.insert(0, '/tmp/cb/tools')
    import container
    return decode_part(container.decode_file(path))


if __name__ == '__main__':
    import sys
    r = decode_wsgo(sys.argv[1])
    P = r['positions']
    print('vc=%d tris=%d pos_start=%d faces@%d' %
          (r['vcount'], len(r['faces']), r['pos_start'], r['face_off']))
    print('bbox', P.min(0), P.max(0))
    print('normals', None if r['normals'] is None else r['normals'].shape,
          'uvs', None if r['uvs'] is None else r['uvs'].shape)
    print('nodes', [n['name'] for n in r['nodes']][:8])
