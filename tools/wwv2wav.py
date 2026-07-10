#!/usr/bin/env python3
"""
wwv2wav.py - WildTangent Cannonballs .wwv (and .wjp/.wsad/... ) WLD3 container tool.

STATUS: The audio PAYLOAD cannot be converted to WAV. See the FINDINGS block below.
This tool parses the WLD3 container and dumps the raw compressed body, so that IF a
WildTangent WLD3/WTAudioClip decoder is ever obtained the last stage can be plugged in.

=========================== FINDINGS (reverse-engineering) ===========================
Container layout (every .wwv/.wjp/.wsad/.wsgo/.wsbm/.wsmo/.wtxt file):

  offset 0   : ASCII banner, e.g.
               "WLD3.wav WildTangent 3D 300 Compressed and Patented\r\n"
               "Converted by XtoWT: <ctime date>\n\r\n\r\n"
  then       : ".START\n"
  then       : small binary manifest, then the PLAINTEXT string
               "(c) Copyright 2001, WildTangent, Inc"  (stored uncompressed),
               followed by more binary manifest incl. literal tokens like 00000000,
               ffffffff, the ASCII word "free", ending with "\r\n"
  then       : ".BODY" + <uint32 LE tag>  (200 for wwv/wjp, 300 for 3D actor formats;
               this is a converter/format-class id, NOT a byte length)
  then       : the PAYLOAD.

The PAYLOAD is compressed with WildTangent's proprietary ("Patented") codec:
  - Shannon entropy of every payload measured at 7.77-8.00 bits/byte (near-random)
    => it is genuinely compressed/encrypted, NOT raw PCM and NOT simple repeating-XOR
       of a RIFF/WAVE stream (tested: no key of length 1-4 yields RIFF/WAVE/fmt/data).
  - No zlib / raw-deflate / gzip / bz2 / lzma / lzo stream present at any offset 0-11.
  - No RIFF/WAVE/OggS/ID3/BM/PNG/JPEG magic anywhere in the payload.
  - Large payloads (e.g. MUSIC/TITLE) contain internal FOURCC-ish sub-chunk tags
    ".RG" ".MX" ".FU" ".IA" => a chunked proprietary bitstream.
  - Decoding in the game is done entirely by the NATIVE class
    wildtangent.webdriver.WTAudioClip (see decompiled Media_Object_Sound.java:
    Main.MainRef.Wt.createAudioClip(".../sound.wwv")). There is NO Java-side decoder,
    so no algorithm to reimplement from the game code.

XORBlockEncryptData.wtxt is itself a WLD3 file with an equally high-entropy (compressed)
body, so it cannot be read without the same proprietary decompressor; it does not act as
a simple XOR key over the raw .wwv bytes.

CONCLUSION: Converting the original audio requires the WildTangent WebDriver / WT3D native
runtime (a defunct 2002-era Windows plugin). For a native macOS clone the practical path is
to REPLACE the 26 SFX + 3 music tracks with royalty-free equivalents (names/roles are known
from the directory layout, see SOUND_MAP below), not to recover the originals.
======================================================================================
"""
import sys, os, glob, struct, math, collections

SOUNDS = "/tmp/cb/assets/120302/MEDIA/SOUNDS"
MUSIC  = "/tmp/cb/assets/120302/MEDIA/MUSIC"
OUT_S  = "/tmp/cb/converted/sounds"
OUT_M  = "/tmp/cb/converted/music"

def parse_wld3(data):
    """Return dict with banner, date, body_tag, body(bytes) or raises."""
    if not data.startswith(b"WLD3"):
        raise ValueError("not a WLD3 file")
    s = data.find(b".START")
    b = data.find(b".BODY")
    if b < 0:
        raise ValueError("no .BODY marker")
    banner = data[:data.find(b"\r\n")].decode("latin1", "replace")
    date = ""
    m = data.find(b"XtoWT: ")
    if m >= 0:
        date = data[m+7:data.find(b"\n", m)].decode("latin1", "replace").strip()
    body_tag = struct.unpack_from("<I", data, b+5)[0]
    body = data[b+9:]
    return dict(banner=banner, date=date, body_tag=body_tag,
                start=s, body_off=b+9, body=body)

def entropy(bs):
    if not bs: return 0.0
    c = collections.Counter(bs)
    return -sum(n/len(bs)*math.log2(n/len(bs)) for n in c.values())

def convert(path, outdir, name):
    data = open(path, "rb").read()
    info = parse_wld3(data)
    os.makedirs(outdir, exist_ok=True)
    raw = os.path.join(outdir, name + ".wwvbody")
    open(raw, "wb").write(info["body"])
    ok = False  # cannot produce valid WAV: payload is proprietary-compressed
    print(f"{name:16s} tag={info['body_tag']:3d} body={len(info['body']):7d}B "
          f"entropy={entropy(info['body']):.2f}  [{info['date']}]  "
          f"-> {raw} (NO WAV: proprietary WT codec)")
    return ok

def main():
    n = 0
    print("== SOUNDS ==")
    for d in sorted(glob.glob(os.path.join(SOUNDS, "**/sound.wwv"), recursive=True)):
        name = os.path.relpath(os.path.dirname(d), SOUNDS).replace("/", "_")
        convert(d, OUT_S, name); n += 1
    print("== MUSIC ==")
    for d in sorted(glob.glob(os.path.join(MUSIC, "**/sound.wwv"), recursive=True)):
        name = os.path.relpath(os.path.dirname(d), MUSIC).replace("/", "_")
        convert(d, OUT_M, name); n += 1
    print(f"\nParsed {n} .wwv files. 0 converted to WAV.")
    print("Reason: WildTangent 'Compressed and Patented' payload; decode is native-only "
          "(WTAudioClip). Replace with royalty-free audio for the clone.")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        convert(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else ".",
                os.path.splitext(os.path.basename(sys.argv[1]))[0])
    else:
        main()
