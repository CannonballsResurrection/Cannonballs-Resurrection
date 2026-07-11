#!/usr/bin/env python3
"""Faithful PWT-scheme sweep with per-axis unused bits and LE/BE bit orders.
Header at 554: vertexcount(32),vcomp(32). Then sweep the byte/bit start of the
unused-bits header (3x6) and accuracy; packed stream follows. Full planarity+cov."""
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

def readval(barr,pos,w,fmsb):
    seg=barr[pos:pos+w].astype(np.int64)
    if fmsb: wt=(1<<np.arange(w-1,-1,-1))
    else: wt=(1<<np.arange(w))
    return int(seg@wt.astype(np.int64))

def unpack_verts(barr,pos,ws,nv,fmsb):
    wx,wy,wz=ws;stride=wx+wy+wz
    need=stride*nv
    if pos+need>len(barr):return None
    seg=barr[pos:pos+need].reshape(nv,stride).astype(np.int64)
    def col(b,w):
        wt=(1<<np.arange(w-1,-1,-1)) if fmsb else (1<<np.arange(w))
        return b@wt.astype(np.int64)
    x=col(seg[:,:wx],wx);y=col(seg[:,wx:wx+wy],wy);z=col(seg[:,wx+wy:],wz)
    return np.stack([x,y,z],1)

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
NV=VCOUNT
# unused header may be at 566..574 bit region; packed after. accuracy 8..20.
for oname,barr in [('lsb',BL),('msb',BB)]:
    for fmsb in [True,False]:
        for hdrbit in range(560*8, 576*8):  # where the 3x6 unused sits
            ux=readval(barr,hdrbit,6,fmsb)
            uy=readval(barr,hdrbit+6,6,fmsb)
            uz=readval(barr,hdrbit+12,6,fmsb)
            for acc in range(8,21):
                wx,wy,wz=acc-ux,acc-uy,acc-uz
                if min(wx,wy,wz)<2 or max(wx,wy,wz)>20:continue
                pos=hdrbit+18
                v=unpack_verts(barr,pos,(wx,wy,wz),NV,fmsb)
                if v is None:continue
                if v.std()<1:continue
                for per in [False,True]:
                    odd=(1<<acc)-1
                    vv=v*(SPAN if per else MAXDIM)/odd+BBMIN
                    pl=planarity(vv)
                    if pl<0.15:
                        res.append((pl,cov(vv),oname,fmsb,hdrbit,hdrbit//8,hdrbit%8,acc,ux,uy,uz,per))
res.sort()
for r in res[:25]:
    print(f"pl={r[0]:.4f} cov={r[1]:.4f} {r[2]} fmsb={r[3]} hdrbyte={r[5]}.{r[6]} acc={r[7]} u=({r[8]},{r[9]},{r[10]}) per={r[11]}")
print('total under 0.15:',len(res))
