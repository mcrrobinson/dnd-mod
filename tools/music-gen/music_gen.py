#!/usr/bin/env python3
"""Synthesizes the mod's class-special stings and event music loops.

Pure Python standard library (no numpy); needs ffmpeg with libvorbis on the PATH.

    python3 tools/music-gen/music_gen.py            # writes every sound
    python3 tools/music-gen/music_gen.py fighter    # only the named ones

Output goes to src/main/resources/assets/dndclasses/sounds/ as mono Ogg Vorbis.
Everything is seeded, so re-running produces the same audio.
"""
import array
import math
import os
import random
import subprocess
import sys
import tempfile
import wave

SR = 32000
TAU = 2 * math.pi
OUT_DIR = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                        "..", "..", "src", "main", "resources", "assets", "dndclasses", "sounds"))


# ---------------------------------------------------------------- basics

def ns(sec):
    return int(sec * SR)


def hz(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def buf(sec):
    return [0.0] * ns(sec)


def mix(dst, src, at=0.0, gain=1.0):
    off = ns(at)
    tail = min(len(src), ns(0.03))  # fade the last 30 ms so cut-off sources don't click
    for i in range(min(len(src), len(dst) - off)):
        left = len(src) - i
        dst[off + i] += src[i] * gain * (left / tail if left < tail else 1.0)
    return dst


def scale(x, g):
    return [v * g for v in x]


def softclip(x, drive=1.0):
    t = math.tanh(drive)
    return [math.tanh(v * drive) / t for v in x]


def fade_out(x, sec):
    n = min(len(x), ns(sec))
    for i in range(n):
        x[len(x) - n + i] *= 1 - i / n
    return x


def fade_in(x, sec):
    n = min(len(x), ns(sec))
    for i in range(n):
        x[i] *= i / n
    return x


def env_ar(n, attack, decay, curve=1.0):
    """Linear attack then exponential decay (time constant `decay` seconds)."""
    a = max(1, ns(attack))
    out = [0.0] * n
    for i in range(n):
        if i < a:
            out[i] = (i / a) ** curve
        else:
            out[i] = math.exp(-(i - a) / SR / decay)
    return out


def env_points(n, pts):
    """Piecewise-linear envelope through (time, level) points."""
    out = [0.0] * n
    pts = [(ns(t), v) for t, v in pts]
    j = 0
    for i in range(n):
        while j < len(pts) - 2 and i >= pts[j + 1][0]:
            j += 1
        (t0, v0), (t1, v1) = pts[j], pts[j + 1]
        if i <= t0:
            out[i] = v0
        elif i >= t1:
            out[i] = v1
        else:
            out[i] = v0 + (v1 - v0) * (i - t0) / (t1 - t0)
    return out


def apply(x, env):
    return [a * b for a, b in zip(x, env)]


# ---------------------------------------------------------------- sources

def noise(sec, rng):
    return [rng.uniform(-1, 1) for _ in range(ns(sec))]


def partials(sec, freq, spec, attack=0.003, glide=None, vibrato=(0, 0)):
    """Sum of sines. spec: list of (ratio, amp, decay seconds). glide: (start ratio, time const)."""
    n = ns(sec)
    out = [0.0] * n
    a = max(1, ns(attack))
    phase = 0.0
    vrate, vdepth = vibrato
    for i in range(n):
        t = i / SR
        f = freq
        if glide:
            f *= 1 + (glide[0] - 1) * math.exp(-t / glide[1])
        if vdepth:
            f *= 1 + vdepth * math.sin(TAU * vrate * t)
        phase += TAU * f / SR
        s = 0.0
        for r, amp, dec in spec:
            s += amp * math.sin(phase * r) * math.exp(-t / dec)
        out[i] = s * min(1.0, i / a)
    return out


def saw(sec, freq, detune=0.0, voices=1, vibrato=(0, 0), rng=None):
    """PolyBLEP sawtooth, optionally several detuned voices."""
    n = ns(sec)
    out = [0.0] * n
    rng = rng or random.Random(1)
    for v in range(voices):
        d = 1 + detune * ((v - (voices - 1) / 2) / max(1, voices - 1) * 2 if voices > 1 else 0)
        p = rng.random()
        vrate, vdepth = vibrato
        vph = rng.random() * TAU
        for i in range(n):
            f = freq * d * (1 + vdepth * math.sin(TAU * vrate * i / SR + vph))
            dt = f / SR
            p += dt
            if p >= 1:
                p -= 1
            s = 2 * p - 1
            if p < dt:
                x = p / dt
                s -= x + x - x * x - 1
            elif p > 1 - dt:
                x = (p - 1) / dt
                s -= x * x + x + x + 1
            out[i] += s / voices
    return out


def pulse(sec, freq, width=0.3):
    a = saw(sec, freq)
    b = saw(sec, freq)
    # Two saws offset in phase make a pulse; cheap approximation via shifted copy
    shift = int(width * SR / freq)
    return [a[i] - (b[i - shift] if i >= shift else 0.0) for i in range(len(a))]


def pluck(sec, freq, rng, decay=0.996, bright=0.5):
    """Karplus-Strong plucked string."""
    period = max(2, int(SR / freq))
    line = [rng.uniform(-1, 1) for _ in range(period)]
    # Soften the excitation for a darker pluck
    for _ in range(int((1 - bright) * 4)):
        line = [(line[i] + line[i - 1]) / 2 for i in range(period)]
    out = [0.0] * ns(sec)
    idx = 0
    for i in range(len(out)):
        cur = line[idx]
        nxt = line[(idx + 1) % period]
        line[idx] = decay * 0.5 * (cur + nxt)
        out[i] = cur
        idx = (idx + 1) % period
    return out


def drum(sec, f_start, f_end, sweep, decay, click=0.3, rng=None):
    """Tom/kick: sine with a downward pitch sweep plus a noise click."""
    rng = rng or random.Random(2)
    n = ns(sec)
    out = [0.0] * n
    phase = 0.0
    for i in range(n):
        t = i / SR
        f = f_end + (f_start - f_end) * math.exp(-t / sweep)
        phase += TAU * f / SR
        out[i] = math.sin(phase) * math.exp(-t / decay)
        if t < 0.02:
            out[i] += click * rng.uniform(-1, 1) * (1 - t / 0.02)
    return out


def bell(sec, freq, decay=1.5, bright=1.0, ratios=(1, 2.0, 2.76, 4.07, 5.4, 6.8)):
    spec = [(r, (1 / (k + 1)) ** (1.2 / bright), decay / (1 + 0.6 * k)) for k, r in enumerate(ratios)]
    return partials(sec, freq, spec, attack=0.001)


# ---------------------------------------------------------------- filters

def svf(x, cutoff, q=0.707, mode="low"):
    """TPT state-variable filter; cutoff is a number or a per-sample list."""
    k = 1 / q
    ic1 = ic2 = 0.0
    out = [0.0] * len(x)
    const = not isinstance(cutoff, list)
    if const:
        g = math.tan(math.pi * min(cutoff, SR * 0.45) / SR)
        a1 = 1 / (1 + g * (g + k)); a2 = g * a1; a3 = g * a2
    for i, v0 in enumerate(x):
        if not const:
            g = math.tan(math.pi * min(max(cutoff[i], 10), SR * 0.45) / SR)
            a1 = 1 / (1 + g * (g + k)); a2 = g * a1; a3 = g * a2
        v3 = v0 - ic2
        v1 = a1 * ic1 + a2 * v3
        v2 = ic2 + a2 * ic1 + a3 * v3
        ic1 = 2 * v1 - ic1
        ic2 = 2 * v2 - ic2
        if mode == "low":
            out[i] = v2
        elif mode == "band":
            out[i] = v1 * k
        else:
            out[i] = v0 - k * v1 - v2
    return out


def formant(x, freqs=(800, 1150, 2900), gains=(1.0, 0.6, 0.25), q=8):
    out = [0.0] * len(x)
    for f, g in zip(freqs, gains):
        mix(out, svf(x, f, q, "band"), 0, g)
    return out


def reverb(x, wet=0.3, room=0.84, damp=0.3, tail=0.0):
    """Small Schroeder/Freeverb-style mono reverb."""
    x = x + [0.0] * ns(tail)
    combs = [int(d * SR / 44100) for d in (1116, 1188, 1277, 1356, 1422, 1491)]
    aps = [int(d * SR / 44100) for d in (556, 441, 341)]
    acc = [0.0] * len(x)
    for d in combs:
        line = [0.0] * d
        idx = 0
        store = 0.0
        for i, v in enumerate(x):
            o = line[idx]
            store = o * (1 - damp) + store * damp
            line[idx] = v + store * room
            idx = (idx + 1) % d
            acc[i] += o
    acc = [v / len(combs) for v in acc]
    for d in aps:
        line = [0.0] * d
        idx = 0
        for i, v in enumerate(acc):
            b = line[idx]
            line[idx] = v + b * 0.5
            acc[i] = b - v
            idx = (idx + 1) % d
    return [d + w * wet for d, w in zip(x, acc)]


# ---------------------------------------------------------------- output

def normalize(x, peak_db=-1.0, rms_db=None):
    peak = max(1e-9, max(abs(v) for v in x))
    g = 10 ** (peak_db / 20) / peak
    if rms_db is not None:
        rms = math.sqrt(sum(v * v for v in x) / len(x))
        g = min(g, 10 ** (rms_db / 20) / max(rms, 1e-9))
    return [v * g for v in x]


def make_loop(x, loop_sec, xfade_sec):
    """x is rendered loop_sec + xfade_sec long; fold the overhang onto the start."""
    L = ns(loop_sec)
    X = ns(xfade_sec)
    out = x[:L]
    for i in range(X):
        a = math.sin(0.5 * math.pi * i / X)
        b = math.cos(0.5 * math.pi * i / X)
        out[i] = x[i] * a + x[L + i] * b
    return out


def write_ogg(name, x, quality=3):
    os.makedirs(OUT_DIR, exist_ok=True)
    pcm = array.array("h", (int(max(-1.0, min(1.0, v)) * 32767) for v in x))
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = os.path.join(tmp, name + ".wav")
        with wave.open(wav_path, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        out = os.path.join(OUT_DIR, name + ".ogg")
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", wav_path,
                        "-c:a", "libvorbis", "-q:a", str(quality), out], check=True)
    print(f"{name}.ogg  {len(x) / SR:.1f}s  {os.path.getsize(out) // 1024} KB")


