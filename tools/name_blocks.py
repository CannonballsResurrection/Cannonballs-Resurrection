#!/usr/bin/env python3
"""
name_blocks.py - map decompressed NSIS datablock blocks to their real filenames.

Target: WildTangent "Cannonballs" installer, built with NSIS v1.98 (~2002).

Header layout (empirically verified for this installer):
  - 0x000..0x104  : NSIS 1.x "common header" (string-table refs, sizes)
  - 0x104         : entries table, 979 records x 24 bytes (int32 which + 5 int32 parms)
  - 0x5ccc..end   : string table (null-terminated, chars >= 0xE0 are variable codes)

Relevant opcodes (NSIS 1.98):
  14 = EW_CREATEDIR   parms: (str_path, update_outpath_flag, 0, 0, 0)
       When parm2 == 1 this is SetOutPath: it changes the current output dir.
  21 = EW_EXTRACTFILE parms: (overwrite, str_name, datablock_offset, ftime_lo, ftime_hi)
       str_name is the bare filename; it lands in the current outpath.

Datablock offset base: the compressed header itself is the block at datablock
offset 0x1c (block 000). Extract entries store offsets relative to the first
real file block, which sits at datablock offset 0x2ea4. Hence:
    block_file_offset = entry_datablock_offset + 0x2ea4
This was verified: entry offsets 0, 453, 6133, 12592, 263876 map exactly to
blocks 001..005 (0x2ea4, 0x3069, 0x4699, 0x5fd4, 0x43568).

Variable codes seen in this string table (0xE0 + n = $n etc. in NSIS 1.x):
  0xE1 = $1  -> install dir (script copies $INSTDIR into $1)   => tree root
  0xE9 = $9  -> WildTangent dir (from registry "WTDirectory")  => WTDIR/
  0xFC      -> temp dir (nsisui installer UI scratch)          => _installer_ui/
  0xE2/0xE3 -> shell-folder pieces (Start Menu paths)          - not extracted
Anything else decodes to a $VARxx placeholder folder.

Output: JSON mapping on stdout section, and copies blocks into /tmp/cb/assets.
"""
import json
import os
import re
import struct
import sys

CB = "/tmp/cb"
HEADER = os.path.join(CB, "header.bin")
BLOCKS = os.path.join(CB, "blocks")
ASSETS = os.path.join(CB, "assets")

ENTRIES_OFF = 0x104
NUM_ENTRIES = 979
ENTRY_SIZE = 24
STRTAB_OFF = 0x5CCC
DATABLOCK_BASE = 0x2EA4  # entry offset 0 == this datablock offset

EW_CREATEDIR = 14
EW_EXTRACTFILE = 21

VAR_ROOTS = {
    0xE1: "",               # $1 = install dir -> root of asset tree
    0xE9: "WTDIR",          # $9 = WildTangent shared dir
    0xFC: "_installer_ui",  # temp dir for nsisui
}


def load_header():
    with open(HEADER, "rb") as f:
        return f.read()


def get_string(data, off):
    """Decode a string-table entry; map NSIS variable codes to names."""
    if off < 0:
        return ""
    start = STRTAB_OFF + off
    end = data.index(b"\0", start)
    raw = data[start:end]
    out = []
    for b in raw:
        if b >= 0xE0:
            if b in VAR_ROOTS:
                out.append(VAR_ROOTS[b])
            else:
                out.append("$VAR%02X" % b)
        else:
            out.append(chr(b))
    return "".join(out)


def parse_entries(data):
    ents = []
    for i in range(NUM_ENTRIES):
        rec = struct.unpack_from("<6i", data, ENTRIES_OFF + i * ENTRY_SIZE)
        ents.append(rec)
    return ents


def block_index():
    """offset-in-datablock -> block filename"""
    idx = {}
    for fn in sorted(os.listdir(BLOCKS)):
        m = re.match(r"(\d+)_([0-9a-f]+)\.bin$", fn)
        if m:
            idx[int(m.group(2), 16)] = fn
    return idx


def norm_path(p):
    """Backslash path with possible leading root -> clean relative path."""
    p = p.replace("\\", "/")
    p = re.sub(r"/+", "/", p).strip("/")
    return p


