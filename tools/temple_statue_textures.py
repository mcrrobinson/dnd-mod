"""Generates the Dragon Temple statue textures (the temple rework card).

    python3 tools/temple_statue_textures.py

All 16x16 under textures/block/:
- <ember|frost|storm>_dragonstone: carved statue stone with small scales in relief, each lit on its upper rim
  and shadowed under its lower edge. One drawing, three stones: red-brown, pale frost marble, storm slate.
- <ember|frost|storm>_dragon_belly: the statue's chest and belly, broad horizontal plates with dark seams and a
  lit upper edge.
- <ember|frost|storm>_dragonstone_eye: the eye block's front, a glowing slit-pupilled eye under a heavy brow set
  into dragonstone (its other faces are the dragonstone).
- dragon_horn: ivory with ridged growth rings, darker towards the base (the bottom rows).
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures/block')
N = 16


def rgba(c):
    return tuple(max(0, min(255, int(v))) for v in c[:3]) + (255,)


def shade(c, d):
    return rgba(tuple(v + d for v in c[:3]))


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return rgba(tuple(a[i] + (b[i] - a[i]) * t for i in range(3)))


def save(name, rows):
    pngio.write_png(os.path.join(OUT, name + '.png'), N, N, rows)
    print('wrote', name + '.png')


# Stone colours: rim light, scale body, scale shadow, deep crease, and (eye) glow, glow core, brow.
STONES = {
    'ember': {'rim': (196, 92, 64), 'body': (150, 54, 42), 'low': (112, 36, 32), 'crease': (58, 18, 20),
              'belly': (232, 168, 70), 'seam': (122, 52, 30),
              'glow': (255, 150, 30), 'core': (255, 236, 120)},
    'frost': {'rim': (246, 250, 255), 'body': (206, 222, 236), 'low': (166, 190, 214), 'crease': (98, 124, 158),
              'belly': (232, 242, 250), 'seam': (140, 168, 198),
              'glow': (90, 220, 255), 'core': (226, 252, 255)},
    'storm': {'rim': (92, 148, 176), 'body': (56, 96, 132), 'low': (38, 66, 102), 'crease': (18, 30, 56),
              'belly': (128, 184, 188), 'seam': (40, 86, 104),
              'glow': (255, 226, 70), 'core': (255, 255, 210)},
}

def scale_at(px, py):
    """(cx, cy, dx, dy) of the diamond scale at a pixel: scales are 8 wide and 8 tall, in rows 4 apart with odd
    rows offset by 4, so they tile the block like a snake's hide; dx/dy are -1..1 from its middle."""
    for cy in range(-8, 25, 4):
        offset = 4 if (cy // 4) % 2 else 0
        for cx in range(offset - 8, 25, 8):
            dx, dy = (px - cx) / 4.0, (py - cy) / 4.0
            if abs(dx) + abs(dy) <= 1.0:
                return cx, cy, dx, dy
    return None


def dragonstone(kind, seed=11):
    s = STONES[kind]
    rng = random.Random(seed)
    rows = [[None] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            cx, cy, dx, dy = scale_at(x + 0.5, y + 0.5)
            edge = abs(dx) + abs(dy)
            if edge > 0.8:
                # The crease round it: deep under its lower edges, lighter along its upper ones.
                c = s['crease'] if dy > -0.05 else s['low']
            elif abs(dx) < 0.2 and dy < 0.4:
                c = s['rim']  # the keel down its middle
            else:
                # Lit from above, darker towards its lower point.
                c = mix(s['rim'], s['low'], (dy + 0.9) / 1.6 + 0.2 * dx)
                c = mix(c, s['body'], 0.45)
            rows[y][x] = shade(c, rng.randint(-5, 5))
    return rows


def belly(kind):
    """Broad ventral plates, 2 rows to a block: each bulges out (lit at its top, shaded at its bottom) and its
    lower edge curves down in the middle, like the plates under a snake."""
    s = STONES[kind]
    rng = random.Random(23)
    rows = [[None] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            # The seam under each plate sags towards the middle of the block.
            sag = 1.0 - ((x + 0.5 - 8.0) / 8.0) ** 2
            v = (y + 0.5 - 2.5 * sag) % 8.0
            if v > 6.8:
                c = s['seam']
            else:
                c = mix(shade(s['belly'], 26), shade(s['belly'], -34), (v / 6.8) ** 1.4)
            if x in (0, 15):
                c = shade(c, -12)
            rows[y][x] = shade(c, rng.randint(-4, 4))
    return rows


EYE = [
    "................",
    "................",
    "..BBBBBBBBBBBB..",
    ".BBBBBBBBBBBBBB.",
    "..bbbbbbbbbbbb..",
    "...kGGGGGGGGk...",
    "..kGGGgPPgGGGk..",
    ".kGgggCPPCgggGk.",
    ".kGgggCPPCgggGk.",
    "..kGGGgPPgGGGk..",
    "...kGGGGGGGGk...",
    "....kkkkkkkk....",
    "................",
    "................",
    "................",
    "................",
]


def eye(kind):
    s = STONES[kind]
    rows = dragonstone(kind, seed=31)
    colours = {
        'B': shade(s['rim'], -20), 'b': rgba(s['crease']), 'k': rgba((20, 14, 14)),
        'G': rgba(s['glow']), 'g': mix(s['glow'], s['core'], 0.5), 'C': rgba(s['core']), 'P': rgba((10, 6, 6)),
    }
    for y, line in enumerate(EYE):
        assert len(line) == N
        for x, ch in enumerate(line):
            if ch in colours:
                rows[y][x] = colours[ch]
    return rows


def horn():
    rng = random.Random(5)
    rows = [[None] * N for _ in range(N)]
    tip = (246, 238, 214)
    base = (150, 122, 86)
    for y in range(N):
        for x in range(N):
            # y 0 is the tip end of the texture, 15 the base.
            c = mix(tip, base, y / 15.0)
            if (y + (x // 5)) % 3 == 0:
                c = shade(c, -26)  # growth ridge
            elif (y + (x // 5)) % 3 == 1:
                c = shade(c, 10)
            if x in (0, 15):
                c = shade(c, -16)
            rows[y][x] = shade(c, rng.randint(-4, 4))
    return rows


def main():
    for kind in STONES:
        save(f'{kind}_dragonstone', dragonstone(kind))
        save(f'{kind}_dragon_belly', belly(kind))
        save(f'{kind}_dragonstone_eye', eye(kind))
    save('dragon_horn', horn())


if __name__ == '__main__':
    main()