def finish_sting(x, sec, wet=0.3, room=0.84):
    x = reverb(x, wet=wet, room=room)
    x = x[:ns(sec)] + [0.0] * max(0, ns(sec) - len(x))
    fade_out(x, 0.6)
    return normalize(x, -1.0, -15.0)


# ---------------------------------------------------------------- stings

def brass(sec, freq, rng, attack=0.03, bright=2500, base=500, voices=3, release=0.25, vib=(5.5, 0.004)):
    """Filtered detuned saws with a filter envelope; reads as a brass section stab."""
    x = saw(sec, freq, detune=0.006, voices=voices, vibrato=vib, rng=rng)
    n = len(x)
    cut = [base + bright * math.exp(-i / SR / 0.25) * min(1, i / max(1, ns(attack)))
           for i in range(n)]
    x = svf(x, cut, 0.9)
    env = env_points(n, [(0, 0), (attack, 1), (attack + 0.15, 0.8), (sec - release, 0.7), (sec, 0)])
    return apply(x, env)


def sting_fighter(rng):
    # "da-DAAA": short brass pickup then a big stab, with an anvil-like metal clang on the downbeat
    out = buf(3.5)
    chord = [50, 57, 62, 66]  # D major, open voicing
    for m in chord:
        mix(out, brass(0.18, hz(m), rng, attack=0.01, bright=2000), 0.0, 0.18)
    for m in chord + [38]:
        mix(out, brass(1.6, hz(m), rng, attack=0.02, bright=3500, base=700), 0.24, 0.25)
    clang = bell(2.0, 330, decay=0.8, ratios=(1, 2.32, 3.86, 5.1, 6.9, 8.6))
    mix(out, clang, 0.24, 0.5)
    hit = svf(noise(0.08, rng), 3000, 0.7, "high")
    mix(out, apply(hit, env_ar(len(hit), 0.001, 0.02)), 0.24, 0.6)
    mix(out, drum(1.0, 120, 50, 0.04, 0.3, rng=rng), 0.24, 0.8)
    return finish_sting(out, 3.2)


