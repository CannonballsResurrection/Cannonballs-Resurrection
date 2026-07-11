#!/usr/bin/env python3
"""Build the Cannonballs! Resurrection cinematic trailer.

Pipeline:
  1. Record each gameplay clip with the game's own `--record` mode (real in-engine
     footage, 8-player match with a live HUD, scripted cinematic camera) -> PNG frames.
  2. Encode each PNG sequence to an intermediate mp4 (cached).
  3. Render the three text cards as full-screen black PNGs (Pillow), each its own
     faded segment.
  4. Concat the ordered segments [card1 | card2 | Tropicali opening | weapon showcase |
     card3 | island cuts | victory finale], lay the WAV music over the whole thing
     (fade in/out), encode H.264 + AAC at the game's native 4:3 aspect.

Quick-cut island durations fill the exact music length so the trailer lands the last
frame of the winner-orbit finale on the final note.

Everything lives under repo `trailer/` (gitignored). Usage:
  python3 tools/build_trailer.py            # native-4:3 final (1440x1080)
  python3 tools/build_trailer.py --draft    # faster 960x720 draft
  python3 tools/build_trailer.py --only-assemble   # skip recording, re-cut/cards only
"""
import argparse, json, os, subprocess, sys, textwrap, shutil
from PIL import Image, ImageDraw, ImageFont

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def run(cmd):
    print("  $", " ".join(str(c) for c in cmd[:7]), "..." if len(cmd) > 7 else "")
    subprocess.run(cmd, check=True)


def probe_duration(path):
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "default=noprint_wrappers=1:nokey=1", path],
        check=True, capture_output=True, text=True).stdout.strip()
    return float(out)


