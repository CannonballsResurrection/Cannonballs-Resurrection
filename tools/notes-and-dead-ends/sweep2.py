#!/usr/bin/env python3
"""Fine sweep over bit-start and per-vertex geometry. Assume 3 comps/vert,
uniform width w per comp (2..12). Try both field-endianness. LSB byte order
(matched the repeat). Score planarity on first 60 verts."""
import struct
import numpy as np
from bitarray import bitarray

D=open('/tmp/cb/analysis/arrow.bin','rb').read()
VCOUNT=316
BBMIN=np.array([-0.1775510,-0.6173291,-1.2202568])
BBMAX=np.array([0.1775510,0.6173290,1.1933800])
SPAN=BBMAX-BBMIN; MAXDIM=SPAN.max()

def load_faces():
    off=5742;f=[]
    while off+2<=len(D):
        v=struct.unpack('<h',D[off:off+2])[0]
        if v<0 or v>=VCOUNT:break
        f.append(v);off+=2
    return np.array([tuple(f[i:i+3]) for i in range(0,len(f)-len(f)%3,3)])
TRIS=load_faces()

def bits(order):
    ba=bitarray(endian=order);ba.frombytes(D)
    return np.frombuffer(ba.unpack(zero=b'\x00',one=b'\x01'),dtype=np.uint8)
BL=bits('little');BB=bits('big')

def unpack(bitsarr,start,w,nverts,field_msb):
    stride=3*w
    if start+stride*nverts>len(bitsarr):return None
    seg=bitsarr[start:start+stride*nverts].reshape(nverts,3,w).astype(np.int64)
    if field_msb:
        wt=(1<<np.arange(w-1,-1,-1)).astype(np.int64)
    else:
        wt=(1<<np.arange(w)).astype(np.int64)
    return seg@wt   # nverts x 3

def deq(vi,w,per_axis):
    odd=(1<<w)-1
    if per_axis: return vi*SPAN/odd+BBMIN
    return vi*MAXDIM/odd+BBMIN

def planarity(v):
    n=len(v);nq=n//4
    q=v[:nq*4].reshape(nq,4,3)
    nm=np.cross(q[:,1]-q[:,0],q[:,2]-q[:,0])
    nn=np.linalg.norm(nm,axis=1);ok=nn>1e-9
    if ok.sum()<3:return 9e9
    nmn=nm[ok]/nn[ok,None]
    d=np.abs(np.einsum('ij,ij->i',q[ok,3]-q[ok,0],nmn))
    sc=np.linalg.norm(q[ok,2]-q[ok,0],axis=1)+1e-9
    return float((d/sc).mean())

res=[]
NV=60
for oname,barr in [('lsb',BL),('msb',BB)]:
    for fmsb in [True,False]:
        for w in range(2,13):
            for start in range(560*8, 600*8):
                v=unpack(barr,start,w,NV,fmsb)
                if v is None:continue
                # spread check: reject degenerate (all same)
                if v.std()<1: continue
                vv=deq(v,w,False)
                pl=planarity(vv)
                res.append((pl,oname,fmsb,w,start,start//8,start%8))
res.sort()
for r in res[:30]:
    print(f"pl={r[0]:.5f} order={r[1]} fmsb={r[2]} w={r[3]} bitstart={r[4]} byte={r[5]} bitoff={r[6]}")