def sting_paladin(rng):
    # Noble horn call G-C-E-G rising, with a fifth underneath and a timpani roll
    out = buf(4.0)
    notes = [(55, 0.0, 0.3), (60, 0.3, 0.3), (64, 0.6, 0.3), (67, 0.9, 1.8)]
    for m, at, d in notes:
        mix(out, brass(d, hz(m), rng, attack=0.06, bright=1200, base=600, voices=2, release=0.35), at, 0.4)
        mix(out, brass(d, hz(m - 12), rng, attack=0.06, bright=800, base=400, voices=2, release=0.35), at, 0.22)
    for m in (48, 55, 64):
        mix(out, brass(1.8, hz(m), rng, attack=0.15, bright=600, base=500, voices=2, release=0.5), 0.9, 0.18)
    for k in range(10):
        mix(out, drum(0.6, 110, 65, 0.03, 0.25, click=0.1, rng=rng), 0.9 + k * 0.06, 0.12 + 0.05 * k)
    mix(out, drum(1.5, 110, 55, 0.05, 0.5, rng=rng), 1.5, 0.9)
    return finish_sting(out, 3.8, wet=0.4, room=0.88)


def sting_cleric(rng):
    # Bright chimes cascading up a C major chord over a soft choir "ah"
    out = buf(4.0)
    for k, m in enumerate([72, 76, 79, 84, 88]):
        mix(out, bell(3.0, hz(m), decay=1.4, bright=1.5, ratios=(1, 2.0, 3.0, 4.2, 5.4)), k * 0.09, 0.3)
    for m in (60, 64, 67, 72):
        v = formant(saw(3.5, hz(m), detune=0.004, voices=3, vibrato=(5, 0.003), rng=rng))
        mix(out, apply(v, env_points(len(v), [(0, 0), (0.4, 1), (2.5, 0.8), (3.5, 0)])), 0.05, 0.25)
    shimmer = svf(noise(2.0, rng), 9000, 0.7, "high")
    mix(out, apply(shimmer, env_ar(len(shimmer), 0.05, 0.4)), 0.0, 0.08)
    return finish_sting(out, 3.8, wet=0.45, room=0.9)


