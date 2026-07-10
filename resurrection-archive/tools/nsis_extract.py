#!/usr/bin/env python3
"""Extract the payload from a ~2002-era WildTangent NSIS self-extracting installer.

Modern 7-Zip can't decode this old NSIS script; it exposes the raw payload blob as a file named
'[0]'. Extract that blob first (7z x installer.exe), then run:

    python3 nsis_extract.py path/to/[0] [out_dir]

It walks the NSIS data section (each entry is [uint32 size][deflate data]; high bit of size = the
block is zlib-deflated), inflates each block, and writes them as block_NNN.bin. Identify the results
with `file block_*.bin` (PE exes/DLLs, the WLD3 .cab, HTML/JS, .wt assets, etc.).
"""
import struct, zlib, sys, os

blob_path = sys.argv[1] if len(sys.argv) > 1 else "[0]"
outdir = sys.argv[2] if len(sys.argv) > 2 else "payload"
blob = open(blob_path, "rb").read()

flags, sig = struct.unpack("<II", blob[0:8])
magic = blob[8:20]
len_header, len_data = struct.unpack("<II", blob[20:28])
print(f"flags={flags} sig={sig:#x} magic={magic!r} header={len_header} data={len_data} size={len(blob)}")
os.makedirs(outdir, exist_ok=True)

pos, idx = 28, 0
while pos + 4 <= len(blob):
    (raw,) = struct.unpack("<I", blob[pos:pos+4])
    comp = bool(raw & 0x80000000)
    size = raw & 0x7fffffff
    if size == 0 or pos + 4 + size > len(blob):
        print(f"stop at pos={pos} raw={raw:#x} size={size}")
        break
    chunk = blob[pos+4:pos+4+size]
    out = zlib.decompress(chunk, -15) if comp else chunk
    open(os.path.join(outdir, f"block_{idx:03d}.bin"), "wb").write(out)
    print(f"block {idx}: comp={comp} csize={size} -> {len(out)} bytes")
    pos += 4 + size
    idx += 1
    if idx > 5000:
        print("safety stop"); break
print(f"done: {idx} blocks -> {outdir}/")
