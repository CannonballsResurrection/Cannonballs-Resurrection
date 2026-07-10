#!/usr/bin/env python3
"""Synthesize the Cannonballs SFX set as 16-bit mono WAVs.

The original 26 .wwv sounds use a proprietary codec and are unrecoverable,
so every effect here is built from noise bursts, sine sweeps and envelopes
to approximate the events in SPEC.md section 10.

Usage: python3 tools/gen_sfx.py [outdir]
Default outdir: shared/Resources/SFX
"""
import math
import os
import random
import struct
import sys
import wave

SR = 22050
random.seed(1802)


def write_wav(path, samples):
    samples = [max(-1.0, min(1.0, s)) for s in samples]
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(s * 32000)) for s in samples))
    print("wrote", path)


def env_exp(n, decay):
    return [math.exp(-decay * i / SR) for i in range(n)]


def noise(n):
    return [random.uniform(-1, 1) for _ in range(n)]


def lowpass(x, alpha):
    out, y = [], 0.0
    for s in x:
        y += alpha * (s - y)
        out.append(y)
    return out


def highpass(x, alpha):
    lp = lowpass(x, alpha)
    return [a - b for a, b in zip(x, lp)]


def sine_sweep(n, f0, f1, amp=1.0):
    out, phase = [], 0.0
    for i in range(n):
        f = f0 + (f1 - f0) * i / n
        phase += 2 * math.pi * f / SR
        out.append(amp * math.sin(phase))
    return out


def mix(*tracks):
    n = max(len(t) for t in tracks)
    out = [0.0] * n
    for t in tracks:
        for i, s in enumerate(t):
            out[i] += s
    return out


def gain(x, g):
    return [s * g for s in x]


def boom(dur=0.9, thump_f0=95, thump_f1=32, noise_amt=0.8, decay=6.0):
    n = int(SR * dur)
    thump = sine_sweep(n, thump_f0, thump_f1)
    nz = lowpass(noise(n), 0.25)
    e = env_exp(n, decay)
    return [(0.9 * t + noise_amt * z) * ev for t, z, ev in zip(thump, nz, e)]


def main(outdir):
    os.makedirs(outdir, exist_ok=True)
    put = lambda name, s: write_wav(os.path.join(outdir, name + ".wav"), s)

    # cannon fire: low thump + sharp noise crack
    crack = gain(highpass(noise(int(SR * 0.18)), 0.4), 0.9)
    crack = [c * e for c, e in zip(crack, env_exp(len(crack), 26))]
    put("cannon_fire", gain(mix(boom(0.7, 110, 40, 0.7, 8.0), crack), 0.9))

    # explosions, escalating sizes
    put("explosion1", gain(boom(0.9, 90, 30, 0.9, 6.0), 0.95))
    put("explosion2", gain(boom(1.2, 80, 26, 1.0, 4.6), 0.95))
    put("explosion3", gain(boom(1.7, 70, 20, 1.1, 3.4), 0.95))

    # splash: band-passed noise swoosh with slow attack
    n = int(SR * 1.0)
    sp = highpass(lowpass(noise(n), 0.5), 0.06)
    out = []
    for i, s in enumerate(sp):
        t = i / n
        env = min(1.0, t * 12) * math.exp(-4.5 * t)
        out.append(s * env)
    put("splash", gain(out, 1.2))

    # cash: rising coin arpeggio of short sine dings
    notes = [1320, 1660, 1980, 2640]
    track = []
    for i, f in enumerate(notes):
        m = int(SR * 0.09)
        ding = [math.sin(2 * math.pi * f * j / SR) * math.exp(-14 * j / SR) for j in range(m)]
        track.extend(ding)
    put("cash", gain(track, 0.75))

    # UI tick / hover
    n = int(SR * 0.05)
    put("click", [math.sin(2 * math.pi * 950 * i / SR) * math.exp(-60 * i / SR) for i in range(n)])
    put("hover", [math.sin(2 * math.pi * 640 * i / SR) * math.exp(-70 * i / SR) * 0.6 for i in range(n)])

    # aiming loops
    n = int(SR * 0.6)
    hum = [0.35 * math.sin(2 * math.pi * 55 * i / SR) + 0.2 * math.sin(2 * math.pi * 110 * i / SR + 0.7)
           for i in range(n)]
    ripple = [0.1 * math.sin(2 * math.pi * 8 * i / SR) for i in range(n)]
    put("turn_loop", [(h * (1 + r)) for h, r in zip(hum, ripple)])
    n = int(SR * 0.5)
    put("tilt", [0.3 * math.sin(2 * math.pi * (420 + 40 * math.sin(2 * math.pi * 6 * i / SR)) * i / SR)
                 for i in range(n)])

    # timer tick + buzzer
    n = int(SR * 0.07)
    put("timer_tick", [math.sin(2 * math.pi * 1250 * i / SR) * math.exp(-45 * i / SR) for i in range(n)])
    n = int(SR * 0.7)
    buzz = [0.5 * (1 if math.sin(2 * math.pi * 160 * i / SR) > 0 else -1) * math.exp(-3 * i / SR) for i in range(n)]
    put("time_up", lowpass(buzz, 0.35))

    # drumroll: rapid filtered noise hits
    track = []
    for hit in range(28):
        m = int(SR * 0.045)
        h = [random.uniform(-1, 1) * math.exp(-38 * j / SR) for j in range(m)]
        track.extend(lowpass(h, 0.5))
    put("drumroll", gain(track, 0.8))

    # quake: long low rumble
    n = int(SR * 1.6)
    rum = lowpass(noise(n), 0.06)
    put("quake", [3.2 * r * math.exp(-2.2 * i / SR) for i, r in enumerate(rum)])

    # teleport: sparkle sweep up
    n = int(SR * 0.7)
    sw = sine_sweep(n, 300, 2400, 0.6)
    shimmer = [0.25 * math.sin(2 * math.pi * 3200 * i / SR) * random.uniform(0.4, 1) for i in range(n)]
    e = [min(1.0, i / (0.05 * SR)) * math.exp(-3.2 * i / SR) for i in range(n)]
    put("teleport", [(a + b) * ev for a, b, ev in zip(sw, shimmer, e)])

    # bounce: woody thock
    n = int(SR * 0.16)
    th = [math.sin(2 * math.pi * 210 * i / SR) * math.exp(-30 * i / SR) for i in range(n)]
    kn = [0.4 * random.uniform(-1, 1) * math.exp(-90 * i / SR) for i in range(n)]
    put("bounce", mix(th, kn))


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "shared", "Resources", "SFX")
    main(out)