def sting_rogue(rng):
    # Quick pizzicato run down a minor scale, a whispered swoosh, and a low muted pluck to end
    out = buf(2.6)
    for k, m in enumerate([76, 74, 71, 67, 64]):
        mix(out, pluck(0.5, hz(m), rng, decay=0.985, bright=0.8), k * 0.07, 0.5)
    mix(out, pluck(1.0, hz(40), rng, decay=0.993, bright=0.4), 0.42, 0.9)
    mix(out, pluck(1.0, hz(52), rng, decay=0.99, bright=0.4), 0.42, 0.5)
    n = noise(0.6, rng)
    sweep = [6000 * math.exp(-i / SR / 0.25) + 1200 for i in range(len(n))]
    w = svf(n, sweep, 4, "band")
    mix(out, apply(w, env_points(len(w), [(0, 0), (0.25, 1), (0.6, 0)])), 0.0, 0.5)
    return finish_sting(out, 2.4, wet=0.25)


def marimba(sec, freq):
    return partials(sec, freq, [(1, 1, 0.5), (3.93, 0.35, 0.1), (9.2, 0.12, 0.03)], attack=0.001)


def sting_druid(rng):
    # Wooden marimba/log-drum pentatonic flourish, frame drum, and a breathy flute holding the top note
    out = buf(3.6)
    run = [57, 60, 62, 64, 67, 69, 72]
    for k, m in enumerate(run):
        mix(out, marimba(1.2, hz(m)), k * 0.08, 0.45)
    mix(out, marimba(2.0, hz(45)), 0.56, 0.7)
    mix(out, drum(1.0, 90, 70, 0.05, 0.35, click=0.5, rng=rng), 0.0, 0.6)
    mix(out, drum(1.0, 90, 70, 0.05, 0.35, click=0.5, rng=rng), 0.56, 0.7)
    fl = partials(2.6, hz(76), [(1, 1, 99), (2, 0.15, 99), (3, 0.05, 99)], attack=0.12, vibrato=(5, 0.006))
    br = svf(noise(2.6, rng), hz(76) * 2, 3, "band")
    fl = [a + 0.25 * b for a, b in zip(fl, br)]
    mix(out, apply(fl, env_points(len(fl), [(0, 0), (0.15, 1), (1.8, 0.7), (2.6, 0)])), 0.56, 0.3)
    return finish_sting(out, 3.4, wet=0.35)


def sting_barbarian(rng):
    # War drums: three tom hits then a huge hit under a distorted growl/roar
    out = buf(3.4)
    for k in range(3):
        mix(out, drum(0.6, 140, 70, 0.03, 0.18, click=0.5, rng=rng), k * 0.2, 0.6 + 0.1 * k)
    mix(out, drum(1.8, 160, 42, 0.06, 0.6, click=0.8, rng=rng), 0.6, 1.0)
    mix(out, drum(0.6, 140, 70, 0.03, 0.18, click=0.5, rng=rng), 0.6, 0.6)
    g = saw(2.0, 55, detune=0.03, voices=4, vibrato=(7, 0.02), rng=rng)
    g = [a + 0.6 * b for a, b in zip(g, noise(2.0, rng))]
    cut = [400 + 1400 * math.sin(math.pi * min(1, i / ns(1.2))) for i in range(len(g))]
    g = softclip(svf(g, cut, 2.5), 3.0)
    g = apply(g, env_points(len(g), [(0, 0), (0.15, 1), (1.0, 0.6), (2.0, 0)]))
    mix(out, g, 0.6, 0.45)
    return finish_sting(out, 3.2, wet=0.25, room=0.8)