def build_mapping():
    data = load_header()
    ents = parse_entries(data)
    blocks = block_index()

    outpath = ""  # relative to asset root
    mapping = {}  # block filename -> relative target path
    extracts = 0
    dupes = 0
    missing = []

    for e in ents:
        op = e[0]
        if op == EW_CREATEDIR and e[2] == 1:
            outpath = norm_path(get_string(data, e[1]))
        elif op == EW_EXTRACTFILE:
            extracts += 1
            name = get_string(data, e[2])
            off = e[3] + DATABLOCK_BASE
            rel = norm_path(os.path.join(outpath, name))
            fn = blocks.get(off)
            if fn is None:
                missing.append((rel, off))
                continue
            if fn in mapping:
                dupes += 1
                # keep first mapping; record alternates
                mapping.setdefault("_alt_" + fn, []).append(rel)
            else:
                mapping[fn] = rel
    return data, mapping, extracts, dupes, missing, blocks


def guess_ext(path):
    """Guess an extension from magic bytes for unnamed blocks."""
    with open(path, "rb") as f:
        h = f.read(16)
    if h[:3] == b"\xff\xd8\xff":
        return ".jpg"
    if h[:4] == b"\x89PNG":
        return ".png"
    if h[:4] == b"GIF8":
        return ".gif"
    if h[:4] == b"RIFF":
        return ".wav"
    if h[:4] == b"WLD3":
        return ".wld3"
    if h[:2] == b"MZ":
        return ".exe"
    if h[:2] == b"PK":
        return ".zip"
    if h[:4] == b"(\x00\x00\x00":
        return ".dib"  # device-independent bitmap (icon image)
    if b"NullsoftInst" in h.replace(b"\xef\xbe\xad\xde", b""):
        return ".nsis-uninstdata"
    if h[4:8] == b"\xef\xbe\xad\xde":
        return ".nsis-uninstdata"
    return ".bin"


def apply_copies(mapping, blocks):
    """Copy blocks to /tmp/cb/assets/<path>; unknowns to _unknown/."""
    import shutil

    named = {k: v for k, v in mapping.items() if not k.startswith("_alt_")}
    copied = []
    for fn, rel in sorted(named.items()):
        dst = os.path.join(ASSETS, rel)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copy2(os.path.join(BLOCKS, fn), dst)
        copied.append(rel)
        # duplicate-content extracts (NSIS dedupes identical files)
        for alt in mapping.get("_alt_" + fn, []):
            dst2 = os.path.join(ASSETS, alt)
            os.makedirs(os.path.dirname(dst2), exist_ok=True)
            shutil.copy2(os.path.join(BLOCKS, fn), dst2)
            copied.append(alt)
    # unknowns (skip block 000 = the header itself)
    unk_dir = os.path.join(ASSETS, "_unknown")
    unknowns = []
    for off, fn in sorted(blocks.items()):
        if fn in named or fn.startswith("000_"):
            continue
        src = os.path.join(BLOCKS, fn)
        ext = guess_ext(src)
        os.makedirs(unk_dir, exist_ok=True)
        dst = os.path.join(unk_dir, fn[:-4] + ext)
        shutil.copy2(src, dst)
        unknowns.append("_unknown/" + fn[:-4] + ext)
    return copied, unknowns


def main():
    data, mapping, extracts, dupes, missing, blocks = build_mapping()
    named = {k: v for k, v in mapping.items() if not k.startswith("_alt_")}
    unnamed = [fn for off, fn in sorted(blocks.items()) if fn not in named]
    # block 000 is the header itself
    unnamed = [f for f in unnamed if not f.startswith("000_")]

    report = {
        "extract_entries": extracts,
        "named_blocks": len(named),
        "duplicate_extracts": dupes,
        "entries_with_no_block": missing,
        "unnamed_blocks": unnamed,
    }
    alts = {k[5:]: v for k, v in mapping.items() if k.startswith("_alt_")}
    with open(os.path.join(CB, "tools", "block_map.json"), "w") as f:
        json.dump({"mapping": named, "alternates": alts, "report": report}, f, indent=1)
    print(json.dumps(report, indent=1)[:4000])
    print("mapping written to tools/block_map.json")

    if "--apply" in sys.argv:
        copied, unknowns = apply_copies(mapping, blocks)
        print("copied %d files (+%d unknown) into %s" % (len(copied), len(unknowns), ASSETS))


if __name__ == "__main__":
    main()