def render_card_png(card, W, H, font_path, out_png):
    """White serif text, centered on black, with a soft stroke."""
    img = Image.new("RGB", (W, H), (0, 0, 0))
    draw = ImageDraw.Draw(img)
    size = round(H * float(card["height_scale"]))
    font = ImageFont.truetype(font_path, size)
    text = card["text"]
    wrap = int(card.get("wrap", 0))
    if wrap:
        text = "\n".join(textwrap.wrap(text, wrap))
    draw.multiline_text((W / 2, H / 2), text, font=font, fill=(255, 255, 255),
                        anchor="mm", align="center", spacing=size * 0.35,
                        stroke_width=max(2, size // 22), stroke_fill=(0, 0, 0))
    img.save(out_png)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--storyboard", default=os.path.join(REPO, "tools", "trailer_storyboard.json"))
    ap.add_argument("--draft", action="store_true", help="faster 960x720 draft")
    ap.add_argument("--only-assemble", action="store_true", help="skip recording; re-encode + assemble")
    ap.add_argument("--skip-record", action="store_true", help="reuse existing PNG frames if present")
    ap.add_argument("--release", action="store_true", help="use the release build of the game binary")
    ap.add_argument("--out", default=None)
    args = ap.parse_args()

    sb = json.load(open(args.storyboard))
    fps = int(sb["fps"])
    res = sb["draft_resolution"] if args.draft else sb["resolution"]
    W, H = (int(x) for x in res.lower().split("x"))
    # Music/font paths may use $ENV vars or ~ so nothing personal is hard-coded in the
    # tracked storyboard. CB_TRAILER_MUSIC overrides the storyboard's music entirely.
    def _resolve(p):
        return os.path.expanduser(os.path.expandvars(p))
    music = _resolve(os.environ.get("CB_TRAILER_MUSIC", sb["music"]))
    font = _resolve(sb["font"])
    warmup = float(sb.get("warmup", 3.0))
    if not os.path.exists(music):
        sys.exit(f"music not found: {music}")
    D = probe_duration(music)

    build = "release" if args.release else "debug"
    binpath = os.path.join(REPO, "macos", ".build", build, "Cannonballs")
    if not args.only_assemble and not os.path.exists(binpath):
        sys.exit(f"game binary not found: {binpath}  (cd macos && swift build{' -c release' if args.release else ''})")

    base = os.path.join(REPO, "trailer")
    frames_dir, clips_dir, tmp_dir = (os.path.join(base, d) for d in ("frames", "clips", "tmp"))
    for d in (frames_dir, clips_dir, tmp_dir):
        os.makedirs(d, exist_ok=True)

    cards = {c["id"]: c for c in sb["cards"]}
    card_dur = {cid: float(c["end"]) - float(c["start"]) for cid, c in cards.items()}
    intro = sorted((c for c in sb["cards"] if c.get("slot") == "intro"), key=lambda c: c["start"])
    after_showcase = [c for c in sb["cards"] if c.get("slot") == "after_showcase"]

    quick = sb["quick_islands"]
    fixed = (sum(card_dur.values()) + float(sb["opening"]["seconds"])
             + float(sb["showcase"]["seconds"]) + float(sb["finale"]["seconds"]))
    quick_secs = (D - fixed) / len(quick)
    if quick_secs <= 0.4:
        sys.exit(f"music too short ({D:.1f}s); quick cuts would be {quick_secs:.2f}s")

    # ---- ordered segment list -------------------------------------------------
    # kind: ('card', cardspec) or ('clip', label, map, seconds, variety, finale, showcase)
    segs = []
    for c in intro:
        segs.append(("card", c))
    segs.append(("clip", "opening", sb["opening"]["map"], float(sb["opening"]["seconds"]),
                 int(sb["opening"].get("variety", 0)), False, False))
    segs.append(("clip", "showcase", sb["showcase"]["map"], float(sb["showcase"]["seconds"]), 0, False, True))
    for c in after_showcase:
        segs.append(("card", c))
    for i, isl in enumerate(quick):
        segs.append(("clip", f"isle{i:02d}", isl, quick_secs, 7 + i * 3, False, False))
    segs.append(("clip", "finale", sb["finale"]["map"], float(sb["finale"]["seconds"]), 0, True, False))

    print(f"music={D:.2f}s  res={W}x{H}@{fps} (native 4:3)  quick-cut={quick_secs:.2f}s x{len(quick)}")
    t = 0.0
    for seg in segs:
        if seg[0] == "card":
            dur = card_dur[seg[1]["id"]]
            print(f"  {t:6.2f}-{t+dur:6.2f}  CARD: {seg[1]['text'][:40]}")
            t += dur
        else:
            _, label, mp, secs, var, fin, sc = seg
            tag = "  [FINALE]" if fin else ("  [SHOWCASE]" if sc else "")
            print(f"  {t:6.2f}-{t+secs:6.2f}  {mp}{tag}")
            t += secs

    # ---- render each segment to an mp4 ---------------------------------------
    body = []
    for seg in segs:
        if seg[0] == "card":
            card = seg[1]
            png = os.path.join(tmp_dir, f"{card['id']}.png")
            out = os.path.join(clips_dir, f"{card['id']}.mp4")
            dur = card_dur[card["id"]]
            render_card_png(card, W, H, font, png)
            run(["ffmpeg", "-y", "-loop", "1", "-t", f"{dur}", "-i", png,
                 "-vf", f"fade=in:st=0:d=0.5,fade=out:st={dur-0.5}:d=0.5,format=yuv420p",
                 "-r", str(fps), "-c:v", "libx264", "-crf", "16", out])
            body.append(out)
            continue
        _, label, mp, secs, var, fin, sc = seg
        fdir = os.path.join(frames_dir, label)
        out = os.path.join(clips_dir, f"{label}.mp4")
        frame0 = os.path.join(fdir, "frame_00000.png")
        if not args.only_assemble and not (args.skip_record and os.path.exists(frame0)):
            if os.path.isdir(fdir):
                shutil.rmtree(fdir)
            cmd = [binpath, "--record", mp, fdir, "--seconds", f"{secs:.4f}",
                   "--fps", str(fps), "--size", f"{W}x{H}", "--warmup", f"{warmup}", "--variety", str(var)]
            if fin:
                cmd.append("--finale")
            if sc:
                cmd.append("--showcase")
            run(cmd)
        if not os.path.exists(frame0):
            sys.exit(f"no frames for {label} ({fdir}); run without --only-assemble")
        run(["ffmpeg", "-y", "-framerate", str(fps), "-i", os.path.join(fdir, "frame_%05d.png"),
             "-vf", f"scale={W}:{H}:force_original_aspect_ratio=disable",
             "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "16", "-r", str(fps), out])
        body.append(out)

    # ---- final: concat all segments + music ----------------------------------
    concat_in = "".join(f"[{i}:v]" for i in range(len(body)))
    fade_out_st = max(0.0, D - 2.0)
    fc = (f"{concat_in}concat=n={len(body)}:v=1:a=0[v];"
          f"[{len(body)}:a]afade=t=in:st=0:d=1.5,afade=t=out:st={fade_out_st}:d=2[a]")

    out = args.out or os.path.join(REPO, sb["out"])
    if args.draft:
        root, ext = os.path.splitext(out)
        out = root + "-draft" + ext
    os.makedirs(os.path.dirname(out), exist_ok=True)

    cmd = ["ffmpeg", "-y"]
    for v in body:
        cmd += ["-i", v]
    cmd += ["-i", music, "-filter_complex", fc, "-map", "[v]", "-map", "[a]",
            "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "22" if args.draft else "18",
            "-c:a", "aac", "-b:a", "192k", "-r", str(fps), "-shortest", out]
    run(cmd)
    print(f"\nTRAILER: {out}")


if __name__ == "__main__":
    main()