def sting_monk(rng):
    # Gong strike with a slow shimmer, plus a beating singing-bowl tone
    out = buf(4.0)
    ratios = (1, 1.48, 1.95, 2.42, 2.98, 3.67, 4.21, 5.03, 6.12)
    spec = [(r, 1 / (1 + 0.5 * k), 3.5 / (1 + 0.25 * k)) for k, r in enumerate(ratios)]
    g = partials(4.0, 98, spec, attack=0.004, glide=(1.04, 0.6))
    mix(out, g, 0.0, 0.6)
    mallet = svf(noise(0.1, rng), 300, 0.7)
    mix(out, apply(mallet, env_ar(len(mallet), 0.001, 0.02)), 0.0, 1.5)
    bowl = partials(4.0, 587, [(1, 1, 3), (1.004, 1, 3), (2.71, 0.3, 1.2)], attack=0.3)
    mix(out, bowl, 0.1, 0.18)
    return finish_sting(out, 3.9, wet=0.4, room=0.88)


def sting_warlock(rng):
    # Dark low swell (tritone drone opening up), whispers, then a sub boom
    out = buf(3.8)
    swell = 1.8
    n = ns(swell)
    for m in (38, 44, 50):
        d = saw(swell, hz(m), detune=0.01, voices=3, rng=rng)
        cut = [150 + 1600 * (i / n) ** 3 for i in range(n)]
        d = svf(d, cut, 1.5)
        mix(out, apply(d, [(i / n) ** 2.5 for i in range(n)]), 0.0, 0.4)
    wh = svf(noise(swell, rng), 2500, 2, "band")
    mix(out, apply(wh, [(i / n) ** 3 for i in range(n)]), 0.0, 0.25)
    mix(out, drum(2.0, 90, 32, 0.08, 0.8, click=0.4, rng=rng), swell, 1.0)
    for m in (26, 32):
        t = svf(saw(1.8, hz(m), detune=0.01, voices=2, rng=rng), 300, 1.2)
        mix(out, apply(t, env_ar(len(t), 0.005, 0.6)), swell, 0.4)
    return finish_sting(out, 3.6, wet=0.35, room=0.88)


def sting_wizard(rng):
    # Rapid rising arcane arpeggio (whole-tone bells) into a shimmering chord and an upward whoosh
    out = buf(3.4)
    steps = [0, 2, 4, 6, 8, 10]
    k = 0
    for octave in (72, 84):
        for s in steps:
            mix(out, bell(1.0, hz(octave + s), decay=0.5, bright=2), k * 0.05, 0.22)
            k += 1
    for m in (79, 83, 86, 90):
        b = partials(2.5, hz(m), [(1, 1, 1.5), (1.003, 0.8, 1.5), (3, 0.1, 0.5)], attack=0.02)
        trem = [0.7 + 0.3 * math.sin(TAU * 7 * i / SR) for i in range(len(b))]
        mix(out, apply(b, trem), 0.6, 0.18)
    n = noise(1.0, rng)
    sweep = [500 * 12 ** (i / len(n)) for i in range(len(n))]
    w = svf(n, sweep, 3, "band")
    mix(out, apply(w, env_points(len(w), [(0, 0), (0.7, 1), (1.0, 0)])), 0.0, 0.3)
    return finish_sting(out, 3.2, wet=0.4, room=0.88)


def sting_ranger(rng):
    # Bow twang and arrow whoosh, then a two-note hunting whistle call
    out = buf(3.0)
    tw = pluck(0.8, 82, rng, decay=0.99, bright=0.9)
    mix(out, tw, 0.0, 0.8)
    n = noise(0.35, rng)
    w = svf(n, [3000 - 6000 * i / len(n) + 3500 for i in range(len(n))], 3, "band")
    mix(out, apply(w, env_points(len(w), [(0, 0), (0.05, 1), (0.35, 0)])), 0.05, 0.5)
    for m, at, d in ((76, 0.4, 0.25), (83, 0.65, 0.2), (81, 0.85, 1.4)):
        fl = partials(d, hz(m), [(1, 1, 99), (2, 0.1, 99)], attack=0.03, glide=(0.97, 0.03), vibrato=(6, 0.008))
        br = svf(noise(d, rng), hz(m), 4, "band")
        fl = [a + 0.2 * b for a, b in zip(fl, br)]
        mix(out, apply(fl, env_points(len(fl), [(0, 0), (0.03, 1), (d - 0.08, 0.8), (d, 0)])), at, 0.35)
    mix(out, drum(0.6, 120, 80, 0.03, 0.2, click=0.6, rng=rng), 0.4, 0.4)
    return finish_sting(out, 2.8, wet=0.35, room=0.86)


