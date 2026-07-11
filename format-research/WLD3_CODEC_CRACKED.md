# Decoding WildTangent WLD3 assets (.wwv / .wsad) — solved

The Cannonballs game assets are wrapped in WildTangent's WLD3 container, headed
`WLD3.wav WildTangent 3D 300 Compressed and Patented`. This format had a
reputation for being undecodable outside WildTangent's dead native browser
plugin. It is fully decodable. All 29 audio files from Cannonballs were
recovered to standard WAV; the 3D models decode through the same pipeline to the
PWT geometry format.

## The pipeline (audio)

```
WLD3 container
  -> parse ASCII header + length-prefixed metadata fields (after ".START")
  -> derive a per-file key table by hashing those metadata fields
  -> rolling-XOR deobfuscation of the payload after ".BODY"
       decodeByte = inbyte XOR previous_inbyte XOR keytable[i % len(keytable)]
  -> the result is a Microsoft CAB (MSCF) containing one MSZIP/DEFLATE member
  -> extract it -> plain RIFF/WAVE (8/16-bit PCM, MS-ADPCM, or MP3-in-WAV)
```

Key facts that mattered:

- **There is no proprietary audio codec.** "Compressed" means DEFLATE (MSZIP
  inside a CAB). "Patented" refers to WildTangent's **3D vertex-quantization**
  patent (US6577769B1), which applies to the *model* format, not audio.
- The payload reads as ~8.0 bits/byte entropy with index-of-coincidence 1.000
  and no cross-file keystream reuse. That is exactly what XOR-obfuscated DEFLATE
  looks like, which is why naive "find the XOR key" attempts fail: the key is
  per-file (hashed from that file's own metadata) and the plaintext underneath
  is itself high-entropy compressed data.
- `XORBlockEncryptData.wtxt` is a **red herring** for assets: in the game's Java
  it only feeds player-name copy-protection scrambling, never the asset decode.

## Tooling

The open-source decoder **diamondman/WTExtractor** (`pywttools/wtextract.py`)
implements the container + key-derivation + CAB/MSZIP steps. Run it per file;
output is a standard media file you can transcode with `afconvert`/`ffmpeg`.

### One fix was needed: older v200 containers

WTExtractor only accepted the v300 header
(`WLD3.wav WildTangent 3D 300 ...`). Six Cannonballs sounds use the earlier
**v200** header (`WLD3 WildTangent 3D 200 ...`, with a leading space and no
`.wav` type tag). The payload encryption is identical; only the header line
differs. Accepting both — and defaulting the base type to `wav` when the v200
header omits it — recovers those files. This is a small, worth-upstreaming patch.

```python
# in WTDecoder.decode(), replacing the single-version header check:
rest = headline[1:]                       # drop leading '.' (v300) or ' ' (v200)
if rest.startswith("WildTangent"):
    self.base_type = "wav"                # v200 sound container has no type tag
    magic_msg = rest
else:
    self.base_type, magic_msg = rest.split(' ', 1)
if magic_msg not in ("WildTangent 3D 300 Compressed and Patented\r\n",
                     "WildTangent 3D 200 Compressed and Patented\r\n"):
    raise WTFormatException("File does not have correct Magic Line. Exiting.", -2)
```

## Results

All 29 `.wwv` files decoded (23 v300 directly + 6 v200 after the patch):

- 26 sound effects → WAV (8/16-bit PCM and MS-ADPCM, 8–44.1 kHz). Durations and
  content match their roles: `QUAKE` 5.0s rumble, `OCEAN` 11.8s loop, `OVER`
  0.024s UI blip, `EXPLOSION1-3`, `LAUNCH_CANNON`, `TELEPORT`, etc. Every file
  carries real signal (RMS 3.7–47%).
- 3 music tracks → MP3-in-WAV (the low-bitrate in-game versions).

Models (`.wsad`) decode through the same container to a CAB containing a `VFHH`
/ `ActorDescription` blob — the PWT geometry format that WTExtractor's
`pwtdecode.py` / `pwtviewer` target (this is where the patented vertex
quantization lives).

Decoded audio lives in `decoded_original_assets/` and is wired into the macOS
clone in place of the previously-synthesized SFX.
