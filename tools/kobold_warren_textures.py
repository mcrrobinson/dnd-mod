"""Block textures for the kobold warrens and their traps (docs/structures/kobold-warrens.md).

    python3 tools/kobold_warren_textures.py

Writes (16x16, textures/block/):
- loose_ceiling.png: andesite-grey rock with dark cracks running through it (a rockfall's roof)
- crumbling_floor_straw.png: a kobold pit's lid of matted straw over stretched hide
- kobold_tripline.png: a dull, knotted cord (only its middle rows are used by the model)
- spike_trap_spike.png: dark iron for the spikes of a spike strip, lighter at the tips
- kobold_egg.png: speckled red eggshell
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
TEX = os.path.join(ROOT, 'src/main/resources/assets/dndclasses/textures')
CLEAR = (0, 0, 0, 0)


def canvas(fill=CLEAR):
    return [[fill] * 16 for _ in range(16)]


def put(px, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        px[y][x] = c if len(c) == 4 else (*c, 255)


def mix(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def jitter(rng, c, n):
    j = rng.randint(-n, n)
    return tuple(max(0, min(255, v + j)) for v in c)


def write(path, px):
    full = os.path.join(TEX, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    pngio.write_png(full, 16, 16, px)
    print('wrote', os.path.relpath(full, ROOT))


def loose_ceiling():
    rng = random.Random(11)
    px = canvas()
    base = (132, 134, 133)
    for y in range(16):
        for x in range(16):
            c = jitter(rng, base, 12)
            if rng.random() < 0.12:
                c = mix(c, (168, 168, 166), 0.5)
            put(px, x, y, c)
    # Cracks: random walks from the edges inwards, with a pale lip on one side
    for start in ((0, 4), (15, 9), (6, 0), (10, 15)):
        x, y = start
        for _ in range(11):
            put(px, x, y, (52, 52, 54))
            put(px, x + 1, y, mix(px[y][min(15, x + 1)][:3], (180, 180, 178), 0.4))
            x += rng.choice((-1, 0, 1)) + (1 if start[0] == 0 else -1 if start[0] == 15 else 0)
            y += rng.choice((-1, 0, 1)) + (1 if start[1] == 0 else -1 if start[1] == 15 else 0)
            x = max(0, min(15, x))
            y = max(0, min(15, y))
    write('block/loose_ceiling.png', px)


def straw_lid():
    rng = random.Random(12)
    px = canvas()
    hide = (122, 86, 54)
    straws = [(196, 164, 70), (178, 146, 58), (212, 182, 92), (160, 128, 50)]
    for y in range(16):
        for x in range(16):
            put(px, x, y, jitter(rng, hide, 8))
    # Matted straw laid every which way
    for _ in range(46):
        x = rng.randrange(16)
        y = rng.randrange(16)
        dx, dy = rng.choice(((1, 0), (0, 1), (1, 1), (1, -1)))
        c = rng.choice(straws)
        for i in range(rng.randint(3, 6)):
            put(px, x + dx * i, y + dy * i, jitter(rng, c, 10))
    write('block/crumbling_floor_straw.png', px)


def tripline():
    rng = random.Random(13)
    px = canvas()
    cord = (104, 88, 66)
    for x in range(16):
        put(px, x, 7, jitter(rng, cord, 10))
        put(px, x, 8, jitter(rng, mix(cord, (60, 50, 38), 0.4), 8))
    for x in (3, 11):
        put(px, x, 6, (80, 66, 48))
        put(px, x, 9, (80, 66, 48))
    write('block/kobold_tripline.png', px)


def spike():
    rng = random.Random(14)
    px = canvas()
    for y in range(16):
        t = y / 15
        for x in range(16):
            c = mix((168, 170, 176), (58, 58, 64), t)
            put(px, x, y, jitter(rng, c, 6))
    write('block/spike_trap_spike.png', px)


def egg():
    rng = random.Random(15)
    px = canvas()
    shell = (176, 58, 44)
    for y in range(16):
        for x in range(16):
            c = jitter(rng, shell, 8)
            if y < 5:
                c = mix(c, (214, 104, 82), 0.35)
            if rng.random() < 0.16:
                c = jitter(rng, (92, 30, 26), 8)
            elif rng.random() < 0.05:
                c = (232, 196, 150)
            put(px, x, y, c)
    write('block/kobold_egg.png', px)


if __name__ == '__main__':
    loose_ceiling()
    straw_lid()
    tripline()
    spike()
    egg()