def sting_bard(rng):
    # Lute flourish up a G major arpeggio, a strummed chord and a tambourine shake
    out = buf(3.0)
    for k, m in enumerate([55, 59, 62, 67, 71, 74]):
        mix(out, pluck(1.0, hz(m), rng, decay=0.994, bright=0.6), k * 0.065, 0.35)
    for k, m in enumerate([43, 50, 55, 59, 62, 67]):
        mix(out, pluck(2.2, hz(m), rng, decay=0.997, bright=0.6), 0.45 + k * 0.018, 0.3)
    for k in range(4):
        j = svf(noise(0.15, rng), 7000, 3, "band")
        mix(out, apply(j, env_ar(len(j), 0.002, 0.04)), 0.45 + k * 0.12, 0.3 if k else 0.5)
    return finish_sting(out, 2.8, wet=0.3)


def sting_necromancer(rng):
    # Eerie low "oo" choir cluster, rattling bone clicks and a wavering glass tone
    out = buf(3.8)
    for m in (50, 51, 57):
        v = formant(saw(3.2, hz(m), detune=0.006, voices=3, vibrato=(4.5, 0.006), rng=rng),
                    freqs=(350, 700, 2400), gains=(1, 0.4, 0.1))
        mix(out, apply(v, env_points(len(v), [(0, 0), (0.6, 1), (2.4, 0.8), (3.2, 0)])), 0.0, 0.4)
    for _ in range(14):
        at = rng.uniform(0.05, 1.3)
        c = svf(noise(0.03, rng), rng.uniform(1500, 3500), 6, "band")
        mix(out, apply(c, env_ar(len(c), 0.0005, 0.006)), at, 0.5)
    gl = partials(3.0, hz(81), [(1, 1, 99), (1.414, 0.6, 99)], attack=0.8, vibrato=(3, 0.01))
    mix(out, apply(gl, env_points(len(gl), [(0, 0), (1.0, 1), (3.0, 0)])), 0.4, 0.12)
    return finish_sting(out, 3.6, wet=0.45, room=0.9)


def sting_artificer(rng):
    # Clockwork ticks speeding up, a ratchet whirr, a rising square-ish arpeggio and an anvil ping
    out = buf(3.0)
    t = 0.0
    gap = 0.16
    while t < 0.8:
        c = bell(0.08, 2400, decay=0.02, ratios=(1, 1.7, 2.9))
        mix(out, c, t, 0.4)
        t += gap
        gap = max(0.04, gap * 0.8)
    for k, m in enumerate([60, 64, 67, 72, 76]):
        p = svf(pulse(0.25, hz(m), 0.25), 2500, 0.8)
        mix(out, apply(p, env_ar(len(p), 0.005, 0.08)), 0.8 + k * 0.06, 0.25)
    ping = bell(2.2, 1046, decay=0.9, ratios=(1, 2.76, 5.4, 8.93))
    mix(out, ping, 1.15, 0.5)
    mix(out, drum(0.6, 200, 90, 0.02, 0.15, click=0.6, rng=rng), 1.15, 0.5)
    hum = svf(saw(1.8, hz(36), detune=0.01, voices=2, rng=rng), 400, 1)
    mix(out, apply(hum, env_points(len(hum), [(0, 0), (0.1, 1), (1.8, 0)])), 1.15, 0.3)
    return finish_sting(out, 2.9, wet=0.3)


def sting_bloodhunter(rng):
    # Double heartbeat thump, a sharp slash and a dissonant tremolo string stab
    out = buf(3.0)
    for at in (0.0, 0.2):
        mix(out, drum(0.4, 70, 45, 0.03, 0.12, click=0.1, rng=rng), at, 0.9 if at else 1.0)
    n = noise(0.25, rng)
    s = svf(n, [9000 * math.exp(-i / SR / 0.06) + 800 for i in range(len(n))], 3, "band")
    mix(out, apply(s, env_ar(len(s), 0.003, 0.06)), 0.45, 0.7)
    for m in (47, 48, 53, 59):
        st = svf(saw(2.0, hz(m), detune=0.008, voices=3, rng=rng), 2200, 1)
        trem = [0.6 + 0.4 * math.sin(TAU * 13 * i / SR) for i in range(len(st))]
        st = apply(apply(st, trem), env_points(len(st), [(0, 0), (0.02, 1), (0.3, 0.6), (2.0, 0)]))
        mix(out, st, 0.45, 0.25)
    return finish_sting(out, 2.8, wet=0.3)


