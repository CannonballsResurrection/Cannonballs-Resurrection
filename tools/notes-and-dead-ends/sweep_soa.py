#!/usr/bin/env python3
"""SoA hypothesis: stream = [all X (nv * wx bits)] [all Y] [all Z], each axis
with its own width. Sweep start, widths, orders. Also try the inline per-axis
min/max floats seen at 574 as the actual bbox."""
import struct
import numpy as np
from bitarray import bitarray

D=open('/tmp/cb/analysis/arrow.bin','rb').read()
VCOUNT=316
BBMIN=np.array([-0.1775510,-0.6173291,-1.2202568])
BBMAX=np.array([0.1775510,0.6173290,1.1933800])
SPAN=BBMAX-BBMIN;MAXDIM=SPAN.max()

def load_faces():
    off=5742;f=[]
    while off+2<=len(D):
        v=struct.unpack('<h',D[off:off+2])[0]
        if v<0 or v>=VCOUNT:break
        f.append(v);off+=2
    return np.array([tuple(f[i:i+3]) for i in range(0,len(f)-len(f)%3,3)])
TRIS=load_faces()

def bitsarr(order):
    ba=bitarray(endian=order);ba.frombytes(D)
    return np.frombuffer(ba.unpack(zero=b'\x00',one=b'\x01'),dtype=np.uint8)
BL=bitsarr('little');BB=bitsarr('big')

def col(barr,pos,w,nv,fmsb):
    seg=barr[pos:pos+w*nv].reshape(nv,w).astype(np.int64)
    wt=(1<<np.arange(w-1,-1,-1)) if fmsb else (1<<np.arange(w))
    return seg@wt.astype(np.int64), pos+w*nv

def planarity(v):
    nq=len(v)//4;q=v[:nq*4].reshape(nq,4,3)
    nm=np.cross(q[:,1]-q[:,0],q[:,2]-q[:,0]);nn=np.linalg.norm(nm,axis=1);ok=nn>1e-9
    if ok.sum()<3:return 9e9
    nmn=nm[ok]/nn[ok,None];d=np.abs(np.einsum('ij,ij->i',q[ok,3]-q[ok,0],nmn))
    sc=np.linalg.norm(q[ok,2]-q[ok,0],axis=1)+1e-9;return float((d/sc).mean())
def cov(v):
    a=v[TRIS[:,0]];b=v[TRIS[:,1]];c=v[TRIS[:,2]]
    ar=0.5*np.linalg.norm(np.cross(b-a,c-a),axis=1);return ar.std()/(ar.mean()+1e-12)

res=[]
for oname,barr in [('lsb',BL),('msb',BB)]:
    for fmsb in [True,False]:
        for start in range(560*8,600*8,1):
            for w in range(8,18):  # uniform width per axis, common case
                pos=start
                x,pos=col(barr,pos,w,VCOUNT,fmsb)
                y,pos=col(barr,pos,w,VCOUNT,fmsb)
                z,pos=col(barr,pos,w,VCOUNT,fmsb)
                v=np.stack([x,y,z],1)
                if v.std()<1:continue
                for per in [False,True]:
                    odd=(1<<w)-1
                    vv=v*(SPAN if per else MAXDIM)/odd+BBMIN
                    pl=planarity(vv);cv=cov(vv)
                    if pl+cv<0.9:
                        res.append((pl+cv,pl,cv,oname,fmsb,start//8,start%8,w,per))
res.sort()
for r in res[:20]:
    print(f'sum={r[0]:.4f} pl={r[1]:.4f} cov={r[2]:.4f} {r[3]} fmsb={r[4]} start={r[5]}.{r[6]} w={r[7]} per={r[8]}')
print('candidates<0.9:',len(res))
