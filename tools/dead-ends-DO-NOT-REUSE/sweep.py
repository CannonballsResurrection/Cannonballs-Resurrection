#!/usr/bin/env python3
"""Brute sweep: unpack bits from a byte-offset with the PWT bit order,
per-component width = acc - unused[axis], dequant with global bbox,
score by planarity+area CoV. Vectorized via a precomputed bit array."""
import struct, sys
import numpy as np
from bitarray import bitarray

D = open('/tmp/cb/analysis/arrow.bin','rb').read()
VCOUNT=316
BBMIN=np.array([-0.1775510,-0.6173291,-1.2202568])
BBMAX=np.array([0.1775510,0.6173290,1.1933800])
SPAN=BBMAX-BBMIN
MAXDIM=SPAN.max()

def load_faces():
    off=5742; f=[]
    while off+2<=len(D):
        v=struct.unpack('<h',D[off:off+2])[0]
        if v<0 or v>=VCOUNT: break
        f.append(v); off+=2
    return [tuple(f[i:i+3]) for i in range(0,len(f)-len(f)%3,3)]
TRIS=np.array(load_faces())

def make_bits(order):
    """Return a numpy uint8 array of bits for the whole file.
    order='lsb': within each byte, bit0 first (matches PWT low-bit-first).
    order='msb': bit7 first."""
    ba=bitarray(endian='little' if order=='lsb' else 'big')
    ba.frombytes(D)
    return np.frombuffer(ba.unpack(zero=b'\x00',one=b'\x01'),dtype=np.uint8)

BITS_LSB=make_bits('lsb')
BITS_MSB=make_bits('msb')

def read_fields_be(bits, bitpos, width):
    """Read `width` bits starting at bitpos, interpret with PWT convention:
    PWT readbits reads whole bytes low-first then reverses byte order (big-endian
    assembly). For non-byte-multiple widths it's messy; approximate the common
    case by treating the width-bit field as a big-endian integer over the bit
    stream (MSB of field = first bit). We test both bit orders empirically."""
    pass

def unpack_stream(start_bit, widths, nverts, bits, msb_field):
    """widths=(wx,wy,wz). Read nverts*(wx+wy+wz) bits from `bits` starting
    start_bit. Each component = integer from its `w` bits.
    msb_field: if True first bit is MSB else LSB."""
    total=sum(widths)*nverts
    if start_bit+total>len(bits): return None
    seg=bits[start_bit:start_bit+total]
    # build per-vertex
    wx,wy,wz=widths
    stride=wx+wy+wz
    seg=seg[:stride*nverts].reshape(nverts,stride)
    def tovals(block,w):
        # block: (nverts,w) bits
        if msb_field:
            weights=(1<<np.arange(w-1,-1,-1)).astype(np.int64)
        else:
            weights=(1<<np.arange(w)).astype(np.int64)
        return block.astype(np.int64)@weights
    x=tovals(seg[:,0:wx],wx)
    y=tovals(seg[:,wx:wx+wy],wy)
    z=tovals(seg[:,wx+wy:wx+wy+wz],wz)
    return np.stack([x,y,z],axis=1).astype(float)

def dequant(vints, acc, per_axis):
    odd=(1<<acc)-1
    if per_axis:
        return vints*SPAN/odd+BBMIN
    return vints*MAXDIM/odd+BBMIN

def score(verts):
    if verts is None or len(verts)<VCOUNT: return 9e9,9e9
    a=verts[TRIS[:,0]]; b=verts[TRIS[:,1]]; c=verts[TRIS[:,2]]
    ar=0.5*np.linalg.norm(np.cross(b-a,c-a),axis=1)
    m=ar.mean()
    if m<=0: return 9e9,9e9
    cov=ar.std()/m
    nq=VCOUNT//4
    q=verts[:nq*4].reshape(nq,4,3)
    n=np.cross(q[:,1]-q[:,0],q[:,2]-q[:,0])
    nn=np.linalg.norm(n,axis=1)
    ok=nn>1e-9
    n=n[ok]/nn[ok,None]
    d=np.abs(np.einsum('ij,ij->i',q[ok,3]-q[ok,0],n))
    sc=np.linalg.norm(q[ok,2]-q[ok,0],axis=1)+1e-9
    pl=(d/sc).mean() if ok.any() else 9e9
    return cov,pl

def main():
    results=[]
    # candidate byte starts around the packed stream (574..586), plus header re-reads
    for order,bits in [('lsb',BITS_LSB),('msb',BITS_MSB)]:
        for msb_field in [True,False]:
            for start_byte in range(566, 590):
                sb=start_byte*8
                for acc in range(6,25):
                    for ux in range(0,acc-3):
                        # assume uniform unused across axes first (common)
                        w=acc-ux
                        if w<2: continue
                        for per_axis in [False,True]:
                            v=unpack_stream(sb,(w,w,w),40,bits,msb_field)
                            if v is None: continue
                            vv=dequant(v,acc,per_axis)
                            cov,pl=score_partial(vv)
                            results.append((pl,cov,order,msb_field,start_byte,acc,ux,w,per_axis))
    results.sort()
    for r in results[:25]:
        print(f"pl={r[0]:.4f} cov={r[1]:.4f} order={r[2]} msbfield={r[3]} start={r[4]} acc={r[5]} unused={r[6]} w={r[7]} peraxis={r[8]}")

def score_partial(verts):
    # planarity over first 40 verts (10 quads) + area cov over tris using only <40 idx
    if verts is None: return 9e9,9e9
    n=len(verts)
    nq=n//4
    q=verts[:nq*4].reshape(nq,4,3)
    nm=np.cross(q[:,1]-q[:,0],q[:,2]-q[:,0])
    nn=np.linalg.norm(nm,axis=1)
    ok=nn>1e-9
    if not ok.any(): return 9e9,9e9
    nmn=nm[ok]/nn[ok,None]
    d=np.abs(np.einsum('ij,ij->i',q[ok,3]-q[ok,0],nmn))
    sc=np.linalg.norm(q[ok,2]-q[ok,0],axis=1)+1e-9
    pl=(d/sc).mean()
    # crude area cov using consecutive tris within range
    tsel=TRIS[(TRIS<n).all(axis=1)]
    if len(tsel)<4: return pl,9e9
    a=verts[tsel[:,0]];b=verts[tsel[:,1]];c=verts[tsel[:,2]]
    ar=0.5*np.linalg.norm(np.cross(b-a,c-a),axis=1)
    cov=ar.std()/(ar.mean()+1e-12)
    return pl,cov

if __name__=='__main__':
    main()