def sting_alchemist(rng):
    # Bubbling chirps in a flask, a glass clink, then a "poof" and a glassy D major chord
    out = buf(3.2)
    for _ in range(16):
        at = rng.uniform(0.0, 0.9)
        f0 = rng.uniform(400, 1100)
        b = partials(0.06, f0, [(1, 1, 0.03)], attack=0.003, glide=(0.6, 0.02))
        mix(out, b, at, 0.35)
    mix(out, bell(1.5, 2349, decay=0.5, ratios=(1, 2.76, 5.4)), 0.95, 0.3)
    poof = svf(noise(0.5, rng), 1200, 0.8)
    mix(out, apply(poof, env_ar(len(poof), 0.01, 0.12)), 1.0, 0.6)
    for m in (74, 78, 81, 86):
        gl = partials(2.0, hz(m), [(1, 1, 1.2), (1.002, 0.8, 1.2), (2.0, 0.2, 0.5)], attack=0.01)
        mix(out, gl, 1.0, 0.2)
    return finish_sting(out, 3.0, wet=0.4, room=0.86)


# ---------------------------------------------------------------- bard instrument songs

def flute_note(sec, freq, rng, attack=0.04, vib=(5.5, 0.006)):
    fl = partials(sec, freq, [(1, 1, 99), (2, 0.18, 99), (3, 0.06, 99)], attack=attack, vibrato=vib)
    br = svf(noise(sec, rng), freq * 2, 3, "band")
    fl = [a + 0.22 * b for a, b in zip(fl, br)]
    return apply(fl, env_points(len(fl), [(0, 0), (attack, 1), (max(attack, sec - 0.06), 0.8), (sec, 0)]))


def song_lute(rng):
    # Gentle lute air in D major: a plucked melody over rolled chords (the regeneration song)
    out = buf(4.4)
    beat = 0.36
    chords = [[38, 45, 50, 54, 57], [43, 50, 55, 59, 62], [45, 52, 57, 61, 64], [38, 45, 50, 54, 62]]
    for c, chord in enumerate(chords):
        for k, m in enumerate(chord):
            mix(out, pluck(2.0, hz(m), rng, decay=0.996, bright=0.5), c * beat * 3 + k * 0.025, 0.22)
    melody = [(0, 66), (1, 69), (2, 74), (3, 71), (4, 67), (5, 71), (6, 73), (7, 69), (8, 76), (9, 74), (10, 73), (12, 74)]
    for step, m in melody:
        mix(out, pluck(1.2, hz(m), rng, decay=0.993, bright=0.75), 0.05 + step * beat * 0.75 * 1.2, 0.3)
    return finish_sting(out, 4.0, wet=0.35, room=0.86)


def song_drum(rng):
    # War drums: a driving tom rhythm building to a big double hit (the strength song)
    out = buf(4.4)
    beat = 0.22
    pattern = [1, 0, 0.6, 0, 1, 0, 0.6, 0.6, 1, 0, 0.6, 0, 1, 0.7, 0.8, 0.9]
    for k, v in enumerate(pattern):
        if v:
            mix(out, drum(0.7, 110, 62, 0.03, 0.22, click=0.4, rng=rng), k * beat, v)
        if k % 4 == 2:
            s = svf(noise(0.2, rng), 3000, 1.2, "band")
            mix(out, apply(s, env_ar(len(s), 0.001, 0.05)), k * beat, 0.35)
    end = len(pattern) * beat
    for at in (end, end + 0.18):
        mix(out, drum(1.2, 80, 38, 0.05, 0.5, click=0.6, rng=rng), at, 1.2)
    return finish_sting(out, 4.0, wet=0.25, room=0.8)


def song_flute(rng):
    # Quick, skipping flute jig in G (the speed song)
    out = buf(4.2)
    step = 0.16
    notes = [67, 71, 74, 79, 78, 76, 74, 71, 72, 76, 79, 76, 74, 71, 67, 71, 74, 76, 78, 79]
    for k, m in enumerate(notes):
        d = step * (2 if k == len(notes) - 1 else 1)
        mix(out, flute_note(d + 0.04, hz(m), rng, attack=0.02), 0.05 + k * step, 0.4)
    mix(out, flute_note(0.9, hz(79), rng, attack=0.03), 0.05 + len(notes) * step, 0.45)
    for k in range(0, len(notes) + 4, 2):
        mix(out, drum(0.3, 140, 90, 0.02, 0.08, click=0.3, rng=rng), 0.05 + k * step, 0.25)
    return finish_sting(out, 4.0, wet=0.35, room=0.86)


# ---------------------------------------------------------------- loops

