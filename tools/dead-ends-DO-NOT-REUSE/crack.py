#!/usr/bin/env python3
"""Vertex-crack harness for .wsgo geom. Replicates PWT BitfieldReader,
tries decoding arrow vertices at various stream starts / accuracies /
bit orders, scores by quad planarity + triangle-area CoV."""
import struct, math, sys
import numpy as np

D = open('/tmp/cb/analysis/arrow.bin','rb').read()
VCOUNT = 316
BBMIN = np.array([-0.1775510, -0.6173291, -1.2202568])
BBMAX = np.array([ 0.1775510,  0.6173290,  1.1933800])

# faces (quads split): read int16 LE triples from 5742 until >=316
def load_faces():
    off=5742; faces=[]
    while off+2<=len(D):
        v=struct.unpack('<h', D[off:off+2])[0]
        if v<0 or v>=VCOUNT: break
        faces.append(v); off+=2
    tris=[tuple(faces[i:i+3]) for i in range(0,len(faces)-len(faces)%3,3)]
    return tris
TRIS=load_faces()

# ---- PWT-style bit reader (big-endian sub-byte order). Replicated exactly. ----
class BitReaderBE:
    """Matches pwtdecode.BitfieldReader: reads bytes low-bit-first, assembles
    big-endian ints. readbits(n) returns bytes to be unpacked as >i."""
    def __init__(self, buf, start=0):
        self.buf=buf; self.pos=start; self.bytebit=0; self.cur=None
    def _load(self):
        self.cur=self.buf[self.pos]; self.pos+=1
    def _consume(self, count):
        if self.bytebit==0:
            self._load()
        res=(self.cur>>self.bytebit)&((1<<count)-1)
        self.bytebit=(self.bytebit+count)%8
        return res
    @property
    def _rem(self): return 8-self.bytebit
    def _readbyte(self):
        if self.bytebit==0:
            return self._consume(8)
        need=self.bytebit
        rem=self._rem
        return self._consume(rem) | (self._consume(need)<<(8-need))
    def _readsub(self, bc):
        left=self._rem
        if left<bc:
            nn=bc-left
            return self._consume(left)|(self._consume(nn)<<left)
        return self._consume(bc)
    def readbits(self, bc):
        out=[]
        for _ in range(bc//8): out.append(self._readbyte())
        if bc%8: out.append(self._readsub(bc%8))
        return bytes(out[::-1])
    def read_int(self, bc):
        b=self.readbits(bc)
        b=b'\x00'*(4-len(b))+b
        return struct.unpack('>i', b)[0]

def dequant_pwt(start, accuracy, use_local_min=True, per_axis_scale=False, nverts=VCOUNT):
    """Follow PWT vertex path: at `start`, read vcount(32),comp(32),bboxMIN(3f),
    3x6bit unused, then packed. Returns Nx3 float array or None."""
    br=BitReaderBE(D, start)
    try:
        vc=br.read_int(32); vcomp=br.read_int(32)
    except Exception: return None
    # These may not match; we skip validation and just use passed structure.
    # bboxMIN 3 floats (big-endian per pwt)
    mn=struct.unpack('>3f', br.readbits(96))
    bbx=struct.unpack('>B', br.readbits(6))[0]
    bby=struct.unpack('>B', br.readbits(6))[0]
    bbz=struct.unpack('>B', br.readbits(6))[0]
    if bbx>accuracy or bby>accuracy or bbz>accuracy: return None
    verts=[]
    for _ in range(nverts):
        x=br.read_int(accuracy-bbx); y=br.read_int(accuracy-bby); z=br.read_int(accuracy-bbz)
        verts.append((x,y,z))
    verts=np.array(verts,dtype=float)
    span=(BBMAX-BBMIN)
    maxdim=span.max()
    odd=(1<<accuracy)-1
    minv=np.array(mn) if use_local_min else BBMIN
    if per_axis_scale:
        out=verts*span/odd+minv
    else:
        out=verts*maxdim/odd+minv
    return out

def score(verts):
    if verts is None or len(verts)<VCOUNT: return 1e9,1e9
    # triangle area CoV
    areas=[]
    for a,b,c in TRIS:
        va,vb,vc=verts[a],verts[b],verts[c]
        ar=0.5*np.linalg.norm(np.cross(vb-va,vc-va))
        areas.append(ar)
    areas=np.array(areas)
    if areas.mean()<=0: return 1e9,1e9
    cov=areas.std()/areas.mean()
    # quad planarity: quads are verts {4k..4k+3}
    planar=[]
    nq=VCOUNT//4
    for k in range(nq):
        q=verts[4*k:4*k+4]
        # fit plane via normal of first tri, measure 4th vert dist
        n=np.cross(q[1]-q[0], q[2]-q[0])
        nn=np.linalg.norm(n)
        if nn<1e-12: continue
        n/=nn
        d=abs(np.dot(q[3]-q[0], n))
        scale=np.linalg.norm(q[2]-q[0])+1e-9
        planar.append(d/scale)
    planar=np.mean(planar) if planar else 1e9
    return cov, planar

if __name__=='__main__':
    # quick sweep
    results=[]
    for start in range(566, 600):
        for acc in range(8, 25):
            v=dequant_pwt(start, acc, nverts=40)
            if v is None: continue
            cov,pl=score_partial(v) if False else (None,None)
    print("harness ready. TRIS:", len(TRIS))
