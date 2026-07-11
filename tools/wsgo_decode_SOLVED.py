#!/usr/bin/env python3
"""
SOLVED decoder for the WildTangent `.wsgo` "geom" / VIPM mesh format.

Reverse-engineered from actorobject.dll + WDENGINE.dll (recovered from the 86Box
XP image). The mesh vertex/normal/uv data is stored as byte-PLANAR 16-bit quantized
arrays (this was the key that broke 5 sessions of failed guessing):

  geVFileUtil_ReadFloatArray16(vfile, count, stride, min, max):
      read count HIGH bytes, then count LOW bytes  (two separate planes)
      u16[i]  = (hi[i] << 8) | lo[i]
      out[i]  = min + u16[i]/65535.0 * (max - min)         # 1.5259e-05 == 1/65535
    (degenerate: if max - min < ~0.000797, the axis is constant = min)

  A vec3 array (positions, normals) = 3 consecutive per-component blocks, each:
      [f32 min][f32 max][count high-bytes][count low-bytes]

  VIPM_CreateFromFile: "ViPm" magic + u32 version + ~124-byte header struct; the
  first u32 after version (== ViPm+8) is the render vertex count N. Positions begin
  at ViPm+132. Faces are int16 triangle indices (values < N), quad-split winding.
"""
import struct, numpy as np

def read_comp(d, o, N):
    mn, mx = struct.unpack_from('<ff', d, o)
    hi = np.frombuffer(d[o+8:o+8+N], np.uint8).astype(np.uint32)
    lo = np.frombuffer(d[o+8+N:o+8+2*N], np.uint8)
    return mn + (hi*256 + lo)/65535.0 * (mx - mn)

def decode_geom(d):
    """d = container-decoded .wsgo 'geom' blob. Returns (verts Nx3, faces list)."""
    vip = d.find(b"ViPm")
    N = struct.unpack_from('<I', d, vip+8)[0]     # render vertex count
    ps = vip + 132                                 # positions start (after 124B header)
    st = 8 + 2*N                                   # per-component block size
    V = np.stack([read_comp(d, ps, N),
                  read_comp(d, ps+st, N),
                  read_comp(d, ps+2*st, N)], 1)
    # faces: longest int16 run of indices < N
    NN=len(d); best=(0,0); o=vip
    while o+2<NN:
        j=o; c=0
        while j+2<=NN and struct.unpack_from('<H',d,j)[0]<N: c+=1; j+=2
        if c>best[0]: best=(c,o)
        o += max(1,c*2) if c>3 else 1
    o=best[1]; idx=[]
    while o+2<=NN:
        v=struct.unpack_from('<H',d,o)[0]
        if v>=N: break
        idx.append(v); o+=2
    faces=[tuple(idx[i:i+3]) for i in range(0,(len(idx)//3)*3,3)]
    return V, faces

if __name__=="__main__":
    import sys, subprocess
    WT="/tmp/cb/WTExtractor/pywttools/wtextract.py"; PY=sys.executable
    subprocess.run([PY,WT,sys.argv[1],"/tmp/_g.bin","-q"])
    V,faces=decode_geom(open("/tmp/_g.bin","rb").read())
    print(f"{len(V)} verts, {len(faces)} faces")
    with open(sys.argv[2] if len(sys.argv)>2 else "out.obj","w") as f:
        for v in V: f.write(f"v {v[0]} {v[1]} {v[2]}\n")
        for a,b,c in faces: f.write(f"f {a+1} {b+1} {c+1}\n")