def loop_low_health(rng):
    # 75 bpm heartbeat over a low A drone with a quiet tense minor-second string pair.
    beat = 0.8
    loop = beat * 24  # 19.2 s
    xf = 1.0
    sec = loop + xf
    out = buf(sec)
    t = 0.0
    while t < sec:
        mix(out, drum(0.5, 75, 42, 0.025, 0.14, click=0.05, rng=rng), t, 1.0)
        mix(out, drum(0.4, 70, 45, 0.02, 0.1, click=0.03, rng=rng), t + 0.24, 0.6)
        t += beat
    n = len(out)
    drone = svf(saw(sec, 55, detune=0.004, voices=3, rng=rng), 220, 1.0)
    swell = [0.7 + 0.3 * math.sin(TAU * i / SR / (loop / 2)) for i in range(n)]
    mix(out, apply(drone, swell), 0, 0.35)
    mix(out, partials(sec, 55, [(1, 1, 999), (0.5, 0.5, 999)]), 0, 0.25)
    for m in (76, 77):
        st = svf(saw(sec, hz(m), detune=0.004, voices=2, vibrato=(5, 0.003), rng=rng), 1800, 0.8)
        trem = [0.5 + 0.5 * math.sin(TAU * 6 * i / SR) for i in range(n)]
        lvl = [0.5 + 0.5 * math.sin(TAU * i / SR / loop - math.pi / 2) for i in range(n)]
        mix(out, apply(apply(st, trem), lvl), 0, 0.05)
    out = reverb(out, wet=0.2, room=0.8)[:n]
    out = make_loop(out, loop, xf)
    return normalize(out, -1.0, -17.0)


def loop_nether_fortress(rng):
    # Slow dark drone on D with a breathing filter, a minor-sixth that comes and goes,
    # low rumble, distant metallic booms and faint fire crackle.
    loop = 32.0
    xf = 3.0
    sec = loop + xf
    out = buf(sec)
    n = len(out)
    lfo = [0.5 - 0.5 * math.cos(TAU * i / SR / (loop / 2)) for i in range(n)]
    for m, g in ((26, 0.5), (38, 0.4), (45, 0.18)):
        d = saw(sec, hz(m), detune=0.005, voices=3, vibrato=(0.1, 0.002), rng=rng)
        d = svf(d, [180 + 500 * v for v in lfo], 1.2)
        mix(out, d, 0, g)
    sixth = svf(saw(sec, hz(46), detune=0.005, voices=2, rng=rng), 500, 1)
    lvl = [0.5 - 0.5 * math.cos(TAU * i / SR / loop) for i in range(n)]
    mix(out, apply(sixth, lvl), 0, 0.15)
    rumble = svf(svf(noise(sec, rng), 90, 0.7), 90, 0.7)
    mix(out, rumble, 0, 2.5)
    ratios = (1, 1.52, 2.17, 2.9, 3.6, 4.6)
    spec = [(r, 1 / (1 + 0.6 * k), 4.0 / (1 + 0.4 * k)) for k, r in enumerate(ratios)]
    for at, f in ((4.0, 49.0), (20.0, 43.6)):
        boom = partials(8.0, f, spec, attack=0.02)
        mix(out, svf(boom, 600, 0.7), at, 0.35)
    for _ in range(90):
        at = rng.uniform(0, sec - 0.05)
        c = svf(noise(0.02, rng), rng.uniform(1500, 4000), 3, "band")
        mix(out, apply(c, env_ar(len(c), 0.0005, 0.004)), at, rng.uniform(0.02, 0.07))
    out = reverb(out, wet=0.45, room=0.9, damp=0.5)[:n]
    out = make_loop(out, loop, xf)
    return normalize(out, -1.0, -19.0)


SOUNDS = {
    "sting_barbarian": sting_barbarian,
    "sting_bard": sting_bard,
    "sting_cleric": sting_cleric,
    "sting_druid": sting_druid,
    "sting_fighter": sting_fighter,
    "sting_monk": sting_monk,
    "sting_paladin": sting_paladin,
    "sting_ranger": sting_ranger,
    "sting_rogue": sting_rogue,
    "sting_necromancer": sting_necromancer,
    "sting_warlock": sting_warlock,
    "sting_wizard": sting_wizard,
    "sting_artificer": sting_artificer,
    "sting_bloodhunter": sting_bloodhunter,
    "sting_alchemist": sting_alchemist,
    "low_health": loop_low_health,
    "nether_fortress": loop_nether_fortress,
    "song_lute": song_lute,
    "song_drum": song_drum,
    "song_flute": song_flute,
}


def main(argv):
    wanted = argv or list(SOUNDS)
    for name in wanted:
        key = name if name in SOUNDS else "sting_" + name
        if key not in SOUNDS:
            sys.exit(f"unknown sound {name}; choose from {', '.join(SOUNDS)}")
        rng = random.Random(key)
        quality = 2 if key in ("low_health", "nether_fortress") else 3
        write_ogg(key, SOUNDS[key](rng), quality)


if __name__ == "__main__":
    main(sys.argv[1:])
