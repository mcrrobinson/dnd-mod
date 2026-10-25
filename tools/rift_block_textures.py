"""Generates the Underdark Rift block textures (design 9, T2).

    python3 tools/rift_block_textures.py

Writes 16x16 textures under textures/block/:
- rift_veil.png (+ .mcmeta): the veil pool, an animated (16 frames) swirl of black and deep violet with motes
  sinking through it.
- rift_anchor.png / rift_anchor_top.png: crying-obsidian-like stone (near-black purple with violet tear streaks),
  and its end face.
- rift_anchor_crystal.png / rift_anchor_crystal_top.png: the crowning block, its upper half grown over with a
  pale violet crystal.
Deterministic (seeded), so reruns give the same pixels.
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
BLOCK = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')
N = 16
FRAMES = 16


def clamp(v):
    return max(0, min(255, int(round(v))))


def rgba(c, a=255):
    return (clamp(c[0]), clamp(c[1]), clamp(c[2]), a)


def shade(c, d):
    return rgba((c[0] + d, c[1] + d, c[2] + d))


def mix(a, b, t):
    return rgba(tuple(a[i] + (b[i] - a[i]) * t for i in range(3)))


def save(name, rows, mcmeta=None):
    os.makedirs(BLOCK, exist_ok=True)
    pngio.write_png(os.path.join(BLOCK, name + '.png'), N, len(rows), rows)
    if mcmeta:
        with open(os.path.join(BLOCK, name + '.png.mcmeta'), 'w') as fh:
            json.dump(mcmeta, fh, indent=2)
            fh.write('\n')
    print('wrote', name + '.png')


VOID = (8, 4, 14)
DEEP = (40, 14, 70)
VIOLET = (120, 60, 200)
MOTE = (210, 170, 255)


def veil(rng):
    """Frames of a slow swirl: two interfering waves, tiling at 16, with motes drifting down."""
    motes = [(rng.randrange(N), rng.randrange(N), rng.random()) for _ in range(6)]
    rows = []
    for f in range(FRAMES):
        phase = 2 * math.pi * f / FRAMES
        frame = []
        for y in range(N):
            line = []
            for x in range(N):
                a = math.sin(2 * math.pi * (x + y) / N + phase)
                b = math.sin(2 * math.pi * (x - 2 * y) / N - 2 * phase)
                c = math.sin(2 * math.pi * (2 * x) / N + phase * 3) * 0.5
                v = (a + b + c) / 2.5  # -1..1
                t = max(0.0, v) ** 1.6
                col = mix(VOID, DEEP, (v + 1) / 2 * 0.7)
                col = mix(col, VIOLET, t * 0.55)
                line.append(shade(col, rng.randint(-3, 3)))
            frame.append(line)
        for mx, my, glow in motes:
            yy = (my + f) % N
            frame[yy][mx] = mix(MOTE, VIOLET, glow * 0.4)
            frame[(yy - 1) % N][mx] = mix(frame[(yy - 1) % N][mx], MOTE, 0.4)
        rows.extend(frame)
    return rows


OBSIDIAN = (22, 12, 34)
OBSIDIAN_LIGHT = (44, 26, 66)
TEAR = (150, 70, 230)
TEAR_BRIGHT = (205, 140, 255)


def anchor(rng, end=False):
    rows = [[shade(OBSIDIAN, rng.randint(-5, 5)) for _ in range(N)] for _ in range(N)]
    for _ in range(9):
        x, y = rng.randrange(N), rng.randrange(N)
        for dx in range(rng.randint(1, 3)):
            rows[y][(x + dx) % N] = shade(OBSIDIAN_LIGHT, rng.randint(-6, 6))
    if end:
        # A violet ring round a bright core.
        for y in range(N):
            for x in range(N):
                r = math.hypot(x - 7.5, y - 7.5)
                if 4.5 < r < 5.6:
                    rows[y][x] = mix(TEAR, OBSIDIAN, 0.35)
                elif r < 1.6:
                    rows[y][x] = rgba(TEAR_BRIGHT)
        return rows
    # Tear streaks running down.
    for _ in range(4):
        x = rng.randrange(1, N - 1)
        y = rng.randrange(0, 6)
        for dy in range(rng.randint(4, 9)):
            if y + dy < N:
                rows[y + dy][x] = mix(TEAR, TEAR_BRIGHT, rng.random() * 0.5) if dy % 3 else rgba(TEAR_BRIGHT)
    return rows


CRYSTAL = (190, 150, 255)
CRYSTAL_DARK = (120, 80, 200)


def crystal(rng, end=False):
    rows = anchor(rng, end)
    for y in range(N):
        for x in range(N):
            if end:
                r = math.hypot(x - 7.5, y - 7.5)
                if r < 5.0:
                    rows[y][x] = mix(CRYSTAL, CRYSTAL_DARK, (math.sin(x * 1.3 + y) + 1) / 4)
            else:
                # Facets climbing out of the stone: a jagged edge across the middle.
                edge = 7 + int(2 * math.sin(x * 1.7)) - (2 if x % 5 == 2 else 0)
                if y < edge:
                    facet = ((x + y) // 3) % 2
                    base = CRYSTAL if facet else CRYSTAL_DARK
                    rows[y][x] = shade(base, rng.randint(-8, 8) + (10 if (x + y) % 6 == 0 else 0))
    return rows


def main():
    rng = random.Random(9002)
    save('rift_veil', veil(rng), {'animation': {'frametime': 3, 'interpolate': True}})
    save('rift_anchor', anchor(rng))
    save('rift_anchor_top', anchor(rng, True))
    save('rift_anchor_crystal', crystal(rng))
    save('rift_anchor_crystal_top', crystal(rng, True))


if __name__ == '__main__':
    main()
